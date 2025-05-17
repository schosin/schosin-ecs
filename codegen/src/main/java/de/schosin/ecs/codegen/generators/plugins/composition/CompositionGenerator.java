package de.schosin.ecs.codegen.generators.plugins.composition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import org.jspecify.annotations.NonNull;

import com.palantir.javapoet.ArrayTypeName;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;

import de.schosin.ecs.codegen.Utils;

public class CompositionGenerator {

    private static final ClassName COMPOSITION = ClassName.get("", "Composition");
    private static final ClassName BASE_COMPOSITION = ClassName.get("", "BaseComposition");
    private static final ClassName SPEC = ClassName.get("", "Spec");

    private static final ClassName GROUP = ClassName.get("", "BaseComposition", "Group");
    private static final ParameterizedTypeName UNARY_GROUP = ParameterizedTypeName.get(ClassName.get(UnaryOperator.class), GROUP);

    private static final ClassName BUILDER = ClassName.get("", "Builder");
    private static final ArrayTypeName BUILDER_ARRAY = ArrayTypeName.of(BUILDER);

    public static final String OF_PREFIX = "Of";

    public static JavaFile generateFile(TypeElement composition, int maxParams) {
        System.out.println("Process Composition with %d parameters: %s".formatted(maxParams, composition));

        var className = ClassName.get(composition);
        return JavaFile.builder(className.packageName(), Composition.create(maxParams))
                .skipJavaLangImports(true)
                .indent(Utils.INDENT)
                .build();
    }

    private static class Composition {

        public static TypeSpec create(int maxParams) {
            return TypeSpec.interfaceBuilder(COMPOSITION)
                    .addModifiers(Modifier.PUBLIC)
                    .addSuperinterfaces(List.of(BASE_COMPOSITION, SPEC))
                    .addMethods(staticMethods())
                    .addMethods(createRetrieveMethods("retrieve", 1, maxParams))
                    .addTypes(buildTypes(maxParams))
                    .build();
        }

        private static Iterable<MethodSpec> staticMethods() {
            var all = MethodSpec.methodBuilder("all")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(BUILDER)
                    .addStatement("return builder()")
                    .build();

            var allComponents = MethodSpec.methodBuilder("all")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(Utils.REGULAR_COMPONENT_TYPE_WILDCARD_ARRAY, "components").varargs()
                    .returns(BUILDER)
                    .addStatement("return builder().all(components)")
                    .build();

            var allClasses = MethodSpec.methodBuilder("all")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(Utils.WILDCARD_CLASS_ARRAY, "classes").varargs()
                    .returns(BUILDER)
                    .addStatement("return builder().all(classes)")
                    .build();

            var allBuilders = MethodSpec.methodBuilder("all")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(BUILDER_ARRAY, "builders").varargs()
                    .returns(BUILDER)
                    .addStatement("return builder().all(builders)")
                    .build();

            var allUnary = MethodSpec.methodBuilder("all")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(UNARY_GROUP, "consumer")
                    .returns(BUILDER)
                    .addStatement("return builder().all(consumer)")
                    .build();

            var oneComponents = MethodSpec.methodBuilder("one")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(Utils.REGULAR_COMPONENT_TYPE_WILDCARD_ARRAY, "components").varargs()
                    .returns(BUILDER)
                    .addStatement("return builder().one(components)")
                    .build();

            var oneClasses = MethodSpec.methodBuilder("one")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(Utils.WILDCARD_CLASS_ARRAY, "classes").varargs()
                    .returns(BUILDER)
                    .addStatement("return builder().one(classes)")
                    .build();

            var oneBuilders = MethodSpec.methodBuilder("one")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(BUILDER_ARRAY, "builders").varargs()
                    .returns(BUILDER)
                    .addStatement("return builder().one(builders)")
                    .build();

            var oneUnary = MethodSpec.methodBuilder("one")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(UNARY_GROUP, "consumer")
                    .returns(BUILDER)
                    .addStatement("return builder().one(consumer)")
                    .build();

            var noneComponents = MethodSpec.methodBuilder("none")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(Utils.REGULAR_COMPONENT_TYPE_WILDCARD_ARRAY, "components").varargs()
                    .returns(BUILDER)
                    .addStatement("return builder().none(components)")
                    .build();

            var noneClasses = MethodSpec.methodBuilder("none")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(Utils.WILDCARD_CLASS_ARRAY, "classes").varargs()
                    .returns(BUILDER)
                    .addStatement("return builder().none(classes)")
                    .build();

            var noneBuilders = MethodSpec.methodBuilder("none")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(BUILDER_ARRAY, "builders").varargs()
                    .returns(BUILDER)
                    .addStatement("return builder().none(builders)")
                    .build();

            var noneUnary = MethodSpec.methodBuilder("none")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(UNARY_GROUP, "consumer")
                    .returns(BUILDER)
                    .addStatement("return builder().none(consumer)")
                    .build();

            var builder = MethodSpec.methodBuilder("builder")
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                    .returns(BUILDER)
                    .addStatement("return new $1T()", BUILDER)
                    .build();

            return List.of(
                    all, allComponents, allClasses, allBuilders, allUnary,
                    oneComponents, oneClasses, oneBuilders, oneUnary,
                    noneComponents, noneClasses, noneBuilders, noneUnary,
                    builder);
        }

        private static Iterable<TypeSpec> buildTypes(int maxParams) {
            var of = buildOf();
            var compositions = IntStream.range(1, maxParams + 1).mapToObj(idx -> buildCompositionN(of, idx, maxParams)).toList();

            var types = new ArrayList<TypeSpec>(maxParams + 2);
            types.add(of);
            types.addAll(compositions);
            types.add(Builder.create());
            types.add(buildCreator(maxParams));

            return types;
        }

        private static TypeSpec buildOf() {
            var typeVariable = TypeVariableName.get("C");

            var processEntity = MethodSpec.methodBuilder("process")
                    .addJavadoc(Javadoc.PROCESS_ENTITY)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(TypeName.INT, "entityId")
                    .addParameter(ParameterSpec.builder(typeVariable, "process").addAnnotation(NonNull.class).build())
                    .build();

            var process = MethodSpec.methodBuilder("process")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(ParameterSpec.builder(typeVariable, "process").addAnnotation(NonNull.class).build())
                    .build();

            var inserted = MethodSpec.methodBuilder("inserted")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(ParameterSpec.builder(typeVariable, "callback").addAnnotation(NonNull.class).build())
                    .build();

            var removed = MethodSpec.methodBuilder("removed")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(ParameterSpec.builder(typeVariable, "callback").addAnnotation(NonNull.class).build())
                    .build();

            return TypeSpec.interfaceBuilder(OF_PREFIX)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.SEALED)
                    .addTypeVariable(typeVariable)
                    .addSuperinterfaces(List.of(BASE_COMPOSITION, SPEC))
                    .addMethods(List.of(processEntity, process, inserted, removed))
                    .build();
        }

        private static TypeSpec buildCompositionN(TypeSpec of, int n, int maxParams) {
            var name = OF_PREFIX + n;

            var typeVariables = Utils.generateTypeVariables("T", n);
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);
            var parameters = IntStream.range(1, n + 1).mapToObj(idx -> ParameterSpec.builder(typeVariables.get(idx - 1), "component" + idx).build()).toList();

            var consume = MethodSpec.methodBuilder("consume")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(TypeName.INT, "entityId")
                    .addParameters(parameters)
                    .build();

            var consumer = TypeSpec.interfaceBuilder("Consumer")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addTypeVariables(typeVariables)
                    .addMethod(consume)
                    .build();

            var parameterizedConsumer = ParameterizedTypeName.get(ClassName.get("", name, consumer.name()), typeVariablesArray);
            var parameterizedOf = ParameterizedTypeName.get(ClassName.get("", of.name()), parameterizedConsumer);

            return TypeSpec.interfaceBuilder(name)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.NON_SEALED)
                    .addTypeVariables(typeVariables)
                    .addSuperinterface(parameterizedOf)
                    .addMethods(createRetrieveMethods("and", n + 1, maxParams))
                    .addType(consumer)
                    .build();
        }

        private static List<MethodSpec> createRetrieveMethods(String name, int start, int maxParams) {
            if (maxParams - start < 0) {
                return List.of();
            }

            var methods = new ArrayList<MethodSpec>(2 * (maxParams - start));
            methods.addAll(IntStream.range(start, maxParams + 1).mapToObj(idx -> createComponentRetrieveMethod(name, start, idx)).toList());
            methods.addAll(IntStream.range(start, maxParams + 1).mapToObj(idx -> createClassRetrieveMethod(name, start, idx)).toList());

            return methods;
        }

        private static MethodSpec createComponentRetrieveMethod(String name, int start, int n) {
            var typeVariables = Utils.generateTypeVariables("T", start, n);

            var returnTypeVariablesArray = Utils.generateTypeVariables("T", 1, n).toArray(TypeVariableName[]::new);

            var returnName = start == 1 ? OF_PREFIX + (start + n - 1) : OF_PREFIX + (n);
            var returnType = ClassName.get("", returnName);
            var returnTypeParameterized = ParameterizedTypeName.get(returnType, returnTypeVariablesArray);

            var parameters = IntStream.range(0, typeVariables.size())
                    .mapToObj(idx -> ParameterSpec.builder(Utils.componentType(Utils.WILDCARD, typeVariables.get(idx)), "component" + (start + idx)).build())
                    .toList();

            return MethodSpec.methodBuilder(name)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addTypeVariables(typeVariables)
                    .addParameters(parameters)
                    .returns(returnTypeParameterized)
                    .build();
        }

        private static MethodSpec createClassRetrieveMethod(String name, int start, int n) {
            var typeVariables = Utils.generateTypeVariables("T", start, n);

            var returnTypeVariablesArray = Utils.generateTypeVariables("T", 1, n).toArray(TypeVariableName[]::new);

            var returnName = start == 1 ? OF_PREFIX + (start + n - 1) : OF_PREFIX + (n);
            var returnType = ClassName.get("", returnName);
            var returnTypeParameterized = ParameterizedTypeName.get(returnType, returnTypeVariablesArray);

            var parameters = IntStream.range(0, typeVariables.size())
                    .mapToObj(idx -> ParameterSpec.builder(Utils.clazz(typeVariables.get(idx)), "component" + (start + idx)).build())
                    .toList();

            return MethodSpec.methodBuilder(name)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addTypeVariables(typeVariables)
                    .addParameters(parameters)
                    .returns(returnTypeParameterized)
                    .build();
        }

        private static TypeSpec buildCreator(int maxParams) {
            var parameterBuilder = ParameterSpec.builder(BUILDER, "builder")
                    .build();

            var createComposition = MethodSpec.methodBuilder("createComposition")
                    .addJavadoc(Javadoc.CREATE_COMPOSITION)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(parameterBuilder)
                    .returns(COMPOSITION)
                    .build();

            var interfaceBuilder = TypeSpec.interfaceBuilder("Creator")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addMethod(createComposition);

            for (int i = 1; i <= maxParams; i++) {
                var typeVariables = Utils.generateTypeVariables("T", i);
                var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

                var returnType = ParameterizedTypeName.get(ClassName.get("", OF_PREFIX + i), typeVariablesArray);

                var componentParameters = IntStream.range(1, i + 1)
                        .mapToObj(j -> ParameterSpec.builder(Utils.componentType(Utils.WILDCARD, typeVariables.get(j - 1)), "component" + j).build())
                        .toList();

                var classParameters = IntStream.range(1, i + 1)
                        .mapToObj(j -> ParameterSpec.builder(Utils.clazz(typeVariables.get(j - 1)), "component" + j).build())
                        .toList();

                var parameterNames = classParameters.stream().map(ParameterSpec::name).collect(Collectors.joining(", "));

                var classMethod = MethodSpec.methodBuilder("createComposition")
                        .addJavadoc(Javadoc.CREATE_COMPOSITION_OF)
                        .addTypeVariables(typeVariables)
                        .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                        .addParameter(parameterBuilder)
                        .addParameters(classParameters)
                        .returns(returnType)
                        .addStatement("return createComposition(builder).retrieve(%s)".formatted(parameterNames))
                        .build();

                var componentMethod = MethodSpec.methodBuilder("createComposition")
                        .addJavadoc(Javadoc.CREATE_COMPOSITION_OF)
                        .addTypeVariables(typeVariables)
                        .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                        .addParameter(parameterBuilder)
                        .addParameters(componentParameters)
                        .returns(returnType)
                        .addStatement("return createComposition(builder).retrieve(%s)".formatted(parameterNames))
                        .build();

                interfaceBuilder.addMethod(classMethod);
                interfaceBuilder.addMethod(componentMethod);
            }

            return interfaceBuilder.build();
        }

    }

    private static class Builder {

        private static final ParameterizedTypeName SET_GROUP = ParameterizedTypeName.get(ClassName.get(Set.class), GROUP);
        private static final ClassName HASH_SET = ClassName.get(HashSet.class);

        private static TypeSpec create() {
            return TypeSpec.classBuilder(BUILDER)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .addFields(createFields())
                    .addMethods(createAllMethods())
                    .addMethods(createAccessors())
                    .addMethod(createToString())
                    .build();
        }

        private static Iterable<FieldSpec> createFields() {
            var all = FieldSpec.builder(GROUP, "all")
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .initializer("new $1T()", GROUP)
                    .build();

            var ones = FieldSpec.builder(SET_GROUP, "ones")
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .initializer("new $1T<>()", HASH_SET)
                    .build();

            var none = FieldSpec.builder(GROUP, "none")
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .initializer("new $1T()", GROUP)
                    .build();

            return List.of(all, ones, none);
        }

        private static Iterable<MethodSpec> createAllMethods() {
            return List.of(
                    createComponentMethod("all", Javadoc.ALL_COMPONENTS),
                    createClassMethod("all", Javadoc.ALL_CLASSES),
                    createBuilderMethod("all", Javadoc.ALL_BUILDERS),
                    createUnaryOperatorMethod("all", Javadoc.ALL_UNARY),
                    createComponentOne(),
                    createClassOne(),
                    createBuilderOne(),
                    createUnaryOperatorOne(),
                    createComponentMethod("none", Javadoc.NONE_COMPONENTS),
                    createClassMethod("none", Javadoc.NONE_CLASSES),
                    createBuilderMethod("none", Javadoc.NONE_BUILDERS),
                    createUnaryOperatorMethod("none", Javadoc.NONE_UNARY));
        }

        private static MethodSpec createComponentMethod(String name, String javadoc) {
            return MethodSpec.methodBuilder(name)
                    .addJavadoc(javadoc)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(Utils.REGULAR_COMPONENT_TYPE_WILDCARD_ARRAY, "components").varargs()
                    .returns(BUILDER)
                    .addStatement("%s.add(components)".formatted(name))
                    .addStatement("return this")
                    .build();
        }

        private static MethodSpec createClassMethod(String name, String javadoc) {
            return MethodSpec.methodBuilder(name)
                    .addJavadoc(javadoc)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(Utils.WILDCARD_CLASS_ARRAY, "classes").build()).varargs()
                    .returns(BUILDER)
                    .addStatement("%s.add(classes)".formatted(name))
                    .addStatement("return this")
                    .build();
        }

        private static MethodSpec createBuilderMethod(String name, String javadoc) {
            return MethodSpec.methodBuilder(name)
                    .addJavadoc(javadoc)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(ArrayTypeName.of(BUILDER), "builders").build()).varargs()
                    .returns(BUILDER)
                    .addStatement("%s.add(builders)".formatted(name))
                    .addStatement("return this")
                    .build();
        }

        private static MethodSpec createUnaryOperatorMethod(String name, CodeBlock javadoc) {
            return MethodSpec.methodBuilder(name)
                    .addJavadoc(javadoc)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(UNARY_GROUP, "operator").build())
                    .returns(BUILDER)
                    .addStatement("operator.apply(%s)".formatted(name))
                    .addStatement("return this")
                    .build();
        }

        private static MethodSpec createComponentOne() {
            return MethodSpec.methodBuilder("one")
                    .addJavadoc(Javadoc.ONE_COMPONENTS)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(Utils.REGULAR_COMPONENT_TYPE_WILDCARD_ARRAY, "components").varargs()
                    .returns(BUILDER)
                    .addCode(CodeBlock.builder()
                            .beginControlFlow("if (components.length == 0)")
                            .addStatement("return this")
                            .endControlFlow()
                            .addStatement("return one(group -> group.add(components))")
                            .build())
                    .build();
        }

        private static MethodSpec createClassOne() {
            return MethodSpec.methodBuilder("one")
                    .addJavadoc(Javadoc.ONE_CLASSES)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(Utils.WILDCARD_CLASS_ARRAY, "classes").build()).varargs()
                    .returns(BUILDER)
                    .addCode(CodeBlock.builder()
                            .beginControlFlow("if (classes.length == 0)")
                            .addStatement("return this")
                            .endControlFlow()
                            .addStatement("return one(group -> group.add(classes))")
                            .build())
                    .build();
        }

        private static MethodSpec createBuilderOne() {
            return MethodSpec.methodBuilder("one")
                    .addJavadoc(Javadoc.ONE_BUILDERS)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(ArrayTypeName.of(BUILDER), "builders").build()).varargs()
                    .returns(BUILDER)
                    .addCode(CodeBlock.builder()
                            .beginControlFlow("if (builders.length == 0)")
                            .addStatement("return this")
                            .endControlFlow()
                            .addStatement("return one(group -> group.add(builders))")
                            .build())
                    .build();
        }

        private static MethodSpec createUnaryOperatorOne() {
            return MethodSpec.methodBuilder("one")
                    .addJavadoc(Javadoc.ONE_UNARY)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(UNARY_GROUP, "operator").build())
                    .returns(BUILDER)
                    .addCode(CodeBlock.builder()
                            .addStatement("var group = new $1T()", GROUP)
                            .addStatement("operator.apply(group)".formatted())
                            .beginControlFlow("if (group.isEmpty())")
                            .addStatement("throw new IllegalStateException(\"Group is empty, add atleast one class or builder.\")")
                            .endControlFlow()
                            .addStatement("this.ones.add(group)")
                            .addStatement("return this")
                            .build())
                    .build();
        }

        private static Iterable<MethodSpec> createAccessors() {
            var getAll = MethodSpec.methodBuilder("getAll")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .returns(GROUP)
                    .addStatement("return this.all")
                    .build();

            var getOnes = MethodSpec.methodBuilder("getOnes")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .returns(SET_GROUP)
                    .addStatement("return this.ones")
                    .build();

            var getNone = MethodSpec.methodBuilder("getNone")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .returns(GROUP)
                    .addStatement("return this.none")
                    .build();

            return List.of(getAll, getOnes, getNone);
        }

        private static MethodSpec createToString() {
            return MethodSpec.methodBuilder("toString")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(String.class)
                    .addCode(CodeBlock.builder()
                            .addStatement("var comma = false")
                            .addStatement("var builder = new $1T()", StringBuilder.class)
                            .addStatement("builder.append(this.getClass().getSimpleName()).append(\"(\")")
                            .beginControlFlow("if (!this.all.isEmpty())")
                            .addStatement("builder.append(this.all)")
                            .addStatement("comma = true")
                            .endControlFlow()
                            .beginControlFlow("if (!this.ones.isEmpty())")
                            .beginControlFlow("if (comma)")
                            .addStatement("builder.append(\", \")")
                            .endControlFlow()
                            .addStatement("builder.append(this.ones)")
                            .addStatement("comma = true")
                            .endControlFlow()
                            .beginControlFlow("if (!this.none.isEmpty())")
                            .beginControlFlow("if (comma)")
                            .addStatement("builder.append(\", \")")
                            .endControlFlow()
                            .addStatement("builder.append(this.none)")
                            .endControlFlow()
                            .addStatement("builder.append(\")\")")
                            .addStatement("return builder.toString()")
                            .build())
                    .build();
        }

    }

    private static class Javadoc {

        private static final String PROCESS_ENTITY = """
                Process the passed entity as if it was part of the {@code process} call.
                """;

        private static final String ALL_COMPONENTS = """
                Adds the components to this builder, limiting the composition to entities that have
                all of the passed components.

                @param components types of components
                @return this builder
                """;

        private static final String ALL_CLASSES = """
                Adds the classes to this builder, limiting the composition to entities that have
                all of the passed components.

                @param classes classes of components
                @return this builder
                """;

        private static final String ALL_BUILDERS = """
                Adds the nested builders to this builder, limiting the composition to entities that match
                all of the passed builders.

                @param builders nested builders
                @return this builder
                """;

        private static final CodeBlock ALL_UNARY = CodeBlock.of("""
                Applies the {@code operator} to this builder, modifying the composition to only match
                entities that match all of the requirements set on the {@link $1T Group}.

                @param operator callback
                @return this builder
                """, GROUP);

        private static final String ONE_COMPONENTS = """
                Adds the components to this builder as a new group, limiting the composition to entities that have
                any of the passed components.

                @param components types of components
                @return this builder
                """;

        private static final String ONE_CLASSES = """
                Adds the classes to this builder as a new group, limiting the composition to entities that have
                any of the passed components.

                @param classes classes of components
                @return this builder
                """;

        private static final String ONE_BUILDERS = """
                Adds the nested builders to this builder as a new group, limiting the composition to entities
                that match any of the passed builders.

                @param builders nested builders
                @return this builder
                """;

        private static final CodeBlock ONE_UNARY = CodeBlock.of("""
                Applies the {@code operator} to a new {@link $1T Group} that is added this builder,
                modifying the composition to only match entities that match any of the requirements
                set on the {@link $1T Group}.

                @param operator callback
                @return this builder
                """, GROUP);

        private static final String NONE_COMPONENTS = """
                Adds the components to this builder, limiting the composition to entities that have
                none of the passed components.

                @param components types of components
                @return this builder
                """;

        private static final String NONE_CLASSES = """
                Adds the classes to this builder, limiting the composition to entities that have
                none of the passed components.

                @param classes classes of components
                @return this builder
                """;

        private static final String NONE_BUILDERS = """
                Adds the nested builders to this builder, limiting the composition to entities that match
                none of the passed builders.

                @param builders nested builders
                @return this builder
                """;

        private static final CodeBlock NONE_UNARY = CodeBlock.of("""
                Applies the {@code operator} to this builder, modifying the composition to only match
                entities that match none of the requirements set on the {@link $1T Group}.

                @param operator callback
                @return this builder
                """, GROUP);

        private static final String CREATE_COMPOSITION = """
                Create a composition that can be used to process entities and to register lifecycle callbacks for entities
                that match the composition defined by the builder.

                <p>
                See the other overloads of this methods if access to components is required.
                </p>
                """;

        private static final String CREATE_COMPOSITION_OF = """
                Create a composition that can be used to process entities and to register lifecycle callbacks for entities
                that match the composition defined by the builder.

                <p>
                Provides overloaded methods of {@code process}, {@code intersted} and {@code removed} that provide the
                components matching the class arguments when creating this composition.
                </p>
                """;

    }

}
