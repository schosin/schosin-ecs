package de.schosin.ecs.codegen.generators.plugins.archetype;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

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

public class ArchetypeManagerGenerator {

    public static final ClassName ARCHETYPE = ClassName.get("de.schosin.ecs.plugins.archetype", "Archetype");
    private static final ClassName ARCHETYPE_CREATOR = ARCHETYPE.nestedClass("Creator");

    private static final ClassName ARCHETYPE_MANAGER = ClassName.get("de.schosin.ecs.plugins.archetype", "ArchetypeManager");
    private static final ClassName COMPONENT_MANAGER = ClassName.get("de.schosin.ecs.engine.components", "ComponentManager");

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

        private static final ClassName ABSTRACT_ARCHETYPE = ARCHETYPE_MANAGER.nestedClass("AbstractArchetypeImpl");

        public static TypeSpec create(int maxParams) {
            var archetypeNs = IntStream.range(1, maxParams + 1)
                    .mapToObj(n -> createArchetypeN(ARCHETYPE_PREFIX + n, n, false))
                    .toList();

            return TypeSpec.classBuilder(NAME)
                    .addModifiers(Modifier.ABSTRACT)
                    .addSuperinterface(ARCHETYPE_CREATOR)
                    .addMethods(creatorMethods(maxParams))
                    .addMethod(createArchetype(maxParams, true))
                    .addType(Initialize.create(maxParams))
                    .addTypes(archetypeNs)
                    .addType(createArchetypeN(ARCHETYPE_PREFIX + "N", maxParams, true))
                    .build();
        }

        private static Iterable<MethodSpec> creatorMethods(int maxParams) {
            return IntStream.range(1, maxParams + 1)
                    .mapToObj(n -> createArchetype(n, false))
                    .toList();
        }

        private static MethodSpec createArchetype(int n, boolean varargs) {
            var suffix = varargs ? "N" : Integer.toString(n);

            var typeVariables = Utils.generateTypeVariables("T", n);
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

            var archetypeOf = ARCHETYPE.nestedClass(ArchetypeGenerator.OF_PREFIX + suffix);
            var parameterizedArchetypeOf = ParameterizedTypeName.get(archetypeOf, typeVariablesArray);

            var parameters = IntStream.range(1, n + 1).mapToObj(idx -> ParameterSpec.builder(Utils.clazz(typeVariables.get(idx - 1)), "component" + idx).build()).collect(Collectors.toList());
            var parameterNames = parameters.stream().map(ParameterSpec::name).collect(Collectors.joining(", "));

            if (varargs) {
                parameters.add(ParameterSpec.builder(Utils.WILDCARD_CLASS_ARRAY, "components").build());
                parameterNames = parameterNames + ", components";
            }

            var implementation = ClassName.get("", ARCHETYPE_PREFIX + suffix);

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
            var name = ClassName.get("", className);
            var suffix = varargs ? "N" : Integer.toString(n);

            var typeVariables = Utils.generateTypeVariables("T", n);
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

            var archetypeOf = ARCHETYPE.nestedClass(ArchetypeGenerator.OF_PREFIX + suffix);
            var parameterizedArchetypeOf = ParameterizedTypeName.get(archetypeOf, typeVariablesArray);

            var parameters = IntStream.range(1, n + 1).mapToObj(idx -> ParameterSpec.builder(Utils.clazz(typeVariables.get(idx - 1)), "component" + idx).build()).collect(Collectors.toList());
            var parameterNames = parameters.stream().map(ParameterSpec::name).collect(Collectors.joining(", "));

            if (varargs) {
                parameters.add(ParameterSpec.builder(Utils.WILDCARD_CLASS_ARRAY, "components").build());
                parameterNames = "concat(Class.class, new Class<?>[] { %s }, components)".formatted(parameterNames.toString());
            }

            var constructor = MethodSpec.constructorBuilder()
                    .addParameter(NAME, "manager")
                    .addParameters(parameters).varargs(varargs)
                    .addStatement("super(manager, %s)".formatted(parameterNames))
                    .build();

            var parentParameterized = ParameterizedTypeName.get(name, typeVariablesArray);
            var constructorFixed = MethodSpec.constructorBuilder()
                    .addParameter(NAME, "manager")
                    .addParameter(Object[].class, "fixed")
                    .addParameter(parentParameterized, "parent")
                    .addStatement("super(manager, fixed, parent)".formatted(parameterNames))
                    .build();

            return TypeSpec.classBuilder(name)
                    .addModifiers(Modifier.STATIC)
                    .superclass(ABSTRACT_ARCHETYPE)
                    .addSuperinterface(parameterizedArchetypeOf)
                    .addTypeVariables(typeVariables)
                    .addMethod(constructor)
                    .addMethod(constructorFixed)
                    .addMethod(with(suffix, n))
                    .addMethod(createEntity(n, varargs))
                    .addMethod(createBatch(n))
                    .build();
        }

        private static MethodSpec with(String suffix, int n) {
            var typeVariables = Utils.generateTypeVariables("T", n);
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

            var returnType = ARCHETYPE.nestedClass(ArchetypeGenerator.OF_PREFIX + suffix);
            var parameterizedReturnType = ParameterizedTypeName.get(returnType, typeVariablesArray);

            return MethodSpec.methodBuilder("with")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(Object[].class, "components").varargs()
                    .returns(parameterizedReturnType)
                    .addStatement("return new $1T<>(manager, components, this)", ClassName.get("", ARCHETYPE_PREFIX + suffix))
                    .build();
        }

        private static MethodSpec createEntity(int n, boolean varargs) {
            var typeVariables = Utils.generateTypeVariables("T", n);

            var parameters = IntStream.range(1, n + 1).mapToObj(idx -> ParameterSpec.builder(typeVariables.get(idx - 1), "component" + idx).build()).collect(Collectors.toList());
            var parameterNames = parameters.stream().map(ParameterSpec::name).collect(Collectors.joining(", "));

            if (varargs) {
                parameters.add(ParameterSpec.builder(Object[].class, "components").build());
                parameterNames = "concat(Object.class, new Object[] { %s }, components)".formatted(parameterNames.toString());
            }

            return MethodSpec.methodBuilder("create")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameters(parameters).varargs(varargs)
                    .returns(TypeName.INT)
                    .addStatement("return createEntity(%s)".formatted(parameterNames))
                    .build();
        }

        private static MethodSpec createBatch(int n) {
            var typeVariablesArray = Utils.generateTypeVariables("T", n).toArray(TypeVariableName[]::new);

            var init = ClassName.get("", "Init");
            var parameterizedInit = ParameterizedTypeName.get(init, typeVariablesArray);

            var initialize = ARCHETYPE.nestedClass("Initialize");
            var parameterizedInitialize = ParameterizedTypeName.get(initialize, parameterizedInit);

            return MethodSpec.methodBuilder("createBatch")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(TypeName.INT, "count")
                    .addParameter(parameterizedInitialize, "init")
                    .returns(ArrayTypeName.of(TypeName.INT))
                    .addStatement("return createEntities(count, init)")
                    .build();
        }

    }

    private static class Initialize {

        private static final ClassName INITIALIZE = ClassName.get("", "InitializeImpl");
        private static final ClassName POOLED_COMPONENT = ClassName.get("de.schosin.ecs.engine.components", "Component").nestedClass("PooledComponent");

        public static TypeSpec create(int maxParams) {
            var superinterfaces = IntStream.range(1, maxParams + 1)
                    .mapToObj(n -> ARCHETYPE.nestedClass("Of" + n).nestedClass("Init"))
                    .toList();

            var superinterfaceN = ARCHETYPE.nestedClass("OfN").nestedClass("Init");

            var componentsField = FieldSpec.builder(Utils.bag(Utils.OBJECT), "components", Modifier.PROTECTED, Modifier.FINAL)
                    .initializer("new $1T<>($2T.class, 8)", Utils.BAG, Utils.OBJECT)
                    .build();

            var constructor = MethodSpec.constructorBuilder()
                    .addParameter(COMPONENT_MANAGER, "componentManager")
                    .addStatement("this.componentManager = componentManager")
                    .build();

            return TypeSpec.classBuilder(INITIALIZE)
                    .addAnnotation(Utils.SUPPRESS_RAWTYPES)
                    .addModifiers(Modifier.STATIC, Modifier.FINAL)
                    .addSuperinterface(Utils.POOLED)
                    .addSuperinterfaces(superinterfaces)
                    .addSuperinterface(superinterfaceN)
                    .addField(COMPONENT_MANAGER, "componentManager", Modifier.PRIVATE, Modifier.FINAL)
                    .addField(componentsField)
                    .addField(TypeName.INT, "size", Modifier.PROTECTED)
                    .addField(TypeName.INT, "added", Modifier.PROTECTED)
                    .addField(TypeName.BOOLEAN, "valid", Modifier.PROTECTED)
                    .addMethod(constructor)
                    .addMethod(reset())
                    .addMethod(getComponent())
                    .addMethods(initializeMethods(maxParams))
                    .build();
        }

        private static MethodSpec reset() {
            var body = CodeBlock.builder()
                    .addStatement("this.components.clear()")
                    .addStatement("this.size = 0")
                    .addStatement("this.added = 0")
                    .addStatement("this.valid = false")
                    .build();

            return MethodSpec.methodBuilder("reset")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addCode(body)
                    .build();
        }

        private static MethodSpec getComponent() {
            return MethodSpec.methodBuilder("get")
                    .addAnnotation(Override.class)
                    .addAnnotation(Utils.SUPPRESS_UNCHECKED)
                    .addModifiers(Modifier.PUBLIC)
                    .addTypeVariable(Utils.T)
                    .returns(Utils.T)
                    .addParameter(Utils.clazz(Utils.T), "clazz")
                    .addStatement("var component = ($1T<T>) componentManager.getComponent(clazz)", POOLED_COMPONENT)
                    .addStatement("return component.getInstance()")
                    .build();
        }

        private static Iterable<MethodSpec> initializeMethods(int maxParams) {
            var methods = new ArrayList<MethodSpec>(maxParams + 1);
            methods.addAll(IntStream.range(1, maxParams + 1).mapToObj(n -> initialize(n)).toList());
            methods.add(initializeN(maxParams));

            return methods;
        }

        private static MethodSpec initialize(int n) {
            var parameters = IntStream.range(1, n + 1).mapToObj(i -> ParameterSpec.builder(Object.class, "component" + i).build()).toList();

            var body = CodeBlock.builder();

            for (int i = 0; i < n; i++) {
                body.addStatement("this.components.set(%d, component%d)".formatted(i, i + 1));
            }

            body.addStatement("this.added = %d".formatted(n));
            body.addStatement("this.valid = true");

            return MethodSpec.methodBuilder("initialize")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameters(parameters)
                    .addCode(body.build())
                    .build();
        }

        private static MethodSpec initializeN(int maxParams) {
            var parameters = IntStream.range(1, maxParams + 1).mapToObj(i -> ParameterSpec.builder(Object.class, "component" + i).build()).toList();

            var body = CodeBlock.builder();

            for (int i = 0; i < maxParams; i++) {
                body.addStatement("this.components.set(%d, component%d)".formatted(i, i + 1));
            }

            body.beginControlFlow("for (int i = 0, s = components.length; i < s; i++)");
            body.addStatement("this.components.set(%d + i, components[i])".formatted(maxParams));
            body.endControlFlow();

            body.addStatement("this.added = %d + components.length".formatted(maxParams));
            body.addStatement("this.valid = true");

            return MethodSpec.methodBuilder("initialize")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameters(parameters)
                    .addParameter(Object[].class, "components").varargs()
                    .addCode(body.build())
                    .build();
        }

    }

}
