package de.schosin.ecs.buildtools.codegen.apt.processor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.processing.Generated;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ArrayTypeName;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
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
    static final ClassName COMPONENTS = ClassName.get("de.schosin.ecs.api.components.mappers", "Components");
    static final ClassName COMPONENT_ACCESSOR = ClassName.get("de.schosin.ecs.api.data", "ComponentAccessor");
    static final ClassName COMPONENT_SET = ClassName.get("de.schosin.ecs.api.components", "ComponentSet");
    static final ClassName COMPONENT_SET_DATA = COMPONENT_SET.nestedClass("ComponentSetData");
    static final ClassName COMPONENT_SET_COMPONENT = COMPONENT_SET.nestedClass("Component");
    static final ClassName COMPONENT_ACCESSOR_PROCESSOR = COMPONENT_SET.nestedClass("IterableProcessor");
    static final ClassName ITERABLE_COMPONENT_ACCESSOR = ClassName.get("de.schosin.ecs.api.data", "IterableComponentAccessor");
    static final ClassName COMPONENT_SET_TYPE = ClassName.get("de.schosin.ecs.api.components.types", "ComponentSetType");

    private static final ClassName POOL = ClassName.get("de.schosin.ecs.utils.collections", "Pool");

    public static final ClassName DATA_ACCESSOR = ClassName.get("de.schosin.ecs.api.data", "DataAccessor");
    public static final ClassName DATA_PROCESSOR = ClassName.get("de.schosin.ecs.api.data", "DataProcessor");
    public static final ClassName ITERABLE_ACCESSOR = ClassName.get("de.schosin.ecs.api.data", "IterableAccessor");

    private static final WildcardTypeName WILDCARD = WildcardTypeName.subtypeOf(Object.class);

    private static final AnnotationSpec SUPPRESS_UNCHECKED = AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "\"unchecked\"").build();

    public List<TypeData> generate(Set<? extends Element> elements) {
        return elements.stream()
                .flatMap(this::generate)
                .toList();
    }

    private Stream<TypeData> generate(Element element) {
        if (element.getKind() == ElementKind.METHOD) {
            return ComponentSetTypes.create(element.accept(new MethodVisitor((ExecutableElement) element), null));
        }

        throw new CancelException("Unsupported kind %s: %s".formatted(element.getKind(), element));
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

            var accessor = ComponentSetAccessor.createAccessor(result);
            if (implementation != null) {
                types.add(accessor);
            }

            return Stream.of(new TypeData(result, factoryMethod, factoryMethodEntity, types));
        }

        private static TypeSpec createInterface(VisitorResult result) {
            var interfaceName = result.interfaceName;
            var implementationName = result.implementationName;
            var accessorName = ClassName.get(result.interfaceName.packageName(), interfaceName.simpleName() + "Accessor");
            var components = result.components;

            var componentSetDataType = ParameterizedTypeName.get(COMPONENT_SET_DATA, interfaceName, interfaceName.nestedClass("Processor"));
            var componentSetData = FieldSpec.builder(componentSetDataType, "DATA", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$1T.DATA", implementationName)
                    .build();

            var processor = interfaceName.nestedClass("Processor");
            var superinterface = ParameterizedTypeName.get(COMPONENT_SET, processor);

            var componentSet = TypeSpec.interfaceBuilder(interfaceName)
                    .addAnnotation(GENERATED)
                    .addModifiers(Modifier.PUBLIC, Modifier.SEALED)
                    .addSuperinterface(superinterface)
                    .addPermittedSubclass(implementationName)
                    .addPermittedSubclass(accessorName)
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

            var nullCase = CodeBlock.builder().add("process(entityId");
            var defaultCase = "process(entityId";

            for (var component : result.components) {
                process.addParameter(component.typeName, component.name);

                if (result.components.size() == 1) {
                    nullCase.add(", ($1T) null", component.type);
                } else {
                    nullCase.add(", null");
                }
                defaultCase += ", data.%s()".formatted(component.name);
            }

            nullCase.add(")");
            defaultCase += ")";

            var defaultProcessBody = CodeBlock.builder()
                    .beginControlFlow("if (data == null)")
                    .addStatement(nullCase.build())
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
            var accessorName = ClassName.get(result.interfaceName.packageName(), interfaceName.simpleName() + "Accessor");
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
                    .add("$1T.builder($2T::getInstance, $3T.INSTANCE)", COMPONENT_SET, accessorName, implementationName.nestedClass("IterableProcessor"));

            var componentSetDataType = ParameterizedTypeName.get(COMPONENT_SET_DATA, interfaceName, interfaceName.nestedClass("Processor"));
            var componentSetData = FieldSpec.builder(componentSetDataType, "DATA", Modifier.STATIC, Modifier.FINAL);

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

            var toStringBody = CodeBlock.builder()
                    .add("return new $1T().append(\"%s(\")".formatted(interfaceName.simpleName()), StringBuilder.class);

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
                componentSetDataInitializer.add(".add(new $1T<>($2T::%s) {})".formatted(component.name), COMPONENT_SET_COMPONENT, interfaceName);

                getInstance.addParameter(component.typeName, component.name);
                getInstance.addStatement("instance.%s = %s".formatted(component.name, component.name));

                if (i > 0) {
                    toStringBody.add(".append(\", \")");
                }
                toStringBody.add(System.lineSeparator() + "        ");
                toStringBody.add(".append(%s)".formatted(component.name));
            }

            componentSetData.initializer(componentSetDataInitializer.add(System.lineSeparator() + "        .build()").build());
            getInstance.addStatement("return instance");

            toStringBody.add(System.lineSeparator() + "        ");
            toStringBody.addStatement(".append(')').toString()");

            var toString = MethodSpec.methodBuilder("toString")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(String.class)
                    .addCode(toStringBody.build())
                    .build();

            return implementation
                    .addField(componentSetData.build())
                    .addMethod(entityIdAccessor)
                    .addMethod(reset.build())
                    .addMethod(free)
                    .addMethod(getInstance.build())
                    .addMethod(toString)
                    .addType(createIterableProcessorType(result))
                    .build();
        }

        private static TypeSpec createIterableProcessorType(VisitorResult result) {
            var superInterface = ParameterizedTypeName.get(COMPONENT_ACCESSOR_PROCESSOR, result.interfaceName, result.interfaceName.nestedClass("Processor"));

            return TypeSpec.enumBuilder("IterableProcessor")
                    .addSuperinterface(superInterface)
                    .addEnumConstant("INSTANCE")
                    .addMethod(iterableProcessorImpl(result))
                    .addMethod(iterableProcessorProcess(result))
                    .build();
        }

        private static MethodSpec iterableProcessorImpl(VisitorResult result) {
            var components = result.components;
            var n = components.size();

            var methodBody = CodeBlock.builder();

            methodBody.addStatement("// get accessors");
            for (int i = 1; i <= n; i++) {
                var type = components.get(i - 1).typeName;
                var mapper = ParameterizedTypeName.get(COMPONENT_ACCESSOR, type);

                methodBody.addStatement("var accessor%d = ($1T) mappers[%d].getComponentAccessor(accessor)".formatted(i, i - 1), mapper);
            }

            methodBody.addStatement("// process");
            methodBody.beginControlFlow("while(accessor.hasNext())");
            methodBody.add("processor.process(accessor.next()");
            for (int i = 1; i <= n; i++) {
                methodBody.indent().add(", %saccessor%d.getComponent(accessor)".formatted(System.lineSeparator(), i)).unindent();
            }
            methodBody.addStatement(")");
            methodBody.endControlFlow(); // while

            methodBody.addStatement("// free accessors");
            for (int i = 1; i <= n; i++) {
                methodBody.addStatement("accessor%d.free()".formatted(i));
            }

            var mappers = ArrayTypeName.of(ParameterizedTypeName.get(COMPONENTS, WILDCARD, WILDCARD));

            return MethodSpec.methodBuilder("process")
                    .addAnnotation(Override.class)
                    .addAnnotation(SUPPRESS_UNCHECKED)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(result.interfaceName.nestedClass("Processor"), "processor")
                    .addParameter(ITERABLE_ACCESSOR, "accessor")
                    .addParameter(mappers, "mappers")
                    .addCode(methodBody.build())
                    .build();
        }

        private static MethodSpec iterableProcessorProcess(VisitorResult result) {
            var parameters = new ArrayList<ParameterSpec>();

            var body = CodeBlock.builder();
            body.beginControlFlow("while(accessor.hasNext())");
            body.add("processor.process(accessor.next()");

            for (int i = 1, s = result.components.size(); i <= s; i++) {
                var component = result.components.get(i - 1);
                var componentAccessor = ParameterizedTypeName.get(COMPONENT_ACCESSOR, component.typeName);

                parameters.add(ParameterSpec.builder(componentAccessor, "accessor" + i).build());

                body.add(", " + System.lineSeparator()).indent().add("accessor%d.getComponent(accessor)".formatted(i)).unindent();
            }

            body.addStatement(")");
            body.endControlFlow();

            return MethodSpec.methodBuilder("process")
                    .addParameter(result.interfaceName.nestedClass("Processor"), "processor")
                    .addParameter(ITERABLE_ACCESSOR, "accessor")
                    .addParameters(parameters)
                    .addCode(body.build())
                    .build();
        }

        private static class ComponentSetAccessor {

            static TypeSpec createAccessor(VisitorResult result) {
                var interfaceName = result.interfaceName;
                var accessorName = ClassName.get(result.interfaceName.packageName(), interfaceName.simpleName() + "Accessor");

                var iterableComponentAccessor = ParameterizedTypeName.get(ITERABLE_COMPONENT_ACCESSOR, interfaceName, interfaceName.nestedClass("Processor"));

                var parameterizedPool = ParameterizedTypeName.get(POOL, accessorName);
                var pool = FieldSpec.builder(parameterizedPool, "POOL", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$1T.unbounded($2T.class, $2T::new)", POOL, accessorName)
                        .build();

                var componentAccessors = result.components.stream()
                        .map(ComponentSetAccessor::componentAccessor)
                        .toList();

                return TypeSpec.classBuilder(accessorName)
                        .addAnnotation(GENERATED)
                        .addModifiers(Modifier.FINAL)
                        .addSuperinterface(interfaceName)
                        .addSuperinterface(iterableComponentAccessor)
                        .addField(pool)
                        .addField(DATA_ACCESSOR, "accessor", Modifier.PRIVATE)
                        .addFields(componentAccessors)
                        .addMethods(IterableComponentAccessorImplementation.methods(result))
                        .addMethods(ComponentSetImplementation.methods(result))
                        .addMethod(reset(result))
                        .build();
            }

            private static FieldSpec componentAccessor(ComponentData componentData) {
                var type = ParameterizedTypeName.get(COMPONENT_ACCESSOR, componentData.typeName);
                return FieldSpec.builder(type, componentData.name, Modifier.PRIVATE).build();
            }

            private static class IterableComponentAccessorImplementation {

                static List<MethodSpec> methods(VisitorResult result) {
                    return List.of(
                            getInstance(result),
                            getComponent(result),
                            process(result),
                            free());
                }

                private static MethodSpec getInstance(VisitorResult result) {
                    var interfaceName = result.interfaceName;
                    var componentAccessor = ParameterizedTypeName.get(COMPONENT_ACCESSOR, interfaceName);

                    var mappers = ArrayTypeName.of(ParameterizedTypeName.get(COMPONENTS, WILDCARD, WILDCARD));

                    var body = CodeBlock.builder();
                    body.addStatement("var instance = POOL.getInstance()");

                    for (int i = 0, s = result.components.size(); i < s; i++) {
                        var componentData = result.components.get(i);
                        var accessor = ParameterizedTypeName.get(COMPONENT_ACCESSOR, componentData.typeName);

                        body.addStatement("instance.%s = ($1T) mappers[%d].getComponentAccessor(accessor)".formatted(componentData.name, i), accessor);
                    }

                    body.addStatement("return instance");

                    return MethodSpec.methodBuilder("getInstance")
                            .addAnnotation(SUPPRESS_UNCHECKED)
                            .addModifiers(Modifier.STATIC)
                            .addParameter(DATA_ACCESSOR, "accessor")
                            .addParameter(mappers, "mappers")
                            .returns(componentAccessor)
                            .addCode(body.build())
                            .build();
                }

                private static MethodSpec getComponent(VisitorResult result) {
                    return MethodSpec.methodBuilder("getComponent")
                            .addAnnotation(Override.class)
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .addParameter(DATA_ACCESSOR, "accessor")
                            .returns(result.interfaceName)
                            .addStatement("this.accessor = accessor")
                            .addStatement("return this")
                            .build();
                }

                private static MethodSpec process(VisitorResult result) {
                    var processor = result.interfaceName.nestedClass("Processor");
                    var iterableProcessor = result.implementationName.nestedClass("IterableProcessor");

                    var body = CodeBlock.builder();
                    body.add("$1T.INSTANCE.process(processor, accessor", iterableProcessor);
                    for (var component : result.components) {
                        body.add(", " + System.lineSeparator()).indent().add(component.name).unindent();
                    }
                    body.addStatement(")");

                    return MethodSpec.methodBuilder("process")
                            .addAnnotation(Override.class)
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .addParameter(ITERABLE_ACCESSOR, "accessor")
                            .addParameter(processor, "processor")
                            .addCode(body.build())
                            .build();
                }

                private static MethodSpec free() {
                    return MethodSpec.methodBuilder("free")
                            .addAnnotation(Override.class)
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .addStatement("POOL.free(this)")
                            .build();
                }

            }

            private static class ComponentSetImplementation {

                static List<MethodSpec> methods(VisitorResult result) {
                    var methods = new ArrayList<MethodSpec>();
                    methods.add(entityId());
                    methods.addAll(result.components.stream().map(ComponentSetImplementation::getter).toList());
                    methods.add(toString(result));

                    return methods;
                }

                private static MethodSpec entityId() {
                    return MethodSpec.methodBuilder("entityId")
                            .addAnnotation(Override.class)
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .returns(TypeName.INT)
                            .addStatement("return accessor.entityId()")
                            .build();
                }

                private static MethodSpec getter(ComponentData componentData) {
                    return MethodSpec.methodBuilder(componentData.name)
                            .addAnnotation(Override.class)
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .returns(componentData.typeName)
                            .addStatement("return this.%s.getComponent(this.accessor)".formatted(componentData.name))
                            .build();
                }

                private static MethodSpec toString(VisitorResult result) {
                    var interfaceName = result.interfaceName;

                    var body = CodeBlock.builder();

                    body.beginControlFlow("if (accessor == null)");
                    body.addStatement("return \"%s(invalidated)\"".formatted(interfaceName.simpleName()));
                    body.endControlFlow();

                    body.add("return new $1T().append(\"%s(\")".formatted(interfaceName.simpleName()), StringBuilder.class);

                    for (int i = 0, s = result.components.size(); i < s; i++) {
                        var componentData = result.components.get(i);

                        if (i > 0) {
                            body.add(".append(\", \")");
                        }

                        body.add(System.lineSeparator());
                        body.indent().add(".append(%s())".formatted(componentData.name)).unindent();
                    }

                    body.add(System.lineSeparator()).indent().addStatement(".append(\")\").toString()").unindent();

                    return MethodSpec.methodBuilder("toString")
                            .addAnnotation(Override.class)
                            .addModifiers(Modifier.PUBLIC)
                            .returns(String.class)
                            .addCode(body.build())
                            .build();
                }

            }

            private static MethodSpec reset(VisitorResult result) {
                var body = CodeBlock.builder();

                body.addStatement("this.accessor = null");
                for (var componentData : result.components) {
                    body.addStatement("this.%s.free()".formatted(componentData.name));
                    body.addStatement("this.%s = null".formatted(componentData.name));
                }

                return MethodSpec.methodBuilder("reset")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addCode(body.build())
                        .build();
            }

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
