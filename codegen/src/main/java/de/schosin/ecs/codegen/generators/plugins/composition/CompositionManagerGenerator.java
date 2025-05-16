package de.schosin.ecs.codegen.generators.plugins.composition;

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
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;

import de.schosin.ecs.codegen.Utils;

public class CompositionManagerGenerator {

    private static final ClassName COMPOSITION = ClassName.get("de.schosin.ecs.plugins.composition", "Composition");

    private static final ClassName COMPOSITION_MANAGER = ClassName.get("de.schosin.ecs.plugins.composition.manager", "CompositionManager");
    private static final ClassName ABSTRACT_COMPOSITION_N = COMPOSITION_MANAGER.nestedClass("AbstractCompositionN");

    private static final ClassName BASE_COMPOSITION_IMPL = ClassName.get("", "BaseCompositionImpl");

    private static final ClassName COMPONENTS = ClassName.get("de.schosin.ecs.api.components", "Components");

    private static final String OF_PREFIX = CompositionGenerator.OF_PREFIX;
    private static final ParameterizedTypeName OF_WILDCARD = ParameterizedTypeName.get(ClassName.get("", OF_PREFIX), Utils.WILDCARD);

    public static JavaFile generateFile(TypeElement type, int maxParams) {
        System.out.println("Process CompositionManager with %d parameters: %s".formatted(maxParams, type));

        var baseCompositionImpl = BaseCompositionImpl.create(maxParams);

        var className = ClassName.get(type);
        return JavaFile.builder(className.packageName(), baseCompositionImpl)
                .skipJavaLangImports(true)
                .indent(Utils.INDENT)
                .build();
    }

    private static class BaseCompositionImpl {

        private static final String METHOD_RETRIEVE = "retrieve";
        private static final String METHOD_GET_COMPONENT = "getComponents";

        private static TypeSpec create(int maxParams) {
            var compositionsN = IntStream.range(1, maxParams + 1)
                    .mapToObj(idx -> CompositionN.create(idx, maxParams))
                    .toList();

            var tExtendOf = TypeVariableName.get("T", OF_WILDCARD);
            var supplier = Utils.supplier(Utils.T);

            var abstractRetrieve = MethodSpec.methodBuilder(METHOD_RETRIEVE)
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .addTypeVariable(tExtendOf)
                    .returns(Utils.T)
                    .addParameter(supplier, "constructor")
                    .addParameter(Utils.COMPONENT_TYPE_WILDCARD_ARRAY, "components").varargs()
                    .build();

            var componentsWildcard = ParameterizedTypeName.get(COMPONENTS, Utils.WILDCARD);

            var abstractGetComponent = MethodSpec.methodBuilder(METHOD_GET_COMPONENT)
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .returns(componentsWildcard)
                    .addParameter(Utils.COMPONENT_TYPE_WILDCARD, "type")
                    .build();

            var retrieveMethods = createRetrieveMethods("this", "retrieve", 1, maxParams);

            return TypeSpec.classBuilder(BASE_COMPOSITION_IMPL)
                    .addModifiers(Modifier.ABSTRACT)
                    .addSuperinterface(COMPOSITION)
                    .addMethod(abstractRetrieve)
                    .addMethod(abstractGetComponent)
                    .addMethods(retrieveMethods)
                    .addTypes(compositionsN)
                    .build();
        }

        private static List<MethodSpec> createRetrieveMethods(String target, String name, int start, int maxParams) {
            if (maxParams - start < 0) {
                return List.of();
            }

            var methods = new ArrayList<MethodSpec>(2 * (maxParams - start));
            methods.addAll(IntStream.range(start, maxParams + 1).mapToObj(idx -> createClassRetrieveMethod(target, name, start, idx)).toList());
            methods.addAll(IntStream.range(start, maxParams + 1).mapToObj(idx -> createComponentRetrieveMethod(target, name, start, idx)).toList());

            return methods;
        }

        private static MethodSpec createComponentRetrieveMethod(String target, String name, int start, int n) {
            var typeVariables = Utils.generateTypeVariables("T", start, n);

            var returnTypeVariablesArray = Utils.generateTypeVariables("T", 1, n).toArray(TypeVariableName[]::new);

            var returnName = start == 1 ? OF_PREFIX + (start + n - 1) : OF_PREFIX + (n);
            var returnType = ClassName.get("", returnName);
            var returnTypeParameterized = ParameterizedTypeName.get(returnType, returnTypeVariablesArray);

            var parameters = IntStream.range(0, typeVariables.size())
                    .mapToObj(idx -> ParameterSpec.builder(Utils.componentType(typeVariables.get(idx)), "component" + (start + idx)).build())
                    .toList();

            var body = CodeBlock.builder();

            // return %s.retrieve(() -> new $1T<>(%s, %s), %s)
            var implementation = ClassName.get("", CompositionN.NAME_PREFIX + n);
            body.add("return %s.retrieve(() -> new $1T<>(%s".formatted(target, target), implementation);

            for (int i = 1; i < n + 1; i++) {
                body.add(", component%d".formatted(i));
            }

            body.add(")");
            for (int i = 1; i < n + 1; i++) {
                body.add(", component%d".formatted(i));
            }

            body.addStatement(")");

            return MethodSpec.methodBuilder(name)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addTypeVariables(typeVariables)
                    .addParameters(parameters)
                    .returns(returnTypeParameterized)
                    .addCode(body.build())
                    .build();
        }

        private static MethodSpec createClassRetrieveMethod(String target, String name, int start, int n) {
            var typeVariables = Utils.generateTypeVariables("T", start, n);

            var returnTypeVariablesArray = Utils.generateTypeVariables("T", 1, n).toArray(TypeVariableName[]::new);

            var returnName = start == 1 ? OF_PREFIX + (start + n - 1) : OF_PREFIX + (n);
            var returnType = ClassName.get("", returnName);
            var returnTypeParameterized = ParameterizedTypeName.get(returnType, returnTypeVariablesArray);

            var parameters = IntStream.range(0, typeVariables.size())
                    .mapToObj(idx -> ParameterSpec.builder(Utils.clazz(typeVariables.get(idx)), "component" + (start + idx)).build())
                    .toList();

            var body = CodeBlock.builder();
            for (int i = start; i < n + 1; i++) {
                body.addStatement("var type%d = $1T.component(component%d)".formatted(i, i), Utils.COMPONENT_TYPE);
            }

            // return %s.retrieve(() -> new $1T<>(%s, %s), %s)
            var implementation = ClassName.get("", CompositionN.NAME_PREFIX + n);
            body.add("return %s.retrieve(() -> new $1T<>(%s".formatted(target, target), implementation);

            for (int i = 1; i < start; i++) {
                body.add(", component%d".formatted(i));
            }
            for (int i = start; i < n + 1; i++) {
                body.add(", type%d".formatted(i));
            }

            body.add(")");
            for (int i = 1; i < start; i++) {
                body.add(", component%d".formatted(i));
            }
            for (int i = start; i < n + 1; i++) {
                body.add(", type%d".formatted(i));
            }

            body.addStatement(")");

            return MethodSpec.methodBuilder(name)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addTypeVariables(typeVariables)
                    .addParameters(parameters)
                    .returns(returnTypeParameterized)
                    .addCode(body.build())
                    .build();
        }

    }

    private static class CompositionN {

        private static final String NAME_PREFIX = "Composition";

        private static TypeSpec create(int n, int maxParams) {
            var name = OF_PREFIX + n;

            var typeVariables = Utils.generateTypeVariables("T", n);
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

            var parameterizedOf = ParameterizedTypeName.get(ClassName.get("", name), typeVariablesArray);

            var fields = n < maxParams
                    ? IntStream.range(1, typeVariables.size() + 1)
                            .mapToObj(idx -> FieldSpec.builder(Utils.componentType(typeVariables.get(idx - 1)), "component" + idx, Modifier.PRIVATE, Modifier.FINAL).build())
                            .toList()
                    : List.<FieldSpec>of();

            var componentConstructor = buildComponentConstructor(n, maxParams, typeVariables);

            var ofMethods = List.of(processEntity(n), process(n), inserted(n), removed(n));

            var retrieveMethods = BaseCompositionImpl.createRetrieveMethods("composition", "and", n + 1, maxParams);

            return TypeSpec.classBuilder(NAME_PREFIX + n)
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .addTypeVariables(typeVariables)
                    .superclass(ABSTRACT_COMPOSITION_N)
                    .addSuperinterface(parameterizedOf)
                    .addFields(fields)
                    .addMethod(componentConstructor)
                    .addMethods(ofMethods)
                    .addMethods(retrieveMethods)
                    .build();
        }

        private static MethodSpec buildComponentConstructor(int n, int maxParams, List<TypeVariableName> typeVariables) {
            var parameters = IntStream.range(1, typeVariables.size() + 1)
                    .mapToObj(idx -> ParameterSpec.builder(Utils.componentType(typeVariables.get(idx - 1)), "component" + idx).build())
                    .toList();

            var parameterNames = parameters.stream().map(ParameterSpec::name).collect(Collectors.joining(", "));

            var constructorBody = CodeBlock.builder();
            constructorBody.addStatement("super(composition, %s)".formatted(parameterNames));

            if (n < maxParams) {
                for (int i = 1; i <= typeVariables.size(); i++) {
                    constructorBody.addStatement("this.component%d = component%d".formatted(i, i));
                }
            }

            return MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PROTECTED)
                    .addParameter(BASE_COMPOSITION_IMPL, "composition")
                    .addParameters(parameters)
                    .addCode(constructorBody.build())
                    .build();
        }

        private static ParameterizedTypeName buildCallback(List<TypeVariableName> typeVariables) {
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

            var consumer = ClassName.get("", OF_PREFIX + typeVariablesArray.length, "Consumer");
            return ParameterizedTypeName.get(consumer, typeVariablesArray);
        }

        private static MethodSpec processEntity(int n) {
            var typeVariables = Utils.generateTypeVariables("T", n);
            var callback = buildCallback(typeVariables);

            // "callback.consume(entityId, get(entityId, 0), get(entityId, 1), get(entityId, 2), get(entityId, 3), get(entityId, 4), get(entityId, 5), get(entityId, 6), get(entityId, 7));"
            var methodBody = CodeBlock.builder();
            for (int i = 0, s = typeVariables.size(); i < s; i++) {
                methodBody.addStatement("T%d component%d = get(entityId, %d)".formatted(i + 1, i + 1, i));
            }

            methodBody.add("callback.consume(entityId");
            for (int i = 0; i < typeVariables.size(); i++) {
                methodBody.add(", component%d".formatted(i + 1));
            }
            methodBody.addStatement(")");

            for (int i = 0, s = typeVariables.size(); i < s; i++) {
                methodBody.addStatement("free(%d, component%d)".formatted(i, i + 1));
            }

            return MethodSpec.methodBuilder("process")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(TypeName.INT, "entityId")
                    .addParameter(callback, "callback")
                    .addCode(methodBody.build())
                    .build();
        }

        private static MethodSpec process(int n) {
            var typeVariables = Utils.generateTypeVariables("T", n);
            var callback = buildCallback(typeVariables);

            return MethodSpec.methodBuilder("process")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(callback, "callback")
                    .addStatement("super.process(entityId -> process(entityId, callback))")
                    .build();
        }

        private static MethodSpec inserted(int n) {
            var typeVariables = Utils.generateTypeVariables("T", n);
            var callback = buildCallback(typeVariables);

            return MethodSpec.methodBuilder("inserted")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(callback, "callback")
                    .addStatement("super.inserted(entityId -> process(entityId, callback))")
                    .build();
        }

        private static MethodSpec removed(int n) {
            var typeVariables = Utils.generateTypeVariables("T", n);
            var callback = buildCallback(typeVariables);

            return MethodSpec.methodBuilder("removed")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(callback, "callback")
                    .addStatement("super.removed(entityId -> process(entityId, callback))")
                    .build();
        }

    }

}
