package de.schosin.ecs.codegen.generators.plugins.archetype;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;

import de.schosin.ecs.codegen.Utils;
import de.schosin.ecs.codegen.generators.plugins.datatypes.BaseDataTypeGenerator;

public class BaseArchetypeGenerator {

    public static final ClassName ARCHETYPE = ClassName.get("de.schosin.ecs.plugins.archetype", "BaseArchetype");
    public static final ClassName IMMUTABLE_INT_BAG = ClassName.get("de.schosin.ecs.utils.collections", "ImmutableIntBag");
    private static final ClassName BASE_ARCHETYPE_MANAGER = ClassName.get("", "BaseArchetypeManager");
    private static final ClassName COMPONENT_DATA_STORAGE = ARCHETYPE.nestedClass("ComponentDataStorage");

    public static ClassName dataTypeN(int n) {
        if (n <= 1) {
            throw new IllegalArgumentException("n must be 2 or greater");
        }

        return ClassName.get("de.schosin.ecs.plugins.data.types", "DataType" + n);
    }

    public static List<JavaFile> generateFiles(TypeElement archetype, int maxParams) {
        var packageName = ClassName.get(archetype).packageName();

        var files = new ArrayList<JavaFile>(maxParams + 1);
        files.add(ArchetypeCreator.create(packageName, maxParams));
        files.add(Archetype1.create(packageName));
        files.addAll(IntStream.range(2, maxParams + 1).mapToObj(n -> ArchetypeN.create(packageName, n)).toList());

        return files;
    }

    private static class ArchetypeCreator {

        public static JavaFile create(String packageName, int maxParams) {
            return JavaFile.builder(packageName, createArchetypeCreator(maxParams))
                    .addStaticImport(Utils.COMPONENT_TYPE, "component")
                    .skipJavaLangImports(true)
                    .indent(Utils.INDENT)
                    .build();
        }

        public static TypeSpec createArchetypeCreator(int maxParams) {
            return TypeSpec.interfaceBuilder("ArchetypeCreator")
                    .addModifiers(Modifier.PUBLIC)
                    .addMethods(createFactoryMethods(maxParams))
                    .build();
        }

        private static Iterable<MethodSpec> createFactoryMethods(int maxParams) {
            var methods = new ArrayList<MethodSpec>(maxParams);
            methods.add(createClassFactoryMethod());
            methods.addAll(IntStream.range(2, maxParams + 1).mapToObj(n -> createClassFactoryMethod(n)).toList());
            methods.add(createComponentTypeFactoryMethod());
            methods.addAll(IntStream.range(2, maxParams + 1).mapToObj(n -> createComponentTypeFactoryMethod(n)).toList());

            return methods;
        }

        private static MethodSpec createClassFactoryMethod() {
            var archetype = ParameterizedTypeName.get(ClassName.get("", "Archetype1"), Utils.T);

            return MethodSpec.methodBuilder("createArchetype")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addTypeVariable(Utils.T)
                    .addParameter(Utils.clazz(Utils.T), "clazz")
                    .returns(archetype)
                    .addStatement("return createArchetype(component(clazz))")
                    .build();
        }

        private static MethodSpec createComponentTypeFactoryMethod() {
            var componentType = Utils.regularComponentType(Utils.WILDCARD, Utils.R);
            var archetype = ParameterizedTypeName.get(ClassName.get("", "Archetype1"), Utils.R);

            return MethodSpec.methodBuilder("createArchetype")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addTypeVariable(Utils.R)
                    .addParameter(componentType, "componentType")
                    .returns(archetype)
                    .build();
        }

        private static MethodSpec createClassFactoryMethod(int n) {
            var variables = BaseDataTypeGenerator.getTypeVariables(n);
            var typeVariables = variables.typeVariablesT();

            var parameters = IntStream.range(1, typeVariables.size() + 1)
                    .mapToObj(i -> ParameterSpec.builder(Utils.clazz(typeVariables.get(i - 1)), "component" + i).build())
                    .toList();

            var archetype = ParameterizedTypeName.get(ClassName.get("", "Archetype" + n), variables.typeVariablesArrayT());

            var components = "";
            for (int i = 1; i <= typeVariables.size(); i++) {
                if (i > 1) {
                    components += ", ";
                }

                components += "component(component%d)".formatted(i);
            }

            return MethodSpec.methodBuilder("createArchetype")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addTypeVariables(typeVariables)
                    .addParameters(parameters)
                    .returns(archetype)
                    .addStatement("return createArchetype(%s)".formatted(components))
                    .build();
        }

        private static MethodSpec createComponentTypeFactoryMethod(int n) {
            var variables = BaseDataTypeGenerator.getTypeVariables(n);
            var typeVariables = variables.typeVariablesR();

            var parameters = IntStream.range(1, typeVariables.size() + 1)
                    .mapToObj(i -> ParameterSpec.builder(Utils.regularComponentType(Utils.WILDCARD, typeVariables.get(i - 1)), "component" + i).build())
                    .toList();

            var archetype = ParameterizedTypeName.get(ClassName.get("", "Archetype" + n), variables.typeVariablesArrayR());

            return MethodSpec.methodBuilder("createArchetype")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addTypeVariables(typeVariables)
                    .addParameters(parameters)
                    .returns(archetype)
                    .build();
        }

    }

    private static class Archetype1 {

        public static JavaFile create(String packageName) {
            return JavaFile.builder(packageName, createArchetype1())
                    .skipJavaLangImports(true)
                    .indent(Utils.INDENT)
                    .build();
        }

        static TypeSpec createArchetype1() {
            var consumer = ParameterizedTypeName.get(ClassName.get("", "Archetype1").nestedClass("ArchetypeConsumer1"), Utils.T);
            var archetype = ParameterizedTypeName.get(ARCHETYPE, consumer);

            return TypeSpec.interfaceBuilder("Archetype1")
                    .addModifiers(Modifier.PUBLIC, Modifier.SEALED)
                    .addPermittedSubclass(BASE_ARCHETYPE_MANAGER.nestedClass("Archetype1Impl"))
                    .addTypeVariable(Utils.T)
                    .addSuperinterface(archetype)
                    .addType(factory())
                    .addType(consumer())
                    .addMethod(create())
                    .addMethod(createBatchSupplier())
                    .addMethod(createBatchIntFunction())
                    .addMethod(with())
                    .build();
        }

        private static TypeSpec factory() {
            var create = MethodSpec.methodBuilder("create")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(Utils.T, "component")
                    .build();

            return TypeSpec.interfaceBuilder("Factory1")
                    .addAnnotation(FunctionalInterface.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addTypeVariable(Utils.T)
                    .addMethod(create)
                    .build();
        }

        private static TypeSpec consumer() {
            var archetypeConsumer = ARCHETYPE.nestedClass("ArchetypeConsumer");

            var factory = ParameterizedTypeName.get(ClassName.get("", "Factory1"), Utils.T);

            var accept = MethodSpec.methodBuilder("accept")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(TypeName.INT, "index")
                    .addParameter(factory, "factory")
                    .build();

            var body = CodeBlock.builder()
                    .addStatement("accept(index, component1 -> components.set(mapping[0], component1))")
                    .build();

            var archetypeConsumerAccept = MethodSpec.methodBuilder("accept")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addParameter(COMPONENT_DATA_STORAGE, "components")
                    .addParameter(TypeName.INT, "index")
                    .addParameter(int[].class, "mapping")
                    .addCode(body)
                    .build();

            return TypeSpec.interfaceBuilder("ArchetypeConsumer1")
                    .addAnnotation(FunctionalInterface.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addSuperinterface(archetypeConsumer)
                    .addTypeVariable(Utils.T)
                    .addMethod(accept)
                    .addMethod(archetypeConsumerAccept)
                    .build();
        }

        private static MethodSpec create() {
            return MethodSpec.methodBuilder("create")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addParameter(Utils.T, "component1")
                    .returns(TypeName.INT)
                    .addStatement("return create((i, factory) -> factory.create(component1))")
                    .build();
        }

        private static MethodSpec createBatchSupplier() {
            var supplier = ParameterizedTypeName.get(ClassName.get(Supplier.class), Utils.T);

            return MethodSpec.methodBuilder("createBatch")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addParameter(TypeName.INT, "count")
                    .addParameter(supplier, "supplier")
                    .returns(IMMUTABLE_INT_BAG)
                    .addStatement("return createBatch(count, (i, factory) -> factory.create(supplier.get()))")
                    .build();
        }

        private static MethodSpec createBatchIntFunction() {
            var function = ParameterizedTypeName.get(ClassName.get(IntFunction.class), Utils.T);

            return MethodSpec.methodBuilder("createBatch")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addParameter(TypeName.INT, "count")
                    .addParameter(function, "function")
                    .returns(IMMUTABLE_INT_BAG)
                    .addStatement("return createBatch(count, (i, factory) -> factory.create(function.apply(i)))")
                    .build();
        }

        private static MethodSpec with() {
            var archetype = ParameterizedTypeName.get(ClassName.get("", "Archetype1"), Utils.T);

            return MethodSpec.methodBuilder("with")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(Object[].class, "components").varargs()
                    .returns(archetype)
                    .build();
        }

    }

    private static class ArchetypeN {

        public static JavaFile create(String packageName, int n) {
            return JavaFile.builder(packageName, createArchetypeN(n))
                    .skipJavaLangImports(true)
                    .indent(Utils.INDENT)
                    .build();
        }

        static TypeSpec createArchetypeN(int n) {
            var variables = BaseDataTypeGenerator.getTypeVariables(n);
            var typeVariables = variables.typeVariablesR();

            var consumerN = ParameterizedTypeName.get(ClassName.get("", "Archetype" + n).nestedClass("ArchetypeConsumer" + n), variables.typeVariablesArrayR());
            var archetype = ParameterizedTypeName.get(ARCHETYPE, consumerN);

            return TypeSpec.interfaceBuilder("Archetype" + n)
                    .addModifiers(Modifier.PUBLIC, Modifier.SEALED)
                    .addPermittedSubclass(BASE_ARCHETYPE_MANAGER.nestedClass("Archetype%dImpl".formatted(n)))
                    .addTypeVariables(typeVariables)
                    .addSuperinterface(archetype)
                    .addType(factory(typeVariables))
                    .addType(consumer(typeVariables))
                    .addMethod(create(typeVariables))
                    .addMethod(createConsumer(typeVariables))
                    .addMethod(createBatchConsumer(typeVariables))
                    .addMethod(with(typeVariables))
                    .build();
        }

        private static TypeSpec factory(List<TypeVariableName> typeVariables) {
            var n = typeVariables.size();

            var parameters = IntStream.range(1, n + 1)
                    .mapToObj(i -> ParameterSpec.builder(typeVariables.get(i - 1), "component" + i).build())
                    .toList();

            var create = MethodSpec.methodBuilder("create")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameters(parameters)
                    .build();

            return TypeSpec.interfaceBuilder("Factory" + n)
                    .addAnnotation(FunctionalInterface.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addTypeVariables(typeVariables)
                    .addMethod(create)
                    .build();
        }

        private static TypeSpec consumer(List<TypeVariableName> typeVariables) {
            var n = typeVariables.size();
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

            var archetypeConsumer = ARCHETYPE.nestedClass("ArchetypeConsumer");

            var factory = ParameterizedTypeName.get(ClassName.get("", "Factory" + n), typeVariablesArray);

            var accept = MethodSpec.methodBuilder("accept")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(TypeName.INT, "index")
                    .addParameter(factory, "factory")
                    .build();

            var components = IntStream.range(1, n + 1)
                    .mapToObj(i -> "component" + i)
                    .collect(Collectors.joining(", "));

            var body = CodeBlock.builder();
            body.add("accept(index, (%s) -> {\n".formatted(components)).indent();

            for (int i = 1; i <= n; i++) {
                body.addStatement("components.set(mapping[%d], component%d)".formatted(i - 1, i));
            }

            body.unindent().add("});");

            var archetypeConsumerAccept = MethodSpec.methodBuilder("accept")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addParameter(COMPONENT_DATA_STORAGE, "components")
                    .addParameter(TypeName.INT, "index")
                    .addParameter(int[].class, "mapping")
                    .addCode(body.build())
                    .build();

            return TypeSpec.interfaceBuilder("ArchetypeConsumer" + n)
                    .addAnnotation(FunctionalInterface.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addSuperinterface(archetypeConsumer)
                    .addTypeVariables(typeVariables)
                    .addMethod(accept)
                    .addMethod(archetypeConsumerAccept)
                    .build();
        }

        private static MethodSpec create(List<TypeVariableName> typeVariables) {
            var parameters = IntStream.range(1, typeVariables.size() + 1)
                    .mapToObj(i -> ParameterSpec.builder(typeVariables.get(i - 1), "component" + i).build())
                    .toList();

            var components = "";
            for (int i = 1; i <= typeVariables.size(); i++) {
                if (i > 1) {
                    components += ", ";
                }

                components += "component" + i;
            }

            return MethodSpec.methodBuilder("create")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addParameters(parameters)
                    .returns(TypeName.INT)
                    .addStatement("return create((i, factory) -> factory.create(%s))".formatted(components))
                    .build();
        }

        private static MethodSpec createConsumer(List<TypeVariableName> typeVariables) {
            var n = typeVariables.size();
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

            var factory = ParameterizedTypeName.get(ClassName.get("", "Factory" + n), typeVariablesArray);
            var consumer = ParameterizedTypeName.get(ClassName.get(Consumer.class), factory);

            return MethodSpec.methodBuilder("create")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addParameter(consumer, "consumer")
                    .returns(TypeName.INT)
                    .addStatement("return create((i, factory) -> consumer.accept(factory))")
                    .build();
        }

        private static MethodSpec createBatchConsumer(List<TypeVariableName> typeVariables) {
            var n = typeVariables.size();
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

            var factory = ParameterizedTypeName.get(ClassName.get("", "Factory" + n), typeVariablesArray);
            var consumer = ParameterizedTypeName.get(ClassName.get(Consumer.class), factory);

            return MethodSpec.methodBuilder("createBatch")
                    .addJavadoc(JAVA_DOC)
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addParameter(TypeName.INT, "count")
                    .addParameter(consumer, "consumer")
                    .returns(IMMUTABLE_INT_BAG)
                    .addStatement("return createBatch(count, (i, factory) -> consumer.accept(factory))")
                    .build();
        }

        private static MethodSpec with(List<TypeVariableName> typeVariables) {
            var archetype = ParameterizedTypeName.get(ClassName.get("", "Archetype" + typeVariables.size()), typeVariables.toArray(TypeVariableName[]::new));

            return MethodSpec.methodBuilder("with")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(Object[].class, "components").varargs()
                    .returns(archetype)
                    .build();
        }

        private static final CodeBlock JAVA_DOC = CodeBlock.of("""
                Creates a batch of entities. All components passed to the factory must be non-null.

                <p>
                Example (Lambda):
                {@snippet:
                var entityIds = archetype.createBatch(10, factory -> factory.create(component1, component2));
                }
                </p>

                @param count number of entities to create, {@code consumer} will be invoked that many times
                @param consumer callback, must invoke {@code factory.create(...)}
                @return id of entity
                @return bag of entity ids, instance usable until the next {@link $1T#process()}
                """, Utils.WORLD);

    }

}
