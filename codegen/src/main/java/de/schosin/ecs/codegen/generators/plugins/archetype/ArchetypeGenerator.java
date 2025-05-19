package de.schosin.ecs.codegen.generators.plugins.archetype;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.palantir.javapoet.ArrayTypeName;
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

public class ArchetypeGenerator {

    private static final ClassName ARCHETYPE = ClassName.get("", "Archetype");

    public static final String OF_PREFIX = "Of";

    public static JavaFile generateFile(TypeElement archetype, int maxParams) {
        System.out.println("Process Archetype with %d parameters: %s".formatted(maxParams, archetype));

        var className = ClassName.get(archetype);
        var interfaceBuilder = TypeSpec.interfaceBuilder("Archetype")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(archetype.asType())
                .addTypes(buildTypes(maxParams));

        return JavaFile.builder(className.packageName(), interfaceBuilder.build())
                .addStaticImport(Utils.ARRAY_UTILS, "concat")
                .addStaticImport(Utils.COMPONENT_TYPE, "component")
                .skipJavaLangImports(true)
                .indent(Utils.INDENT)
                .build();
    }

    private static Iterable<TypeSpec> buildTypes(int maxParams) {
        var archetypes = IntStream.range(1, maxParams + 1).mapToObj(idx -> OfN.buildArchetypeX(idx)).toList();

        var types = new ArrayList<TypeSpec>(maxParams + 2);
        types.addAll(archetypes);
        types.add(OfN.buildArchetypeN(maxParams));
        types.add(Creator.buildCreator(maxParams));
        types.add(Initialize.buildInitialize());

        return types;
    }

    private static class OfN {

        private static TypeSpec buildArchetypeX(int n) {
            var name = OF_PREFIX + n;

            var typeVariables = Utils.generateTypeVariables("T", n);
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);
            var parameters = IntStream.range(1, n + 1).mapToObj(idx -> ParameterSpec.builder(typeVariables.get(idx - 1), "component" + idx).build()).toList();

            var self = ParameterizedTypeName.get(ClassName.get("", name), typeVariablesArray);

            var init = buildInit(typeVariables, parameters, false);

            var with = buildWith(self);
            var create = buildCreate(parameters, false);
            var createBatch = createBatch(typeVariablesArray);

            return TypeSpec.interfaceBuilder(name)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addSuperinterface(ARCHETYPE)
                    .addTypeVariables(typeVariables)
                    .addType(init)
                    .addMethods(List.of(with, create, createBatch))
                    .build();
        }

        private static TypeSpec buildArchetypeN(int maxParams) {
            var name = OF_PREFIX + "N";

            var typeVariables = Utils.generateTypeVariables("T", maxParams);
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

            var varargs = ParameterSpec.builder(Object[].class, "components").build();

            var parameters = IntStream.range(1, maxParams + 1).mapToObj(idx -> ParameterSpec.builder(typeVariables.get(idx - 1), "component" + idx).build()).collect(Collectors.toList());
            parameters.add(varargs);

            var self = ParameterizedTypeName.get(ClassName.get("", name), typeVariablesArray);

            var init = buildInit(typeVariables, parameters, true);

            var with = buildWith(self);
            var create = buildCreate(parameters, true);
            var createBatch = createBatch(typeVariablesArray);

            return TypeSpec.interfaceBuilder(name)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addSuperinterface(ARCHETYPE)
                    .addTypeVariables(typeVariables)
                    .addType(init)
                    .addMethods(List.of(with, create, createBatch))
                    .build();
        }

        private static TypeSpec buildInit(List<TypeVariableName> typeVariables, List<ParameterSpec> parameters, boolean varargs) {
            var initialize = MethodSpec.methodBuilder("initialize")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameters(parameters).varargs(varargs)
                    .build();

            return TypeSpec.interfaceBuilder("Init")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addSuperinterface(ARCHETYPE.nestedClass("Initialize").nestedClass("Init"))
                    .addTypeVariables(typeVariables)
                    .addMethods(List.of(initialize))
                    .build();
        }

        private static MethodSpec buildWith(TypeName self) {
            return MethodSpec.methodBuilder("with")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addAnnotation(Override.class)
                    .addParameter(Object[].class, "components").varargs()
                    .returns(self)
                    .build();
        }

        private static MethodSpec buildCreate(List<ParameterSpec> parameters, boolean varargs) {
            return MethodSpec.methodBuilder("create")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameters(parameters).varargs(varargs)
                    .returns(TypeName.INT)
                    .build();
        }

        private static MethodSpec createBatch(TypeVariableName[] typeVariablesArray) {
            var parameterizedInit = ParameterizedTypeName.get(ClassName.get("", "Init"), typeVariablesArray);
            var parameterizedInitialize = ParameterizedTypeName.get(ClassName.get("", "Initialize"), parameterizedInit);

            return MethodSpec.methodBuilder("createBatch")
                    .addJavadoc(Javadoc.CREATE_BATCH)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(TypeName.INT, "count")
                    .addParameter(parameterizedInitialize, "init")
                    .returns(ArrayTypeName.get(int[].class))
                    .build();
        }

    }

    private static class Creator {

        private static TypeSpec buildCreator(int maxParams) {
            var methods = new ArrayList<MethodSpec>(2 * maxParams);

            for (int i = 1; i <= maxParams + 1; i++) {
                var types = Math.min(i, maxParams);

                var typeVariables = Utils.generateTypeVariables("T", types);
                var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

                methods.add(createArchetypeClass(i, maxParams, types, typeVariables, typeVariablesArray));
                methods.add(createArchetypeComponentType(i, maxParams, types, typeVariables, typeVariablesArray));
            }

            methods.add(Utils.CONVERT_REGULAR_COMPONENT_TYPE);

            return TypeSpec.interfaceBuilder("Creator")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addMethods(methods)
                    .build();
        }

        private static MethodSpec createArchetypeClass(int i, int maxParams, int types, List<TypeVariableName> typeVariables, TypeVariableName[] typeVariablesArray) {
            return createArchetype(i, maxParams, types, typeVariables, typeVariablesArray, Utils::clazz, Utils.WILDCARD_CLASS_ARRAY, true);
        }

        private static MethodSpec createArchetypeComponentType(int i, int maxParams, int types, List<TypeVariableName> typeVariables, TypeVariableName[] typeVariablesArray) {
            return createArchetype(i, maxParams, types, typeVariables, typeVariablesArray, Utils::regularComponentType, Utils.REGULAR_COMPONENT_TYPE_WILDCARD_ARRAY, false);
        }

        private static MethodSpec createArchetype(int i, int maxParams, int types, List<TypeVariableName> typeVariables, TypeVariableName[] typeVariablesArray,
                Function<TypeName, ParameterizedTypeName> type, ArrayTypeName varargsType, boolean defaultImpl) {

            var returnType = i <= maxParams
                    ? ParameterizedTypeName.get(ClassName.get("", OF_PREFIX + i), typeVariablesArray)
                    : ParameterizedTypeName.get(ClassName.get("", OF_PREFIX + "N"), typeVariablesArray);

            var methodBuilder = MethodSpec.methodBuilder("createArchetype")
                    .addJavadoc(Javadoc.CREATE_ARCHETYPE)
                    .addModifiers(Modifier.PUBLIC)
                    .addTypeVariables(typeVariables)
                    .returns(returnType);

            for (int j = 1; j < types + 1; j++) {
                var typeVariable = typeVariables.get(j - 1);
                var parameterType = type.apply(typeVariable);

                var param = ParameterSpec.builder(parameterType, "component" + j)
                        .build();

                methodBuilder.addParameter(param);
            }

            if (i == maxParams + 1) {
                var varargs = ParameterSpec.builder(varargsType, "components").build();
                methodBuilder.addParameter(varargs).varargs();
            }

            if (defaultImpl) {
                methodBuilder.addModifiers(Modifier.DEFAULT);

                var body = CodeBlock.builder();

                body.add("return createArchetype(");

                for (int j = 1; j < types + 1; j++) {
                    if (j > 1) {
                        body.add(", ");
                    }
                    body.add("component(component%s)".formatted(j));
                }

                if (i == maxParams + 1) {
                    body.add(", convert(components)");
                }

                body.addStatement(")");

                methodBuilder.addCode(body.build());
            } else {
                methodBuilder.addModifiers(Modifier.ABSTRACT);
            }

            return methodBuilder.build();
        }

    }

    private static class Initialize {

        private static TypeSpec buildInitialize() {
            var typeT = TypeVariableName.get("T");

            var init = TypeSpec.interfaceBuilder("Init")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addMethod(buildGet())
                    .addMethod(buildComponentRelation())
                    .addMethod(buildEntityRelation())
                    .build();

            var initialize = MethodSpec.methodBuilder("initialize")
                    .addJavadoc(Javadoc.INITIALIZE)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(TypeName.INT, "index")
                    .addParameter(typeT, "init")
                    .build();

            return TypeSpec.interfaceBuilder("Initialize")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addTypeVariable(typeT)
                    .addType(init)
                    .addMethod(initialize)
                    .build();
        }

        static MethodSpec buildGet() {
            var pooledType = TypeVariableName.get("T", Utils.POOLED);
            var classT = Utils.clazz(Utils.T);

            return MethodSpec.methodBuilder("get")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addTypeVariable(pooledType)
                    .addParameter(classT, "component")
                    .returns(Utils.T)
                    .build();
        }

        static MethodSpec buildComponentRelation() {
            var returnType = ParameterizedTypeName.get(Utils.COMPONENT_RELATION, Utils.R, Utils.T);

            return MethodSpec.methodBuilder("relation")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addTypeVariable(Utils.R)
                    .addTypeVariable(Utils.T)
                    .addParameter(Utils.R, "relationship")
                    .addParameter(Utils.T, "target")
                    .returns(returnType)
                    .build();
        }

        static MethodSpec buildEntityRelation() {
            var returnType = ParameterizedTypeName.get(Utils.ENTITY_RELATION, Utils.R);

            return MethodSpec.methodBuilder("relation")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addTypeVariable(Utils.R)
                    .addParameter(Utils.R, "relationship")
                    .addParameter(TypeName.INT, "target")
                    .returns(returnType)
                    .build();
        }

    }

    private static class Javadoc {

        public static final String CREATE_BATCH = """
                Create the given number of entities and initialize their components by invoking
                the init callback.
                """;

        public static final String INITIALIZE = """
                Function to initialize the entity with its components. The init function must be called.

                <p>
                Note that for {@link OfN} the passed components must match those classes when the archetype
                was created. Failing to do so may cause {@link ArrayIndexOutOfBoundsException ArrayIndexOutOfBoundsExceptions}
                and other runtime errors to occur.
                </p>
                """;

        private static final String CREATE_ARCHETYPE = """
                Create an archetype for entities with the given component types. Using this over
                {@code world.createEntity(...)} provides better performance.

                <p>
                When adding marker components (e.g. enums or other stateless, non-pooled components) whose values
                are known at compile time, use {@link Archetype#with} instead. That way components like {@code Disabled.INSTANCE}
                will not take up a type parameter, freeing them up for data carrying components.
                </p>

                <p>
                When creating multiple entities at once, using {@code archetype.createBatch(n, this::initEntity)}
                will allow the library to optimize the creation, as calculation of the entity composition is
                only performed when the archetype is created.
                </p>
                """;

    }

}
