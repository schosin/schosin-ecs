package de.schosin.ecs.codegen.generators;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;

import de.schosin.ecs.codegen.Utils;

public class TransmutationManagerGenerator {

    public static final ClassName TRANSMUTER = ClassName.get("de.schosin.ecs.api.archetype", "Transmuter");
    public static final ClassName TRANSMUTER_BUILDER = TRANSMUTER.nestedClass("Builder");
    public static final ClassName TRANSMUTER_CREATOR = TRANSMUTER.nestedClass("Creator");

    public static final ClassName TRANSMUTATION_MANAGER = ClassName.get("de.schosin.ecs.engine.components", "TransmutationManager");
    public static final ClassName ABSTRACT_ADD_TRANSMUTER = TRANSMUTATION_MANAGER.nestedClass("AbstractAddTransmuter");

    public static JavaFile generateFile(TypeElement type, int maxParams) {
        System.out.println("Process TransmutationManager with %d parameters: %s".formatted(maxParams, type));

        var baseTransmutationManager = BaseTransmutationManager.create(maxParams);

        var className = ClassName.get(type);
        return JavaFile.builder(className.packageName(), baseTransmutationManager)
                .addStaticImport(Utils.ARRAY_UTILS, "concat")
                .skipJavaLangImports(true)
                .indent(Utils.INDENT)
                .build();
    }

    private static class BaseTransmutationManager {

        private static final String TRANSMUTER_ADD_PREFIX = "TransmuterAdd";

        private static final ClassName NAME = ClassName.get("", "BaseTransmutationManager");

        public static TypeSpec create(int maxParams) {
            return TypeSpec.classBuilder(NAME)
                    .addModifiers(Modifier.ABSTRACT)
                    .addSuperinterface(TRANSMUTER_CREATOR)
                    .addMethod(getTransmuter())
                    .addMethods(creatorMethods(maxParams))
                    .addTypes(TransmuterAddN.transmuterAdds(maxParams))
                    .build();
        }

        private static MethodSpec getTransmuter() {
            var transmuterT = TypeVariableName.get("T", TRANSMUTER);
            var instanceSupplier = ParameterizedTypeName.get(Utils.SUPPLIER, transmuterT);

            return MethodSpec.methodBuilder("getTransmuter")
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .addTypeVariable(transmuterT)
                    .addParameter(TRANSMUTER_BUILDER, "builder")
                    .addParameter(instanceSupplier, "supplier")
                    .returns(Utils.T)
                    .build();
        }

        private static Iterable<MethodSpec> creatorMethods(int maxParams) {
            var methods = new ArrayList<MethodSpec>(maxParams + 1);
            methods.addAll(IntStream.range(1, maxParams + 1).mapToObj(n -> createTransmuter(Integer.toString(n), n)).toList());
            methods.add(createTransmuter("N", maxParams));

            return methods;
        }

        private static MethodSpec createTransmuter(String suffix, int n) {
            var typeVariables = Utils.generateTypeVariables("T", n);
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

            var parameter = TRANSMUTER_BUILDER.nestedClass(TransmuterGenerator.ADD_PREFIX + suffix);
            var parameterizedParameter = ParameterizedTypeName.get(parameter, typeVariablesArray);

            var returnType = TRANSMUTER.nestedClass(TransmuterGenerator.ADD_PREFIX + suffix);
            var parameterizedReturnType = ParameterizedTypeName.get(returnType, typeVariablesArray);

            var implementation = ClassName.get("", TRANSMUTER_ADD_PREFIX + suffix);

            return MethodSpec.methodBuilder("createTransmuter")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addTypeVariables(typeVariables)
                    .addParameter(parameterizedParameter, "builder")
                    .returns(parameterizedReturnType)
                    .addStatement("return getTransmuter(builder, () -> new $1T<>(this, builder))", implementation)
                    .build();
        }

        private static class TransmuterAddN {

            private static Iterable<TypeSpec> transmuterAdds(int maxParams) {
                var types = new ArrayList<TypeSpec>(maxParams + 1);
                types.addAll(IntStream.range(1, maxParams + 1).mapToObj(n -> createTransmuterAddN(n, false)).toList());
                types.add(createTransmuterAddN(maxParams, true));

                return types;
            }

            private static TypeSpec createTransmuterAddN(int n, boolean varargs) {
                var suffix = varargs ? "N" : Integer.toString(n);
                var name = ClassName.get("", TRANSMUTER_ADD_PREFIX + suffix);

                var typeVariables = Utils.generateTypeVariables("T", n);
                var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

                var superinterface = TRANSMUTER.nestedClass(TransmuterGenerator.ADD_PREFIX + suffix);
                var parameterizedSuperinterface = ParameterizedTypeName.get(superinterface, typeVariablesArray);

                var parameter = TRANSMUTER_BUILDER.nestedClass(TransmuterGenerator.ADD_PREFIX + suffix);
                var parameterizedParameter = ParameterizedTypeName.get(parameter, typeVariablesArray);

                var constructor = MethodSpec.constructorBuilder()
                        .addParameter(NAME, "manager")
                        .addParameter(parameterizedParameter, "builder")
                        .addStatement("super(manager, builder)")
                        .build();

                return TypeSpec.classBuilder(name)
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .superclass(ABSTRACT_ADD_TRANSMUTER)
                        .addSuperinterface(parameterizedSuperinterface)
                        .addTypeVariables(typeVariables)
                        .addMethod(constructor)
                        .addMethod(apply(n, varargs))
                        .build();
            }

            private static MethodSpec apply(int n, boolean varargs) {
                var typeVariables = Utils.generateTypeVariables("T", n);

                var parameters = IntStream.range(1, n + 1).mapToObj(idx -> ParameterSpec.builder(typeVariables.get(idx - 1), "component" + idx).build()).collect(Collectors.toList());
                var parameterNames = parameters.stream().map(ParameterSpec::name).collect(Collectors.joining(", "));

                if (varargs) {
                    parameters.add(ParameterSpec.builder(Object[].class, "components").build());
                    parameterNames = "concat(Object.class, new Object[] { %s }, components)".formatted(parameterNames.toString());
                }

                return MethodSpec.methodBuilder("apply")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(TypeName.INT, "entityId")
                        .addParameters(parameters).varargs(varargs)
                        .returns(TypeName.BOOLEAN)
                        .addStatement("return super.apply(entityId, %s)".formatted(parameterNames))
                        .build();
            }

        }

    }

}
