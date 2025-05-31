package de.schosin.ecs.buildtools.codegen.apt.processor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.processing.Generated;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;
import com.palantir.javapoet.WildcardTypeName;

import de.schosin.ecs.buildtools.codegen.apt.visitor.MethodVisitor;

public class ComponentSetsGenerator {

    public record TypeData(VisitorResult result, MethodSpec factory, MethodSpec entityFactory, List<TypeSpec> types) {
    }

    public record ComponentData(DeclaredType type, TypeName typeName, String name, boolean optional) {
    }

    private static final AnnotationSpec GENERATED = AnnotationSpec.builder(Generated.class)
            .addMember("value", "\"%s\"".formatted(ComponentSetsGenerator.class.getName()))
            .addMember("date", "\"%s\"".formatted(Instant.now()))
            .build();

    static final ClassName COMPONENT_TYPE = ClassName.get("de.schosin.ecs.api.components.types", "ComponentType");
    static final ClassName COMPONENT_SET = ClassName.get("de.schosin.ecs.api.components", "ComponentSet");
    static final ClassName COMPONENT_SET_DATA = COMPONENT_SET.nestedClass("ComponentSetData");
    static final ClassName COMPONENT_ACCESSOR = COMPONENT_SET.nestedClass("ComponentAccessor");
    static final ClassName COMPONENT_SET_TYPE = ClassName.get("de.schosin.ecs.api.components.types", "ComponentSetType");

    private static final ClassName POOL = ClassName.get("de.schosin.ecs.utils.collections", "Pool");

    public static final ClassName DATA_PROCESSOR = ClassName.get("de.schosin.ecs.api.data", "DataProcessor");
    public static final ClassName DATA_PROCESSOR_TYPE = ClassName.get("de.schosin.ecs.api.data", "DataProcessorType");

    public TypeSpec generateComponentSets(List<TypeData> implementations) {
        var constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build();

        var methods = implementations.stream()
                .flatMap(impl -> Stream.of(impl.factory, impl.entityFactory))
                .filter(Objects::nonNull)
                .toList();

        return TypeSpec.classBuilder("ComponentSets")
                .addAnnotation(GENERATED)
                .addModifiers(Modifier.PUBLIC)
                .addField(ComponentSets.lookupMap(implementations))
                .addMethods(methods)
                .addMethod(ComponentSets.getData())
                .addMethod(constructor)
                .build();
    }

    private static class ComponentSets {

        public static FieldSpec lookupMap(List<TypeData> implementations) {
            var initializer = CodeBlock.builder()
                    .add("$1T.ofEntries(", Map.class);

            var outerComma = false;
            for (var implementation : implementations) {
                var result = implementation.result;

                if (outerComma) {
                    initializer.add(", ");
                } else {
                    outerComma = true;
                }

                initializer.add(System.lineSeparator());
                initializer.add("    $1T.entry($2T.class, $2T.DATA)", Map.class, result.interfaceName);
            }

            var componentSet = ParameterizedTypeName.get(COMPONENT_SET, WildcardTypeName.subtypeOf(Object.class));
            var componentSetDataType = ParameterizedTypeName.get(COMPONENT_SET_DATA, WildcardTypeName.subtypeOf(componentSet));
            var classT = ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(componentSet));
            var type = ParameterizedTypeName.get(ClassName.get(Map.class), classT, componentSetDataType);

            return FieldSpec.builder(type, "LOOKUP", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer(initializer.add(")").build())
                    .build();
        }

        private static MethodSpec getData() {
            var typeS = TypeVariableName.get("S", ParameterizedTypeName.get(COMPONENT_SET, WildcardTypeName.subtypeOf(Object.class)));

            var wildcardClass = ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("S"));
            var componentSetData = ParameterizedTypeName.get(COMPONENT_SET_DATA, TypeVariableName.get("S"));

            return MethodSpec.methodBuilder("getData")
                    .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "\"unchecked\"").build())
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addTypeVariable(typeS)
                    .addParameter(wildcardClass, "componentSet")
                    .returns(componentSetData)
                    .addStatement("return ($1T<S>) LOOKUP.get(componentSet)", COMPONENT_SET_DATA)
                    .build();
        }

    }

    public List<TypeData> generate(Set<? extends Element> elements) {
        return elements.stream()
                .flatMap(this::generate)
                .toList();
    }

    private Stream<TypeData> generate(Element element) {
        return switch (element.getKind()) {
            case METHOD -> ComponentSetTypes.create(element.accept(new MethodVisitor((ExecutableElement) element), null));
            default -> throw new CancelException("Unsupported kind %s: %s".formatted(element.getKind(), element));
        };
    }

    private static class ComponentSetTypes {

        private static Stream<TypeData> create(VisitorResult result) {
            var interfaceName = result.interfaceName;

            var factoryMethod = createFactoryMethod(false, result.factoryName, interfaceName, interfaceName, result.components);
            var factoryMethodEntity = createFactoryMethod(true, result.factoryName, interfaceName, interfaceName, result.components);

            var types = new ArrayList<TypeSpec>(2);

            var componentSet = createInterface(result);
            if (componentSet != null) {
                types.add(componentSet);
            }

            var implementation = createImplementation(result);
            if (implementation != null) {
                types.add(implementation);
            }

            return Stream.of(new TypeData(result, factoryMethod, factoryMethodEntity, types));
        }

        private static TypeSpec createInterface(VisitorResult result) {
            var interfaceName = result.interfaceName;
            var implementationName = result.implementationName;
            var components = result.components;

            var componentSetDataType = ParameterizedTypeName.get(COMPONENT_SET_DATA, interfaceName);
            var componentSetData = FieldSpec.builder(componentSetDataType, "DATA", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$1T.DATA", implementationName)
                    .build();

            var processor = interfaceName.nestedClass("Processor");
            var superinterface = ParameterizedTypeName.get(COMPONENT_SET, processor);

            var componentSet = TypeSpec.interfaceBuilder(interfaceName)
                    .addAnnotation(GENERATED)
                    .addModifiers(Modifier.PUBLIC)
                    .addSuperinterface(superinterface)
                    .addField(componentSetData)
                    .addMethod(createFactoryMethod(true, "get", interfaceName, implementationName, components))
                    .addMethod(createFactoryMethod(false, "get", interfaceName, implementationName, components));

            for (var component : components) {
                var interfaceAccessor = MethodSpec.methodBuilder(component.name)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(component.typeName)
                        .build();

                componentSet.addMethod(interfaceAccessor);
            }

            var dataProcessorType = ParameterizedTypeName.get(DATA_PROCESSOR_TYPE, interfaceName, processor);
            componentSet.addSuperinterface(dataProcessorType);
            componentSet.addType(createProcessorType(result));

            var type = ParameterizedTypeName.get(COMPONENT_SET_TYPE, interfaceName, processor);
            var typeField = FieldSpec.builder(type, "TYPE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$1T.componentSet($2T.class, $3T.class)", COMPONENT_TYPE, interfaceName, processor)
                    .build();

            componentSet.addField(typeField);

            return componentSet.build();
        }

        private static MethodSpec createFactoryMethod(boolean entityId, String name, ClassName interfaceName, ClassName target, List<ComponentData> components) {
            var factoryMethod = MethodSpec.methodBuilder(name)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(interfaceName);

            var factoryMethodBody = CodeBlock.builder()
                    .add("return $1T.get(", target);

            if (entityId) {
                factoryMethod.addParameter(TypeName.INT, "entityId");
            }

            factoryMethodBody.add(entityId ? "entityId" : "-1");

            for (var component : components) {
                factoryMethod.addParameter(component.typeName, component.name);
                factoryMethodBody.add(", ").add(component.name);
            }

            factoryMethodBody.addStatement(")");

            return factoryMethod
                    .addCode(factoryMethodBody.build())
                    .build();
        }

        private static TypeSpec createProcessorType(VisitorResult result) {
            var superinterface = ParameterizedTypeName.get(DATA_PROCESSOR, result.interfaceName);

            var process = MethodSpec.methodBuilder("process")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(TypeName.INT, "entityId");

            var nullCase = "process(entityId";
            var defaultCase = "process(entityId";

            for (var component : result.components) {
                process.addParameter(component.typeName, component.name);

                nullCase += ", null";
                defaultCase += ", data.%s()".formatted(component.name);
            }

            nullCase += ")";
            defaultCase += ")";

            var defaultProcessBody = CodeBlock.builder()
                    .beginControlFlow("if (data == null)")
                    .addStatement(nullCase)
                    .addStatement("return")
                    .endControlFlow()
                    .addStatement(defaultCase)
                    .build();

            var defaultProcess = MethodSpec.methodBuilder("process")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addParameter(TypeName.INT, "entityId")
                    .addParameter(result.interfaceName, "data")
                    .addCode(defaultProcessBody)
                    .build();

            return TypeSpec.interfaceBuilder("Processor")
                    .addAnnotation(FunctionalInterface.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addSuperinterface(superinterface)
                    .addMethod(defaultProcess)
                    .addMethod(process.build())
                    .build();
        }

        private static TypeSpec createImplementation(VisitorResult result) {
            var interfaceName = result.interfaceName;
            var implementationName = result.implementationName;
            var components = result.components;

            var parameterizedPool = ParameterizedTypeName.get(POOL, implementationName);
            var pool = FieldSpec.builder(parameterizedPool, "POOL", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$1T.unbounded($2T.class, $2T::new)", POOL, implementationName)
                    .build();

            var entityId = FieldSpec.builder(TypeName.INT, "entityId", Modifier.PRIVATE).initializer("-1").build();
            var entityIdAccessor = MethodSpec.methodBuilder("entityId")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(TypeName.INT)
                    .addStatement("return entityId")
                    .build();

            var implementation = TypeSpec.classBuilder(implementationName)
                    .addAnnotation(GENERATED)
                    .addModifiers(Modifier.FINAL)
                    .addSuperinterface(interfaceName)
                    .addField(pool)
                    .addField(entityId);

            var componentSetDataInitializer = CodeBlock.builder()
                    .add("$1T.builder($2T::factory)", COMPONENT_SET, implementationName);

            var componentSetDataType = ParameterizedTypeName.get(COMPONENT_SET_DATA, interfaceName);
            var componentSetData = FieldSpec.builder(componentSetDataType, "DATA", Modifier.STATIC, Modifier.FINAL);

            var factoryBody = CodeBlock.builder()
                    .add("return get(entityId");

            var factory = MethodSpec.methodBuilder("factory")
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                    .addParameter(TypeName.INT, "entityId")
                    .addParameter(Object[].class, "components").varargs()
                    .returns(interfaceName);

            var getInstance = MethodSpec.methodBuilder("get")
                    .addModifiers(Modifier.STATIC)
                    .addParameter(TypeName.INT, "entityId")
                    .returns(implementationName)
                    .addStatement("var instance = POOL.getInstance()")
                    .addStatement("instance.entityId = entityId");

            var reset = MethodSpec.methodBuilder("reset")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("this.entityId = -1");

            var free = MethodSpec.methodBuilder("free")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("POOL.free(this)")
                    .build();

            // Iterate components
            for (int i = 0, s = components.size(); i < s; i++) {
                var component = components.get(i);

                var componentField = FieldSpec.builder(component.typeName, component.name, Modifier.PRIVATE).build();

                var componentAccessor = MethodSpec.methodBuilder(component.name)
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(component.typeName)
                        .addStatement("return this.%s".formatted(component.name))
                        .build();

                implementation.addField(componentField);
                implementation.addMethod(componentAccessor);

                reset.addStatement("this.%s = null".formatted(component.name));

                componentSetDataInitializer.add(System.lineSeparator() + "        ");
                componentSetDataInitializer.add(".add(new $1T<>($2T::%s) {})".formatted(component.name), COMPONENT_ACCESSOR, interfaceName);

                factoryBody.add("," + System.lineSeparator() + "        ");
                factoryBody.add("($1T) components[%d]".formatted(i), component.type);

                getInstance.addParameter(component.typeName, component.name);
                getInstance.addStatement("instance.%s = %s".formatted(component.name, component.name));
            }

            componentSetData.initializer(componentSetDataInitializer.add(System.lineSeparator() + "        .build()").build());
            factory.addCode(factoryBody.addStatement(")").build());
            getInstance.addStatement("return instance");

            return implementation
                    .addField(componentSetData.build())
                    .addMethod(entityIdAccessor)
                    .addMethod(reset.build())
                    .addMethod(free)
                    .addMethod(factory.build())
                    .addMethod(getInstance.build())
                    .build();
        }

    }

    public enum VisitorKind {
        RECORD, INTERFACE, METHOD, MANUAL
    }

    public static class VisitorResult {
        public VisitorKind kind;
        public String source;
        public String packageName;

        public ClassName interfaceName;
        public ClassName implementationName;

        public String factoryName;

        public final List<ComponentData> components = new ArrayList<>();
    }

}
