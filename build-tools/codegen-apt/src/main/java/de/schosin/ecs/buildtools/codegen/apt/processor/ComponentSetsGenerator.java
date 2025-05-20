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
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

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

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.ComponentSet.ComponentAccessor;
import de.schosin.ecs.api.components.ComponentSet.ComponentSetData;
import de.schosin.ecs.buildtools.codegen.ComponentSetConfig;
import de.schosin.ecs.buildtools.codegen.apt.visitor.InterfaceVisitor;
import de.schosin.ecs.buildtools.codegen.apt.visitor.RecordVisitor;
import de.schosin.ecs.utils.collections.Pool;

public class ComponentSetsGenerator {

    public record TypeData(VisitorResult result, MethodSpec factory, MethodSpec entityFactory, List<TypeSpec> types) {
    }

    public record ComponentData(DeclaredType type, TypeName typeName, String name, boolean optional) {
    }

    private static final AnnotationSpec GENERATED = AnnotationSpec.builder(Generated.class)
            .addMember("value", "\"%s\"".formatted(ComponentSetsGenerator.class.getName()))
            .addMember("date", "\"%s\"".formatted(Instant.now()))
            .build();

    private static final ClassName COMPONENT_SET = ClassName.get(ComponentSet.class);
    private static final ClassName COMPONENT_SET_DATA = ClassName.get(ComponentSetData.class);
    private static final ClassName COMPONENT_ACCESSOR = ClassName.get(ComponentAccessor.class);

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
                if (result.implementationType != null) {
                    continue;
                }

                if (outerComma) {
                    initializer.add(", ");
                } else {
                    outerComma = true;
                }

                var target = result.interfaceType != null ? result.implementationName : result.interfaceName;

                initializer.add(System.lineSeparator());
                initializer.add("    $1T.entry($2T.class, $3T.DATA)", Map.class, result.interfaceName, target);
            }

            var componentSetDataType = ParameterizedTypeName.get(COMPONENT_SET_DATA, WildcardTypeName.subtypeOf(COMPONENT_SET));
            var classT = ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(COMPONENT_SET));
            var type = ParameterizedTypeName.get(ClassName.get(Map.class), classT, componentSetDataType);

            return FieldSpec.builder(type, "LOOKUP", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer(initializer.add(")").build())
                    .build();
        }

        private static MethodSpec getData() {
            var typeS = TypeVariableName.get("S", COMPONENT_SET);

            var wildcardClass = ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("S"));
            var componentSetData = ParameterizedTypeName.get(COMPONENT_SET_DATA, TypeVariableName.get("S"));

            return MethodSpec.methodBuilder("getData")
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
                .filter(this::isValidElement)
                .flatMap(this::generate)
                .toList();
    }

    private boolean isValidElement(Element element) {
        if (!(element instanceof TypeElement typeElement)) {
            return false;
        }

        if (typeElement.getAnnotation(Generated.class) != null) {
            return false;
        }

        if (typeElement.getKind() == ElementKind.RECORD) {
            return typeElement.getAnnotation(ComponentSetConfig.class) != null;
        }

        for (var iface : typeElement.getInterfaces()) {
            if (isOrExtendsComponentSet(iface)) {
                return true;
            }
        }

        return false;
    }

    private boolean isOrExtendsComponentSet(TypeMirror type) {
        if (!(type instanceof DeclaredType declaredType)) {
            return false;
        }

        if (declaredType.getAnnotation(Generated.class) != null) {
            return false;
        }

        var typeElement = declaredType.asElement() instanceof TypeElement elem ? elem : null;
        if (typeElement == null) {
            return false;
        }

        if (ComponentSet.class.getName().equals(typeElement.getQualifiedName().toString())) {
            return true;
        }

        for (var iface : typeElement.getInterfaces()) {
            if (isOrExtendsComponentSet(iface)) {
                return true;
            }
        }

        return false;
    }

    private Stream<TypeData> generate(Element element) {
        if (element instanceof TypeElement typeElement) {
            return createImplementation(typeElement);
        }

        return Stream.empty();
    }

    private Stream<TypeData> createImplementation(TypeElement typeElement) {
        return switch (typeElement.getKind()) {
            case RECORD -> ComponentSetTypes.create(typeElement.accept(new RecordVisitor(typeElement), null));
            case INTERFACE -> ComponentSetTypes.create(typeElement.accept(new InterfaceVisitor(typeElement), null));
            case CLASS -> Stream.empty();
            case ANNOTATION_TYPE, BINDING_VARIABLE, CONSTRUCTOR, ENUM, ENUM_CONSTANT, EXCEPTION_PARAMETER, FIELD, INSTANCE_INIT, LOCAL_VARIABLE, METHOD, MODULE, OTHER, PACKAGE, PARAMETER, RECORD_COMPONENT, RESOURCE_VARIABLE, STATIC_INIT, TYPE_PARAMETER -> throw new CancelException(
                    "Unsupported kind %s: %s".formatted(typeElement.getKind(), typeElement));
        };
    }

    private static class ComponentSetTypes {

        private static Stream<TypeData> create(VisitorResult result) {
            var interfaceName = result.interfaceName;
            var implementationName = result.implementationName;
            var target = result.interfaceType == null || result.implementationType != null ? interfaceName : implementationName;

            var factoryMethod = createFactoryMethod(false, result.factoryName, interfaceName, target, result.components);
            var factoryMethodEntity = createFactoryMethod(true, result.factoryName, interfaceName, target, result.components);

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
            if (result.interfaceType != null) {
                return null;
            }

            var interfaceName = result.interfaceName;
            var implementationName = result.implementationName;
            var components = result.components;

            var componentSetDataType = ParameterizedTypeName.get(COMPONENT_SET_DATA, interfaceName);
            var componentSetData = FieldSpec.builder(componentSetDataType, "DATA", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$1T.DATA", implementationName)
                    .build();

            var componentSet = TypeSpec.interfaceBuilder(interfaceName)
                    .addAnnotation(GENERATED)
                    .addModifiers(Modifier.PUBLIC)
                    .addSuperinterface(ComponentSet.class)
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

        private static TypeSpec createImplementation(VisitorResult result) {
            if (result.implementationType != null) {
                return null;
            }

            var interfaceName = result.interfaceName;
            var implementationName = result.implementationName;
            var components = result.components;

            var parameterizedPool = ParameterizedTypeName.get(ClassName.get(Pool.class), implementationName);
            var pool = FieldSpec.builder(parameterizedPool, "POOL", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$1T.unbounded($2T.class, $2T::new)", Pool.class, implementationName)
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

            if (result.interfaceType != null) {
                implementation.addModifiers(Modifier.PUBLIC);
                getInstance.addModifiers(Modifier.PUBLIC);
                componentSetData.addModifiers(Modifier.PUBLIC);
            }

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
        RECORD, INTERFACE, MANUAL
    }

    public static class VisitorResult {
        public VisitorKind kind;
        public String source;
        public String packageName;

        public ClassName interfaceName;
        public ClassName implementationName;

        public DeclaredType interfaceType;
        public DeclaredType implementationType;

        public String factoryName;

        public final List<ComponentData> components = new ArrayList<>();
    }

}
