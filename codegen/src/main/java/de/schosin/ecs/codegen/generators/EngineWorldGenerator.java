package de.schosin.ecs.codegen.generators;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;

import de.schosin.ecs.codegen.Utils;

public class EngineWorldGenerator {

    private static final ClassName NAME = ClassName.get("", "BaseEngineWorld");

    private static final ClassName ARCHETYPE_MANAGER = ClassName.get("de.schosin.ecs.engine.entities", "ArchetypeManager");
    private static final ClassName TRANSMUTATION_MANAGER = ClassName.get("de.schosin.ecs.engine.components", "TransmutationManager");

    private static final ClassName WORLD = ClassName.get("de.schosin.ecs.api", "World");

    public static JavaFile generateFile(TypeElement type, int archetypeParams, int transmuterParams) {
        System.out.println("Process EngineWorld with %d archetype, %d transmuter parameters: %s".formatted(archetypeParams, transmuterParams, type));

        var baseEngineWorld = baseEngineWorld(archetypeParams, transmuterParams);

        var className = ClassName.get(type);
        return JavaFile.builder(className.packageName(), baseEngineWorld)
                .skipJavaLangImports(true)
                .indent(Utils.INDENT)
                .build();
    }

    private static TypeSpec baseEngineWorld(int archetypeParams, int transmuterParams) {
        var archetypeManagerField = FieldSpec.builder(ARCHETYPE_MANAGER, "archetypeManager", Modifier.PRIVATE).build();
        var transmutationManagerField = FieldSpec.builder(TRANSMUTATION_MANAGER, "transmutationManager", Modifier.PRIVATE).build();

        var init = MethodSpec.methodBuilder("initialize")
                .addModifiers(Modifier.PROTECTED)
                .addParameter(ARCHETYPE_MANAGER, "archetypeManager")
                .addParameter(TRANSMUTATION_MANAGER, "transmutationManager")
                .addCode(CodeBlock.builder()
                        .addStatement("this.archetypeManager = archetypeManager")
                        .addStatement("this.transmutationManager = transmutationManager")
                        .build())
                .build();

        return TypeSpec.classBuilder(NAME)
                .addModifiers(Modifier.ABSTRACT)
                .addSuperinterfaces(List.of(WORLD))
                .addFields(List.of(archetypeManagerField, transmutationManagerField))
                .addMethod(init)
                .addMethods(Archetype.createMethods(archetypeParams))
                .addMethods(Transmuter.createMethods(transmuterParams))
                .build();
    }

    private static class Archetype {

        private static final ClassName ARCHETYPE = ClassName.get("de.schosin.ecs.api.archetype", "Archetype");

        private static Iterable<MethodSpec> createMethods(int maxParams) {
            var methods = new ArrayList<MethodSpec>(maxParams + 1);
            methods.addAll(IntStream.range(1, maxParams + 1).mapToObj(n -> createArchetype(n, false)).toList());
            methods.add(createArchetype(maxParams, true));

            return methods;
        }

        private static MethodSpec createArchetype(int n, boolean varargs) {
            var suffix = varargs ? "N" : Integer.toString(n);

            var typeVariables = Utils.generateTypeVariables("T", n);
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

            var parameters = IntStream.range(1, n + 1).mapToObj(idx -> ParameterSpec.builder(Utils.clazz(typeVariables.get(idx - 1)), "component" + idx).build()).collect(Collectors.toList());
            var parameterNames = parameters.stream().map(ParameterSpec::name).collect(Collectors.joining(", "));

            if (varargs) {
                parameters.add(ParameterSpec.builder(Utils.WILDCARD_CLASS_ARRAY, "components").build());
                parameterNames = parameterNames + ", components";
            }

            var returnType = ARCHETYPE.nestedClass(ArchetypeGenerator.OF_PREFIX + suffix);
            var parameterizedReturnType = ParameterizedTypeName.get(returnType, typeVariablesArray);

            return MethodSpec.methodBuilder("createArchetype")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addTypeVariables(typeVariables)
                    .returns(parameterizedReturnType)
                    .addParameters(parameters).varargs(varargs)
                    .addStatement("return archetypeManager.createArchetype(%s)".formatted(parameterNames))
                    .build();
        }

    }

    private static class Transmuter {

        private static final ClassName TRANSMUTER = ClassName.get("de.schosin.ecs.api.archetype", "Transmuter");

        private static Iterable<MethodSpec> createMethods(int maxParams) {
            var methods = new ArrayList<MethodSpec>(maxParams + 2);
            methods.add(createRemoveTransmuter());
            methods.addAll(IntStream.range(1, maxParams + 1).mapToObj(n -> createTransmuterN(TransmuterGenerator.ADD_PREFIX + n, n)).toList());
            methods.add(createTransmuterN("AddN", maxParams));

            return methods;
        }

        private static MethodSpec createRemoveTransmuter() {
            var parameterType = TRANSMUTER.nestedClass("Builder").nestedClass("Remove");
            var returnType = TRANSMUTER.nestedClass("Remove");

            return MethodSpec.methodBuilder("createTransmuter")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addParameter(parameterType, "builder")
                    .returns(returnType)
                    .addStatement("return transmutationManager.createTransmuter(builder)")
                    .build();
        }

        private static MethodSpec createTransmuterN(String name, int n) {
            var typeVariables = Utils.generateTypeVariables("T", n);
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

            var parameterType = TRANSMUTER.nestedClass("Builder").nestedClass(name);
            var parameterizedParameter = ParameterizedTypeName.get(parameterType, typeVariablesArray);

            var returnType = TRANSMUTER.nestedClass(name);
            var parameterizedReturnType = ParameterizedTypeName.get(returnType, typeVariablesArray);

            return MethodSpec.methodBuilder("createTransmuter")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addTypeVariables(typeVariables)
                    .returns(parameterizedReturnType)
                    .addParameter(parameterizedParameter, "builder")
                    .addStatement("return transmutationManager.createTransmuter(builder)")
                    .build();
        }

    }

}
