package de.schosin.ecs.codegen.generators.plugins.archetype;

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
import de.schosin.ecs.codegen.generators.plugins.datatypes.BaseDataTypeGenerator;

public class ArchetypeManagerGenerator {

    public static final ClassName ARCHETYPE = ClassName.get("de.schosin.ecs.plugins.archetype", "Archetype");
    private static final ClassName ARCHETYPE_CREATOR = ClassName.get("de.schosin.ecs.plugins.archetype", "ArchetypeCreator");

    private static final ClassName ARCHETYPE_MANAGER = ClassName.get("de.schosin.ecs.plugins.archetype", "ArchetypeManager");

    public static JavaFile generateFile(TypeElement type, int maxParams) {
        System.out.println("Process ArchetypeManager with %d parameters: %s".formatted(maxParams, type));

        var baseArchetypeManager = BaseArchetypeManager.create(maxParams);

        var className = ClassName.get(type);
        return JavaFile.builder(className.packageName(), baseArchetypeManager)
                .addStaticImport(Utils.ARRAY_UTILS, "concat")
                .skipJavaLangImports(true)
                .indent(Utils.INDENT)
                .build();
    }

    private static class BaseArchetypeManager {

        private static final ClassName NAME = ClassName.get("", "BaseArchetypeManager");
        private static final String ARCHETYPE_PREFIX = "Archetype";

        private static final ClassName ABSTRACT_ARCHETYPE = ARCHETYPE_MANAGER.nestedClass("AbstractBaseArchetypeImpl");

        public static TypeSpec create(int maxParams) {
            var typesafeCount = FieldSpec.builder(int.class, "TYPESAFE_COUNT", Modifier.PROTECTED, Modifier.STATIC, Modifier.FINAL)
                    .initializer(Integer.toString(maxParams))
                    .build();

            var archetypeNs = IntStream.range(1, maxParams + 1)
                    .mapToObj(n -> createArchetypeN(ARCHETYPE_PREFIX + n, n, false))
                    .toList();

            return TypeSpec.classBuilder(NAME)
                    .addModifiers(Modifier.ABSTRACT)
                    .addSuperinterface(ARCHETYPE_CREATOR)
                    .addField(typesafeCount)
                    .addMethods(creatorMethods(maxParams))
                    .addTypes(archetypeNs)
                    .build();
        }

        private static Iterable<MethodSpec> creatorMethods(int maxParams) {
            return IntStream.range(1, maxParams + 1)
                    .mapToObj(n -> createArchetype(n, false))
                    .toList();
        }

        private static MethodSpec createArchetype(int n, boolean varargs) {
            var typeVariables = Utils.generateTypeVariables("T", n);
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

            var archetypeOf = ClassName.get("", "Archetype" + n);
            var parameterizedArchetypeOf = ParameterizedTypeName.get(archetypeOf, typeVariablesArray);

            var parameters = IntStream.range(1, n + 1)
                    .mapToObj(idx -> ParameterSpec.builder(Utils.regularComponentType(typeVariables.get(idx - 1)), "component" + idx).build())
                    .collect(Collectors.toList());

            var parameterNames = parameters.stream().map(ParameterSpec::name).collect(Collectors.joining(", "));

            if (varargs) {
                parameters.add(ParameterSpec.builder(Utils.REGULAR_COMPONENT_TYPE_WILDCARD_ARRAY, "components").build());
                parameterNames = parameterNames + ", components";
            }

            var implementation = ClassName.get("", ARCHETYPE_PREFIX + n + "Impl");

            return MethodSpec.methodBuilder("createArchetype")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addTypeVariables(typeVariables)
                    .addParameters(parameters).varargs(varargs)
                    .returns(parameterizedArchetypeOf)
                    .addStatement("return new $1T<>(this, %s)".formatted(parameterNames), implementation)
                    .build();
        }

        private static TypeSpec createArchetypeN(String className, int n, boolean varargs) {
            var name = ClassName.get("", className + "Impl");
            var suffix = varargs ? "N" : Integer.toString(n);

            var typeVariables = Utils.generateTypeVariables("T", n);
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

            var archetypeOf = ClassName.get("", "Archetype" + n);
            var parameterizedArchetypeOf = ParameterizedTypeName.get(archetypeOf, typeVariablesArray);

            var superclassProvider = n == 1
                    ? ParameterizedTypeName.get(BaseDataTypeGenerator.DATA_PROVIDER, typeVariables.get(0))
                    : BaseDataTypeGenerator.dataProviderN(n, typeVariables);

            var superclass = ParameterizedTypeName.get(ABSTRACT_ARCHETYPE, superclassProvider);

            var parameters = IntStream.range(1, n + 1)
                    .mapToObj(idx -> ParameterSpec.builder(Utils.regularComponentType(typeVariables.get(idx - 1)), "component" + idx).build())
                    .collect(Collectors.toList());

            var parameterNames = parameters.stream().map(ParameterSpec::name).collect(Collectors.joining(", "));

            if (varargs) {
                parameters.add(ParameterSpec.builder(Utils.REGULAR_COMPONENT_TYPE_WILDCARD_ARRAY, "components").build());
                parameterNames = "concat($1T.class, new $2T[] { %s }, components)".formatted(parameterNames.toString());
            }

            var constructorBody = CodeBlock.builder();
            if (varargs) {
                constructorBody.addStatement("super(manager, %s)".formatted(parameterNames), Utils.REGULAR_COMPONENT_TYPE, Utils.REGULAR_COMPONENT_TYPE_WILDCARD);
            } else {
                constructorBody.addStatement("super(manager, %s)".formatted(parameterNames));
            }

            var constructor = MethodSpec.constructorBuilder()
                    .addParameter(NAME, "manager")
                    .addParameters(parameters).varargs(varargs)
                    .addCode(constructorBody.build())
                    .build();

            var parentParameterized = ParameterizedTypeName.get(name, typeVariablesArray);
            var constructorFixed = MethodSpec.constructorBuilder()
                    .addParameter(NAME, "manager")
                    .addParameter(Object[].class, "fixed")
                    .addParameter(parentParameterized, "parent")
                    .addStatement("super(manager, fixed, parent)")
                    .build();

            return TypeSpec.classBuilder(name)
                    .addModifiers(Modifier.STATIC)
                    .superclass(superclass)
                    .addSuperinterface(parameterizedArchetypeOf)
                    .addTypeVariables(typeVariables)
                    .addMethod(constructor)
                    .addMethod(constructorFixed)
                    .addMethod(with(suffix, n))
                    .build();
        }

        private static MethodSpec with(String suffix, int n) {
            var typeVariables = Utils.generateTypeVariables("T", n);
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

            var returnType = ClassName.get("", "Archetype" + n);
            var parameterizedReturnType = ParameterizedTypeName.get(returnType, typeVariablesArray);

            return MethodSpec.methodBuilder("with")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(Object[].class, "components").varargs()
                    .returns(parameterizedReturnType)
                    .addStatement("return new $1T<>(manager, components, this)", ClassName.get("", ARCHETYPE_PREFIX + n + "Impl"))
                    .build();
        }

    }

}
