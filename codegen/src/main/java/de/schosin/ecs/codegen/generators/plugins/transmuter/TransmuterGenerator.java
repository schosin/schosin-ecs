package de.schosin.ecs.codegen.generators.plugins.transmuter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

public class TransmuterGenerator {

    public static final ClassName TRANSMUTATION_MANAGER = ClassName.get("de.schosin.ecs.engine.components", "TransmutationManager");
    public static final ClassName TRANSMUTATION_MANAGER_BUILDER = TRANSMUTATION_MANAGER.nestedClass("Builder");
    public static final ClassName TRANSMUTATION_MANAGER_ABSTRACT_TRANSMUTER = TRANSMUTATION_MANAGER.nestedClass("AbstractTransmuter");

    private static final ClassName TRANSMUTER = ClassName.get("", "Transmuter");
    private static final ClassName TRANSMUTER_ADD = ClassName.get("", "Transmuter", "Add");
    private static final ClassName BUILDER = ClassName.get("", "Builder");
    private static final ClassName ABSTRACT_BUILDER = ClassName.get("", "AbstractTransmuterBuilder");

    public static final String ADD_PREFIX = "Add";

    public static JavaFile generateFile(TypeElement baseTransmuter, int maxParams) {
        System.out.println("Process Transmuter with %d parameters: %s".formatted(maxParams, baseTransmuter));

        var className = ClassName.get(baseTransmuter);
        var interfaceBuilder = TypeSpec.interfaceBuilder("Transmuter")
                .addJavadoc(Javadoc.TRANSMUTER)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(baseTransmuter.asType())
                .addMethods(buildMethods(maxParams))
                .addTypes(buildTypes(maxParams));

        return JavaFile.builder(className.packageName(), interfaceBuilder.build())
                .addStaticImport(Utils.ARRAY_UTILS, "concat")
                .skipJavaLangImports(true)
                .indent(Utils.INDENT)
                .build();
    }

    private static Iterable<MethodSpec> buildMethods(int maxParams) {
        var methods = new ArrayList<MethodSpec>(maxParams + 2);

        var remove = MethodSpec.methodBuilder("remove")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ParameterSpec.builder(Utils.WILDCARD_CLASS, "component").build())
                .addParameter(ParameterSpec.builder(Utils.WILDCARD_CLASS_ARRAY, "components").build()).varargs()
                .returns(ClassName.get("", "Builder", "Remove"))
                .addStatement("return new Builder.Remove(components).remove(component)")
                .build();

        methods.add(remove);

        for (int i = 1; i <= maxParams + 1; i++) {
            var types = Math.min(i, maxParams);

            var typeVariables = Utils.generateTypeVariables("T", types);
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

            var parameters = IntStream.range(1, types + 1)
                    .mapToObj(idx -> ParameterSpec.builder(Utils.clazz(typeVariables.get(idx - 1)), "component" + idx).build())
                    .collect(Collectors.toList());

            if (i == maxParams + 1) {
                parameters.add(ParameterSpec.builder(Utils.WILDCARD_CLASS_ARRAY, "components").build());
            }

            var name = i <= maxParams
                    ? ADD_PREFIX + i
                    : ADD_PREFIX + "N";

            var returnType = ParameterizedTypeName.get(ClassName.get("", "Builder", name), typeVariablesArray);

            var statementBuilder = new StringBuilder();
            for (int n = 1; n <= types; n++) {
                if (n > 1) {
                    statementBuilder.append(", ");
                }

                statementBuilder.append("component").append(n);
            }

            if (i == maxParams + 1) {
                statementBuilder.append(", ").append("components");
            }

            var statement = "return new Builder.%s<>(%s)".formatted(name, statementBuilder.toString());

            var add = MethodSpec.methodBuilder("add")
                    .addJavadoc(Javadoc.CREATE_TRANSMUTER_ADD)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addTypeVariables(typeVariables)
                    .addParameters(parameters).varargs(i == maxParams + 1)
                    .returns(returnType)
                    .addStatement(statement)
                    .build();

            methods.add(add);
        }

        return methods;
    }

    private static Iterable<TypeSpec> buildTypes(int maxParams) {
        var types = new ArrayList<TypeSpec>(maxParams + 2);
        types.add(buildRemove());
        types.add(buildAdd());
        types.addAll(buildAdds(maxParams));
        types.add(buildBuilder(maxParams));
        types.add(buildCreator(maxParams));

        return types;
    }

    private static TypeSpec buildRemove() {
        var apply = MethodSpec.methodBuilder("apply")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(TypeName.INT, "entityId")
                .returns(TypeName.BOOLEAN)
                .build();

        return TypeSpec.interfaceBuilder("Remove")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addSuperinterface(TRANSMUTER)
                .addMethod(apply)
                .build();
    }

    private static TypeSpec buildAdd() {
        var pooledType = TypeVariableName.get("T", Utils.POOLED);

        var classType = Utils.clazz(pooledType);
        var parameter = ParameterSpec.builder(classType, "clazz").build();

        var getInstance = MethodSpec.methodBuilder("getInstance")
                .addJavadoc(Javadoc.GET_INSTANCE)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addTypeVariable(pooledType)
                .addParameter(parameter)
                .returns(pooledType)
                .build();

        return TypeSpec.interfaceBuilder("Add")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addSuperinterface(TRANSMUTER)
                .addMethod(getInstance)
                .build();
    }

    private static List<TypeSpec> buildAdds(int maxParams) {
        var types = IntStream.range(1, maxParams + 1).mapToObj(TransmuterGenerator::buildAddX).collect(Collectors.toList());
        types.add(buildAddX("AddN", maxParams, true));

        return types;
    }

    private static TypeSpec buildAddX(int n) {
        return buildAddX(ADD_PREFIX + n, n, false);
    }

    private static TypeSpec buildAddX(String name, int n, boolean varargs) {
        var typeVariables = Utils.generateTypeVariables("T", n);
        var parameters = IntStream.range(1, n + 1).mapToObj(idx -> ParameterSpec.builder(typeVariables.get(idx - 1), "component" + idx).build()).collect(Collectors.toList());

        if (varargs) {
            parameters.add(ParameterSpec.builder(Object[].class, "components").build());
        }

        var apply = MethodSpec.methodBuilder("apply")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(TypeName.INT, "entityId")
                .addParameters(parameters).varargs(varargs)
                .returns(TypeName.BOOLEAN)
                .build();

        return TypeSpec.interfaceBuilder(name)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addSuperinterface(TRANSMUTER_ADD)
                .addTypeVariables(typeVariables)
                .addMethod(apply)
                .build();
    }

    private static TypeSpec buildBuilder(int maxParams) {
        var interfaceBuilder = TypeSpec.interfaceBuilder("Builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addSuperinterface(TRANSMUTATION_MANAGER_BUILDER)
                .addMethods(builderAccessors())
                .addType(buildRemoveBuilder(maxParams));

        for (int i = 1; i <= maxParams + 1; i++) {
            var types = Math.min(i, maxParams);
            var typeVariables = Utils.generateTypeVariables("T", types);

            var name = i <= maxParams
                    ? ClassName.get("", "Add" + i)
                    : ClassName.get("", "AddN");

            var addBuilder = buildAddBuilder(name, typeVariables, i == maxParams + 1);
            interfaceBuilder.addType(addBuilder);
        }

        return interfaceBuilder.build();
    }

    private static Iterable<MethodSpec> builderAccessors() {
        var classSet = ParameterizedTypeName.get(ClassName.get(Set.class), Utils.WILDCARD_CLASS);

        var getAdd = MethodSpec.methodBuilder("getAdd")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(classSet)
                .build();

        var getRemove = MethodSpec.methodBuilder("getRemove")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(classSet)
                .build();

        return List.of(getAdd, getRemove);
    }

    private static TypeSpec buildRemoveBuilder(int maxParams) {
        var name = ClassName.get("", "Remove");
        var parameterizedParent = ParameterizedTypeName.get(ABSTRACT_BUILDER, name);

        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ParameterSpec.builder(Utils.WILDCARD_CLASS_ARRAY, "components").build()).varargs()
                .addStatement("remove(components)")
                .build();

        var adds = IntStream.range(1, maxParams + 1).mapToObj(TransmuterGenerator::buildRemoveAddMethod).collect(Collectors.toList());
        adds.add(buildRemoveAddMethod("AddN", maxParams, true));

        return TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .superclass(parameterizedParent)
                .addSuperinterface(BUILDER)
                .addMethod(constructor)
                .addMethods(adds)
                .build();
    }

    private static MethodSpec buildRemoveAddMethod(int n) {
        return buildRemoveAddMethod("Add" + n, n, false);
    }

    private static MethodSpec buildRemoveAddMethod(String addName, int n, boolean varargs) {
        var typeVariables = Utils.generateTypeVariables("T", n);
        var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

        var returnType = ParameterizedTypeName.get(ClassName.get("", "Builder", addName), typeVariablesArray);

        var parameters = IntStream.range(1, typeVariables.size() + 1)
                .mapToObj(i -> ParameterSpec.builder(Utils.clazz(typeVariables.get(i - 1)), "component" + i).build())
                .collect(Collectors.toList());

        var statementArguments = new StringBuilder();
        for (int i = 1; i <= n; i++) {
            if (i > 1) {
                statementArguments.append(", ");
            }

            statementArguments.append("component").append(i);
        }

        var arguments = statementArguments.toString();

        if (varargs) {
            parameters.add(ParameterSpec.builder(Utils.WILDCARD_CLASS_ARRAY, "components").build());
            arguments = arguments + ", components";
        }

        var statement = "return new Builder.%s<>(%s).remove(this.remove)".formatted(addName, arguments);

        return MethodSpec.methodBuilder("add")
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariables(typeVariables)
                .returns(returnType)
                .addParameters(parameters).varargs(varargs)
                .addStatement(statement)
                .build();
    }

    private static TypeSpec buildAddBuilder(ClassName name, List<TypeVariableName> typeVariables, boolean varargs) {
        var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

        var parameterizedName = ParameterizedTypeName.get(name, typeVariablesArray);
        var parameterizedParent = ParameterizedTypeName.get(ABSTRACT_BUILDER, parameterizedName);

        var parameters = IntStream.range(1, typeVariables.size() + 1)
                .mapToObj(i -> ParameterSpec.builder(Utils.clazz(typeVariables.get(i - 1)), "component" + i).build())
                .collect(Collectors.toList());

        var superArguments = new StringBuilder();
        for (int i = 1; i <= typeVariables.size(); i++) {
            if (i > 1) {
                superArguments.append(", ");
            }

            superArguments.append("component" + i);
        }
        var arguments = superArguments.toString();

        if (varargs) {
            parameters.add(ParameterSpec.builder(Utils.WILDCARD_CLASS_ARRAY, "components").build());
            arguments = "concat(Class.class, new Class<?>[] { %s }, components)".formatted(superArguments.toString());
        }

        var statement = "super(%s)".formatted(arguments);

        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameters(parameters).varargs(varargs)
                .addStatement(statement)
                .build();

        return TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .addTypeVariables(typeVariables)
                .superclass(parameterizedParent)
                .addSuperinterface(BUILDER)
                .addMethod(constructor)
                .build();
    }

    private static TypeSpec buildCreator(int maxParams) {
        var interfaceBuilder = TypeSpec.interfaceBuilder("Creator")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        var remove = MethodSpec.methodBuilder("createTransmuter")
                .addJavadoc(Javadoc.CREATE_TRANSMUTER_REMOVE)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(ParameterSpec.builder(ClassName.get("", "Builder", "Remove"), "builder").build())
                .returns(ClassName.get("", "Transmuter", "Remove"))
                .build();

        interfaceBuilder.addMethod(remove);

        for (int i = 1; i <= maxParams + 1; i++) {
            var types = Math.min(i, maxParams);

            var typeVariables = Utils.generateTypeVariables("T", types);
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

            var returnType = i <= maxParams
                    ? ParameterizedTypeName.get(ClassName.get("", ADD_PREFIX + i), typeVariablesArray)
                    : ParameterizedTypeName.get(ClassName.get("", ADD_PREFIX + "N"), typeVariablesArray);

            var parameter = i <= maxParams
                    ? ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get("", "Builder", ADD_PREFIX + i), typeVariablesArray), "builder").build()
                    : ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get("", "Builder", ADD_PREFIX + "N"), typeVariablesArray), "builder").build();

            var createTransmuter = MethodSpec.methodBuilder("createTransmuter")
                    .addJavadoc(Javadoc.CREATE_TRANSMUTER_ADD)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addTypeVariables(typeVariables)
                    .addParameter(parameter)
                    .returns(returnType)
                    .build();

            interfaceBuilder.addMethod(createTransmuter);
        }

        return interfaceBuilder.build();
    }

    private static class Javadoc {

        private static final ClassName COMPONENTS = ClassName.get("de.schosin.ecs.api.components", "Components");

        private static final CodeBlock TRANSMUTER = CodeBlock.builder()
                .addStatement("""
                        Entity transmuter for changing the component composition of entities
                        in one go. Provides better performance than using {@link $1T}
                        individually.

                        <p>
                        Entities modified by a transmuter with more than one modification
                        won't cause intermediate Composition updates.
                        If a transmuter does not change the component composition of an
                        entity, no composition updates will be triggered.
                        </p>
                        """, COMPONENTS)
                .build();

        private static final String GET_INSTANCE = """
                Returns an unused instance for the {@link Pooled pooled} component.

                <p>
                Can be used for applying an adding transmuter, as well as for
                {@link Components#add(int, Object)}, {@link Archetype} or
                {@link World#createEntity(Object...)}.
                </p>

                @param <T> type of component
                @param clazz class of component
                @return unused instance
                """;

        private static final String CREATE_TRANSMUTER_REMOVE = """
                Creates a transmuter that can be used to remove multiple components of an entity
                at once.

                <p>
                Using a transmuter will allow the library to optimize the removal, as
                calculation of the updated entity composition is only performed when the transmuter
                is created.
                </p>
                """;

        private static final String CREATE_TRANSMUTER_ADD = """
                Creates a transmuter that can be used to add and remove multiple components of an entity
                at once.

                <p>
                Using a transmuter will allow the library to optimize the addition and removal, as
                calculation of the updated entity composition is only performed when the transmuter
                is created.
                </p>
                """;

    }

}
