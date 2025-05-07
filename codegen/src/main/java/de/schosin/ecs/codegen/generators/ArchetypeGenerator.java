package de.schosin.ecs.codegen.generators;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.palantir.javapoet.ArrayTypeName;
import com.palantir.javapoet.ClassName;
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
                .skipJavaLangImports(true)
                .indent(Utils.INDENT)
                .build();
    }

    private static Iterable<TypeSpec> buildTypes(int maxParams) {
        var archetypes = IntStream.range(1, maxParams + 1).mapToObj(idx -> buildArchetypeX(idx)).toList();

        var types = new ArrayList<TypeSpec>(maxParams + 2);
        types.addAll(archetypes);
        types.add(buildArchetypeN(maxParams));
        types.add(buildCreator(maxParams));
        types.add(buildInitialize());

        return types;
    }

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

    private static TypeSpec buildCreator(int maxParams) {
        var interfaceBuilder = TypeSpec.interfaceBuilder("Creator")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        for (int i = 1; i <= maxParams + 1; i++) {
            var types = Math.min(i, maxParams);

            var typeVariables = Utils.generateTypeVariables("T", types);
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

            var returnType = i <= maxParams
                    ? ParameterizedTypeName.get(ClassName.get("", OF_PREFIX + i), typeVariablesArray)
                    : ParameterizedTypeName.get(ClassName.get("", OF_PREFIX + "N"), typeVariablesArray);

            var methodBuilder = MethodSpec.methodBuilder("createArchetype")
                    .addJavadoc(Javadoc.CREATE_ARCHETYPE)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addTypeVariables(typeVariables)
                    .returns(returnType);

            for (int j = 1; j < types + 1; j++) {
                var typeVariable = typeVariables.get(j - 1);
                var type = Utils.clazz(typeVariable);

                var param = ParameterSpec.builder(type, "component" + j)
                        .build();

                methodBuilder.addParameter(param);
            }

            if (i == maxParams + 1) {
                var varargs = ParameterSpec.builder(Utils.WILDCARD_CLASS_ARRAY, "components").build();
                methodBuilder.addParameter(varargs).varargs();
            }

            interfaceBuilder.addMethod(methodBuilder.build());
        }

        return interfaceBuilder.build();
    }

    private static TypeSpec buildInitialize() {
        var typeT = TypeVariableName.get("T");

        var init = TypeSpec.interfaceBuilder("Init")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addMethod(buildGet("get"))
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

    static MethodSpec buildGet(String name) {
        var classT = Utils.clazz(Utils.T);

        return MethodSpec.methodBuilder(name)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addTypeVariable(Utils.T)
                .addParameter(classT, "component")
                .returns(Utils.T)
                .build();
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
