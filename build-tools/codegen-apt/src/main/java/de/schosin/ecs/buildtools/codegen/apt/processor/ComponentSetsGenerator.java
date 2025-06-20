package de.schosin.ecs.buildtools.codegen.apt.processor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.processing.Generated;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;
import com.palantir.javapoet.WildcardTypeName;

import de.schosin.ecs.buildtools.codegen.apt.processor.ComponentSetDiscoveryProcessor.Plugins;
import de.schosin.ecs.buildtools.codegen.apt.visitor.MethodVisitor;

public class ComponentSetsGenerator {

    public record TypeData(VisitorResult result, MethodSpec factory, MethodSpec entityFactory, List<TypeSpec> types) {
    }

    public record ComponentData(int idx, DeclaredType type, TypeName typeName, String name, ComponentType componentType, List<FieldData> fields) {
    }

    public record FieldData(TypeMirror type, TypeName typeName, String name, Object setter) {
        public static FieldData recordComponent(TypeMirror type, TypeName typeName, String name) {
            return new FieldData(type, typeName, name, name);
        }
    }

    private static final AnnotationSpec GENERATED = AnnotationSpec.builder(Generated.class)
            .addMember("value", "\"%s\"".formatted(ComponentSetsGenerator.class.getName()))
            .addMember("date", "\"%s\"".formatted(Instant.now()))
            .build();

    static final ClassName COMPONENT_TYPE = ClassName.get("de.schosin.ecs.api.components.types", "ComponentType");
    static final ClassName COMPONENT_SET = ClassName.get("de.schosin.ecs.api.components", "ComponentSet");
    static final ClassName COMPONENT_SET_DATA = COMPONENT_SET.nestedClass("ComponentSetData");
    static final ClassName COMPONENT_ACCESSOR = COMPONENT_SET.nestedClass("ComponentAccessor");
    static final ClassName COMPONENT_ACCESSOR_PROCESSOR = COMPONENT_SET.nestedClass("IterableProcessor");
    static final ClassName COMPONENT_SET_TYPE = ClassName.get("de.schosin.ecs.api.components.types", "ComponentSetType");

    private static final ClassName POOL = ClassName.get("de.schosin.ecs.utils.collections", "Pool");
    public static final ClassName POOLED = ClassName.get("de.schosin.ecs.api", "Pooled");

    public static final ClassName DATA_CONVERTER = ClassName.get("de.schosin.ecs.api.data", "DataConverter");
    public static final ClassName DATA_ACCESSOR = ClassName.get("de.schosin.ecs.api.data", "DataAccessor");
    public static final ClassName DATA_PROCESSOR = ClassName.get("de.schosin.ecs.api.data", "DataProcessor");
    public static final ClassName DATA_PROCESSOR_TYPE = ClassName.get("de.schosin.ecs.api.data", "DataProcessorType");
    public static final ClassName ITERABLE_PROCESSOR = ClassName.get("de.schosin.ecs.api.data", "IterableAccessor");

    private static final ClassName BASE_ARCHETYPE = ClassName.get("de.schosin.ecs.plugins.archetype", "BaseArchetype");
    private static final ClassName ARCHETYPE_CONSUMER = BASE_ARCHETYPE.nestedClass("ArchetypeConsumer");

    private static final WildcardTypeName WILDCARD = WildcardTypeName.subtypeOf(Object.class);

    public List<TypeData> generate(Set<? extends Element> elements, Plugins plugins) {
        return elements.stream()
                .flatMap(element -> generate(element, plugins))
                .toList();
    }

    private Stream<TypeData> generate(Element element, Plugins plugins) {
        if (element.getKind() == ElementKind.METHOD) {
            return ComponentSetTypes.create(element.accept(new MethodVisitor((ExecutableElement) element), null), plugins);
        }

        throw new CancelException("Unsupported kind %s: %s".formatted(element.getKind(), element));
    }

    private static class ComponentSetTypes {

        private static Stream<TypeData> create(VisitorResult result, Plugins plugins) {
            var interfaceName = result.interfaceName;

            var factoryMethod = Interface.createFactoryMethod(false, result.factoryName, interfaceName, interfaceName, result.components);
            var factoryMethodEntity = Interface.createFactoryMethod(true, result.factoryName, interfaceName, interfaceName, result.components);

            var types = new ArrayList<TypeSpec>(2);

            var componentSet = Interface.createInterface(result, plugins);
            if (componentSet != null) {
                types.add(componentSet);
            }

            var implementation = Implementation.createImplementation(result, plugins);
            if (implementation != null) {
                types.add(implementation);
            }

            return Stream.of(new TypeData(result, factoryMethod, factoryMethodEntity, types));
        }

        private static class Interface {

            private static TypeSpec createInterface(VisitorResult result, Plugins plugins) {
                var interfaceName = result.interfaceName;
                var implementationName = result.implementationName;
                var components = result.components;

                var componentSetDataType = ParameterizedTypeName.get(COMPONENT_SET_DATA, interfaceName, interfaceName.nestedClass("Processor"));
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

                if (plugins.archetypePlugin()) {
                    var factory = createFactory(result);
                    if (factory != null) {
                        componentSet.addField(createArchetypeSetType(result));
                        componentSet.addType(factory);
                        componentSet.addType(createArchetypeConsumer(result));
                    }
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

            private static FieldSpec createArchetypeSetType(VisitorResult result) {
                var consumer = result.interfaceName.nestedClass("ArchetypeConsumer");
                var archetypeSetType = ClassName.get("de.schosin.ecs.plugins.archetype", "ArchetypeSet").nestedClass("ArchetypeSetType");
                var parameterizedType = ParameterizedTypeName.get(archetypeSetType, result.interfaceName, consumer);

                return FieldSpec.builder(parameterizedType, "ARCHETYPE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $1T<>(TYPE, $2T.class)", archetypeSetType, consumer)
                        .build();
            }

            private static TypeSpec createFactory(VisitorResult result) {
                var methods = result.components.stream()
                        .flatMap(Interface::createFactoryMethods)
                        .toList();

                if (methods.isEmpty()) {
                    return null;
                }

                return TypeSpec.interfaceBuilder("Factory")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.SEALED)
                        .addPermittedSubclass(result.implementationName.nestedClass("FactoryImpl"))
                        .addMethod(factoryGetInstance())
                        .addMethods(methods)
                        .build();
            }

            private static MethodSpec factoryGetInstance() {
                var pooledType = TypeVariableName.get("T", POOLED);

                var classType = ParameterizedTypeName.get(ClassName.get(Class.class), pooledType);
                var parameter = ParameterSpec.builder(classType, "clazz").build();

                return MethodSpec.methodBuilder("getInstance")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addTypeVariable(pooledType)
                        .addParameter(parameter)
                        .returns(pooledType)
                        .build();
            }

            private static Stream<MethodSpec> createFactoryMethods(ComponentData component) {
                if ("".isEmpty()) {
                    return component.componentType.getInterfaceMethods(component);
                }

                var name = component.name;
                var className = ClassName.get("", "Factory");

                var result = new ArrayList<MethodSpec>();

                result.add(MethodSpec.methodBuilder(name)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(component.typeName, name)
                        .returns(className)
                        .build());

                if (!isPooled(component.type)) {
                    return result.stream();
                }

                var consumer = ParameterizedTypeName.get(ClassName.get(Consumer.class), component.typeName);
                result.add(MethodSpec.methodBuilder(name)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(consumer, name)
                        .returns(className)
                        .build());

                var fields = component.fields;
                if (!fields.isEmpty()) {
                    var parameters = fields.stream()
                            .map(field -> ParameterSpec.builder(field.typeName, field.name).build())
                            .toList();

                    result.add(MethodSpec.methodBuilder(name)
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .addParameters(parameters)
                            .returns(className)
                            .build());
                }

                return result.stream();
            }

            private static TypeSpec createArchetypeConsumer(VisitorResult result) {
                var archetype = ParameterizedTypeName.get(BASE_ARCHETYPE, WILDCARD);
                var factory = ClassName.get("", "Factory");
                var factoryImpl = result.implementationName.nestedClass("FactoryImpl");

                var overrideAcceptBody = CodeBlock.builder()
                        .addStatement("var factory = $1T.getFactory(archetype, components, mapping)", factoryImpl)
                        .addStatement("accept(index, factory)")
                        .addStatement("$1T.freeFactory(factory)", factoryImpl)
                        .build();

                var overrideAccept = MethodSpec.methodBuilder("accept")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                        .addParameter(archetype, "archetype")
                        .addParameter(Object[].class, "components")
                        .addParameter(int.class, "index")
                        .addParameter(int[].class, "mapping")
                        .addCode(overrideAcceptBody)
                        .build();

                var accept = MethodSpec.methodBuilder("accept")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(int.class, "index")
                        .addParameter(factory, "factory")
                        .build();

                return TypeSpec.interfaceBuilder("ArchetypeConsumer")
                        .addAnnotation(FunctionalInterface.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addSuperinterface(ARCHETYPE_CONSUMER)
                        .addMethod(overrideAccept)
                        .addMethod(accept)
                        .build();
            }

        }

        private static class Implementation {

            private static TypeSpec createImplementation(VisitorResult result, Plugins plugins) {
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
                        .add("$1T.builder($2T::factory, $3T.INSTANCE)", COMPONENT_SET, implementationName, implementationName.nestedClass("IterableProcessor"));

                var componentSetDataType = ParameterizedTypeName.get(COMPONENT_SET_DATA, interfaceName, interfaceName.nestedClass("Processor"));
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
                    componentSetDataInitializer.add(".add(new $1T<>($2T::%s) {})".formatted(component.name), COMPONENT_ACCESSOR, interfaceName);

                    factoryBody.add("," + System.lineSeparator() + "        ");
                    factoryBody.add("($1T) components[%d]".formatted(i), component.type);

                    getInstance.addParameter(component.typeName, component.name);
                    getInstance.addStatement("instance.%s = %s".formatted(component.name, component.name));

                    if (i > 0) {
                        toStringBody.add(".append(\", \")");
                    }
                    toStringBody.add(System.lineSeparator() + "        ");
                    toStringBody.add(".append(%s)".formatted(component.name));
                }

                componentSetData.initializer(componentSetDataInitializer.add(System.lineSeparator() + "        .build()").build());
                factory.addCode(factoryBody.addStatement(")").build());
                getInstance.addStatement("return instance");

                toStringBody.add(System.lineSeparator() + "        ");
                toStringBody.addStatement(".append(')').toString()");

                var toString = MethodSpec.methodBuilder("toString")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(String.class)
                        .addCode(toStringBody.build())
                        .build();

                if (plugins.archetypePlugin()) {
                    var factoryImpl = FactoryImpl.createFactoryImpl(result);
                    if (factoryImpl != null) {
                        implementation.addType(factoryImpl);
                    }
                }

                return implementation
                        .addField(componentSetData.build())
                        .addMethod(entityIdAccessor)
                        .addMethod(reset.build())
                        .addMethod(free)
                        .addMethod(factory.build())
                        .addMethod(getInstance.build())
                        .addMethod(toString)
                        .addType(createIterableProcessorType(result))
                        .build();
            }

            private static class FactoryImpl {

                private static TypeSpec createFactoryImpl(VisitorResult result) {

                    var methods = IntStream.range(0, result.components.size())
                            .mapToObj(idx -> createFactoryImplMethods(idx, result.components.get(idx)))
                            .flatMap(Function.identity())
                            .toList();

                    if (methods.isEmpty()) {
                        return null;
                    }

                    var superinterface = result.interfaceName.nestedClass("Factory");
                    var archetype = ParameterizedTypeName.get(BASE_ARCHETYPE, WILDCARD);

                    var impl = ClassName.get("", "FactoryImpl");
                    var parameterizedPool = ParameterizedTypeName.get(POOL, impl);

                    var pool = FieldSpec.builder(parameterizedPool, "POOL", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                            .initializer("$1T.unbounded($2T.class, $2T::new)", POOL, impl)
                            .build();

                    return TypeSpec.classBuilder("FactoryImpl")
                            .addModifiers(Modifier.STATIC, Modifier.FINAL)
                            .addSuperinterface(superinterface)
                            .addField(pool)
                            .addField(archetype, "archetype", Modifier.PRIVATE)
                            .addField(Object[].class, "components", Modifier.PRIVATE)
                            .addField(int[].class, "mapping", Modifier.PRIVATE)
                            .addMethod(getFactory())
                            .addMethod(freeFactory())
                            .addMethod(factoryImplGetInstance())
                            .addMethods(methods)
                            .build();
                }

                private static MethodSpec getFactory() {
                    var archetype = ParameterizedTypeName.get(BASE_ARCHETYPE, WILDCARD);

                    var body = CodeBlock.builder()
                            .addStatement("var instance = POOL.getInstance()")
                            .addStatement("instance.archetype = archetype")
                            .addStatement("instance.components = components")
                            .addStatement("instance.mapping = mapping")
                            .addStatement("return instance")
                            .build();

                    return MethodSpec.methodBuilder("getFactory")
                            .addModifiers(Modifier.STATIC)
                            .addParameter(archetype, "archetype")
                            .addParameter(Object[].class, "components")
                            .addParameter(int[].class, "mapping")
                            .returns(ClassName.get("", "FactoryImpl"))
                            .addCode(body)
                            .build();
                }

                private static MethodSpec freeFactory() {
                    return MethodSpec.methodBuilder("freeFactory")
                            .addModifiers(Modifier.STATIC)
                            .addParameter(ClassName.get("", "FactoryImpl"), "factory")
                            .addStatement("POOL.free(factory)")
                            .build();
                }

                private static MethodSpec factoryImplGetInstance() {
                    var pooledType = TypeVariableName.get("T", POOLED);

                    var classType = ParameterizedTypeName.get(ClassName.get(Class.class), pooledType);
                    var parameter = ParameterSpec.builder(classType, "clazz").build();

                    return MethodSpec.methodBuilder("getInstance")
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .addTypeVariable(pooledType)
                            .addParameter(parameter)
                            .returns(pooledType)
                            .addStatement("return archetype.getInstance(clazz)")
                            .build();
                }

                private static Stream<MethodSpec> createFactoryImplMethods(int idx, ComponentData component) {
                    if ("".isEmpty()) {
                        return component.componentType.getImplementationMethods(component);
                    }

                    var factory = ClassName.get("", "FactoryImpl");
                    var result = new ArrayList<MethodSpec>();

                    /*
                     * TODO if type == Relation, add Relation and Relationship+Target variants
                     * TODO if type == Relations, add Relations, Relation and Relationship+Target variants
                     * TODO must not generate methods for non-regular components
                     * TODO don't generate type if only non-regular components
                     * TODO non-regular: ComponentResult, ComponentSet, Data, ???
                     */
                    var specialHandlingOfRelations = switch (component.componentType) {
                        case CLASS -> true;
                        case POOLED -> true;
                        case COMPONENT_RELATION -> true;
                        case COMPONENT_RELATIONS -> true;
                        case ENTITY_RELATION -> true;
                        case ENTITY_RELATIONS -> true;
                        case NON_REGULAR -> false;
                    };

                    result.add(createPojoMethod(idx, factory, component));

                    if (!isPooled(component.type)) {
                        return result.stream();
                    }

                    result.add(createConsumerMethod(idx, factory, component));

                    var fieldsMethod = createFieldsMethod(idx, factory, component);
                    if (fieldsMethod != null) {
                        result.add(fieldsMethod);
                    }

                    return result.stream();
                }

                private static MethodSpec createPojoMethod(int idx, ClassName factory, ComponentData component) {
                    var name = component.name;

                    var body = CodeBlock.builder()
                            .addStatement("this.components[mapping[%d]] = %s".formatted(idx, name))
                            .addStatement("return this")
                            .build();

                    return MethodSpec.methodBuilder(name)
                            .addAnnotation(Override.class)
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .addParameter(component.typeName, name)
                            .returns(factory)
                            .addCode(body)
                            .build();
                }

                private static MethodSpec createConsumerMethod(int idx, ClassName factory, ComponentData component) {
                    var name = component.name;
                    var consumer = ParameterizedTypeName.get(ClassName.get(Consumer.class), component.typeName);

                    var body = CodeBlock.builder()
                            .addStatement("var %s = archetype.getInstance($1T.class)".formatted(name), component.type)
                            .addStatement("consumerArg.accept(%s)".formatted(name))
                            .addStatement("this.components[mapping[%d]] = %s".formatted(idx, name))
                            .addStatement("return this")
                            .build();

                    return MethodSpec.methodBuilder(name)
                            .addAnnotation(Override.class)
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .addParameter(consumer, "consumerArg")
                            .returns(factory)
                            .addCode(body)
                            .build();
                }

                private static MethodSpec createFieldsMethod(int idx, ClassName factory, ComponentData component) {
                    var name = component.name;

                    var fields = component.fields;
                    if (fields.isEmpty()) {
                        return null;
                    }

                    var parameters = fields.stream()
                            .map(field -> ParameterSpec.builder(field.typeName, field.name).build())
                            .toList();

                    var body = CodeBlock.builder();
                    body.addStatement("var %s = archetype.getInstance($1T.class)".formatted(name), component.type);

                    for (var field : fields) {
                        body.addStatement("%s.%s(%s)".formatted(name, field.setter, field.name));
                    }

                    body.addStatement("this.components[mapping[%d]] = %s".formatted(idx, name));
                    body.addStatement("return this");

                    return MethodSpec.methodBuilder(name)
                            .addAnnotation(Override.class)
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .addParameters(parameters)
                            .returns(factory)
                            .addCode(body.build())
                            .build();
                }

            }

            private static TypeSpec createIterableProcessorType(VisitorResult result) {
                var superInterface = ParameterizedTypeName.get(COMPONENT_ACCESSOR_PROCESSOR, result.interfaceName, result.interfaceName.nestedClass("Processor"));

                return TypeSpec.enumBuilder("IterableProcessor")
                        .addModifiers(Modifier.PRIVATE)
                        .addSuperinterface(superInterface)
                        .addEnumConstant("INSTANCE")
                        .addMethod(iterableProcessorImpl(result))
                        .build();
            }

            private static MethodSpec iterableProcessorImpl(VisitorResult result) {
                var components = result.components;
                var n = components.size();

                var methodBody = CodeBlock.builder();

                for (int i = 1; i <= n; i++) {
                    var type = components.get(i - 1).typeName;
                    var converter = ParameterizedTypeName.get(DATA_CONVERTER, type);

                    methodBody.addStatement("var converter%d = ($1T) converters.get(%d)".formatted(i, i - 1), converter);
                }
                methodBody.beginControlFlow("while(accessor.hasNext())");
                methodBody.addStatement("var entityId = accessor.next()");

                methodBody.addStatement("// retrieve components");
                for (int i = 1; i <= n; i++) {
                    methodBody.addStatement("var component%d = converter%d.getComponent(accessor)".formatted(i, i));
                }

                methodBody.addStatement("// process");
                var processStatement = "processor.process(entityId";
                for (int i = 1; i <= n; i++) {
                    processStatement += ", component%d".formatted(i);
                }
                processStatement += ")";

                methodBody.addStatement(processStatement);

                methodBody.addStatement("// free components");
                for (int i = 1; i <= n; i++) {
                    methodBody.addStatement("converter%d.free(component%d)".formatted(i, i));
                }

                methodBody.endControlFlow(); // while

                var converters = ParameterizedTypeName.get(ClassName.get(List.class), WildcardTypeName.subtypeOf(ParameterizedTypeName.get(DATA_CONVERTER, WILDCARD)));

                return MethodSpec.methodBuilder("process")
                        .addAnnotation(Override.class)
                        .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "\"unchecked\"").build())
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(result.interfaceName.nestedClass("Processor"), "processor")
                        .addParameter(ITERABLE_PROCESSOR, "accessor")
                        .addParameter(converters, "converters")
                        .addCode(methodBody.build())
                        .build();
            }

        }

    }

    private static boolean isPooled(DeclaredType type) {
        return doesImplement(type, POOLED.canonicalName());
    }

    public static boolean doesImplement(DeclaredType type, String interfaceName) {
        var element = (TypeElement) type.asElement();
        if (interfaceName.equals(element.getQualifiedName().toString())) {
            return true;
        }

        for (var iface : element.getInterfaces()) {
            if (iface instanceof DeclaredType declared && doesImplement(declared, interfaceName)) {
                return true;
            }
        }

        return false;
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

    public enum ComponentType {

        CLASS {
            @Override
            Stream<MethodSpec> getInterfaceMethods(ComponentData component) {
                var method = MethodSpec.methodBuilder(component.name)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(component.typeName, component.name)
                        .returns(FACTORY)
                        .build();

                return Stream.of(method);
            }

            @Override
            Stream<MethodSpec> getImplementationMethods(ComponentData component) {
                var body = CodeBlock.builder()
                        .addStatement("this.components[mapping[%d]] = %s".formatted(component.idx, component.name))
                        .addStatement("return this")
                        .build();

                var method = MethodSpec.methodBuilder(component.name)
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addParameter(component.typeName, component.name)
                        .returns(FACTORY)
                        .addCode(body)
                        .build();

                return Stream.of(method);
            }
        },

        POOLED {
            @Override
            Stream<MethodSpec> getInterfaceMethods(ComponentData component) {
                var pojoMethod = MethodSpec.methodBuilder(component.name)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(component.typeName, component.name)
                        .returns(FACTORY)
                        .build();

                var consumer = ParameterizedTypeName.get(ClassName.get(Consumer.class), component.typeName);
                var consumerMethod = MethodSpec.methodBuilder(component.name)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(consumer, component.name)
                        .returns(FACTORY)
                        .build();

                var fields = component.fields;
                if (fields.isEmpty()) {
                    return Stream.of(pojoMethod, consumerMethod);
                }

                var parameters = fields.stream()
                        .map(field -> ParameterSpec.builder(field.typeName, field.name).build())
                        .toList();

                var parameterMethod = MethodSpec.methodBuilder(component.name)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameters(parameters)
                        .returns(FACTORY)
                        .build();

                return Stream.of(pojoMethod, consumerMethod, parameterMethod);
            }

            @Override
            Stream<MethodSpec> getImplementationMethods(ComponentData component) {
                var pojoMethod = createPojoMethod(component);
                var consumerMethod = createConsumerMethod(component);

                var fieldsMethod = createFieldsMethod(component);
                if (fieldsMethod == null) {
                    return Stream.of(pojoMethod, consumerMethod);
                }

                return Stream.of(pojoMethod, consumerMethod, fieldsMethod);
            }

            private MethodSpec createPojoMethod(ComponentData component) {
                var name = component.name;

                var body = CodeBlock.builder()
                        .addStatement("this.components[mapping[%d]] = %s".formatted(component.idx, name))
                        .addStatement("return this")
                        .build();

                return MethodSpec.methodBuilder(name)
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addParameter(component.typeName, name)
                        .returns(FACTORY_IMPL)
                        .addCode(body)
                        .build();
            }

            private MethodSpec createConsumerMethod(ComponentData component) {
                var name = component.name;
                var consumer = ParameterizedTypeName.get(ClassName.get(Consumer.class), component.typeName);

                var body = CodeBlock.builder()
                        .addStatement("var %s = archetype.getInstance($1T.class)".formatted(name), component.type)
                        .addStatement("consumerArg.accept(%s)".formatted(name))
                        .addStatement("this.components[mapping[%d]] = %s".formatted(component.idx, name))
                        .addStatement("return this")
                        .build();

                return MethodSpec.methodBuilder(name)
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addParameter(consumer, "consumerArg")
                        .returns(FACTORY_IMPL)
                        .addCode(body)
                        .build();
            }

            private MethodSpec createFieldsMethod(ComponentData component) {
                var name = component.name;

                var fields = component.fields;
                if (fields.isEmpty()) {
                    return null;
                }

                var parameters = fields.stream()
                        .map(field -> ParameterSpec.builder(field.typeName, field.name).build())
                        .toList();

                var body = CodeBlock.builder();
                body.addStatement("var %s = archetype.getInstance($1T.class)".formatted(name), component.type);

                for (var field : fields) {
                    body.addStatement("%s.%s(%s)".formatted(name, field.setter, field.name));
                }

                body.addStatement("this.components[mapping[%d]] = %s".formatted(component.idx, name));
                body.addStatement("return this");

                return MethodSpec.methodBuilder(name)
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addParameters(parameters)
                        .returns(FACTORY_IMPL)
                        .addCode(body.build())
                        .build();
            }

        },

        COMPONENT_RELATION {
            @Override
            Stream<MethodSpec> getInterfaceMethods(ComponentData component) {
                var relationMethod = MethodSpec.methodBuilder(component.name)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(component.typeName, "relation")
                        .returns(FACTORY)
                        .build();

                var relationship = component.type.getTypeArguments().get(0);
                var target = component.type.getTypeArguments().get(1);
                var createMethod = MethodSpec.methodBuilder(component.name)
                        .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                        .addParameter(ClassName.get(relationship), "relationship")
                        .addParameter(ClassName.get(target), "target")
                        .returns(FACTORY)
                        .addStatement("return %s($1T.create(relationship, target))".formatted(component.name), RELATION)
                        .build();

                return Stream.of(relationMethod, createMethod);
            }

            @Override
            Stream<MethodSpec> getImplementationMethods(ComponentData component) {
                var relationMethod = MethodSpec.methodBuilder(component.name)
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addParameter(component.typeName, "relation")
                        .returns(FACTORY)
                        .addStatement("this.components[mapping[%d]] = relation".formatted(component.idx))
                        .addStatement("return this")
                        .build();

                return Stream.of(relationMethod);
            }
        },

        ENTITY_RELATION {
            @Override
            Stream<MethodSpec> getInterfaceMethods(ComponentData component) {
                var implement = true;
                return Stream.empty();
            }

            @Override
            Stream<MethodSpec> getImplementationMethods(ComponentData component) {
                return Stream.empty();
            }
        },

        COMPONENT_RELATIONS {
            @Override
            Stream<MethodSpec> getInterfaceMethods(ComponentData component) {
                /*
                 * TODO name(Relations), name(Relation), name(Relationship, Target)
                 * all merging into existing
                 * must error out if Relations is empty (storage or archetype)
                 */
                var implement = true;
                return Stream.empty();
            }

            @Override
            Stream<MethodSpec> getImplementationMethods(ComponentData component) {
                return Stream.empty();
            }
        },

        ENTITY_RELATIONS {
            @Override
            Stream<MethodSpec> getInterfaceMethods(ComponentData component) {
                var implement = true;
                return Stream.empty();
            }

            @Override
            Stream<MethodSpec> getImplementationMethods(ComponentData component) {
                return Stream.empty();
            }
        },

        NON_REGULAR {
            @Override
            Stream<MethodSpec> getInterfaceMethods(ComponentData component) {
                return Stream.empty();
            }

            @Override
            Stream<MethodSpec> getImplementationMethods(ComponentData component) {
                return Stream.empty();
            }
        };

        private static final ClassName FACTORY = ClassName.get("", "Factory");
        private static final ClassName FACTORY_IMPL = ClassName.get("", "FactoryImpl");

        private static final ClassName RELATION = ClassName.get("de.schosin.ecs.api.components", "Relation");
        private static final ClassName RELATIONS = ClassName.get("de.schosin.ecs.api.components", "Relations");

        abstract Stream<MethodSpec> getInterfaceMethods(ComponentData component);

        abstract Stream<MethodSpec> getImplementationMethods(ComponentData component);

    }

}
