package de.schosin.ecs.codegen.generators.plugins.composition;

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
import com.palantir.javapoet.WildcardTypeName;

import de.schosin.ecs.codegen.Utils;

public class CompositionManagerGenerator {

    private static final ClassName COMPOSITION = ClassName.get("de.schosin.ecs.plugins.composition", "Composition");

    private static final ClassName COMPOSITION_MANAGER = ClassName.get("de.schosin.ecs.plugins.composition.manager", "CompositionManager");
    private static final ClassName ABSTRACT_COMPOSITION_N = COMPOSITION_MANAGER.nestedClass("AbstractCompositionN");

    private static final ClassName BASE_COMPOSITION_IMPL = ClassName.get("", "BaseCompositionImpl");

    // TODO should be Component only on the other branch
    private static final ClassName COMPONENT = ClassName.get("de.schosin.ecs.engine.components", "Component");

    private static final String OF_PREFIX = CompositionGenerator.OF_PREFIX;
    private static final ParameterizedTypeName OF_WILDCARD = ParameterizedTypeName.get(ClassName.get("", OF_PREFIX), WildcardTypeName.subtypeOf(Object.class));

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
        private static final String METHOD_GET_COMPONENT = "getComponent";

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
                    .addParameter(Utils.WILDCARD_CLASS_ARRAY, "components").varargs()
                    .build();

            var componentT = ParameterizedTypeName.get(COMPONENT, Utils.T);
            var abstractGetComponent = MethodSpec.methodBuilder(METHOD_GET_COMPONENT)
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .addTypeVariable(Utils.T)
                    .returns(componentT)
                    .addParameter(Utils.clazz(Utils.T), "clazz")
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

            return IntStream.range(start, maxParams + 1).mapToObj(idx -> createRetrieveMethod(target, name, start, idx)).toList();
        }

        private static MethodSpec createRetrieveMethod(String target, String name, int start, int n) {
            var typeVariables = Utils.generateTypeVariables("T", start, n);

            var returnTypeVariablesArray = Utils.generateTypeVariables("T", 1, n).toArray(TypeVariableName[]::new);

            var returnName = start == 1 ? OF_PREFIX + (start + n - 1) : OF_PREFIX + (n);
            var returnType = ClassName.get("", returnName);
            var returnTypeParameterized = ParameterizedTypeName.get(returnType, returnTypeVariablesArray);

            var parameters = IntStream.range(0, typeVariables.size())
                    .mapToObj(idx -> ParameterSpec.builder(Utils.clazz(typeVariables.get(idx)), "component" + (start + idx)).build())
                    .toList();

            var parameterNames = IntStream.range(1, n + 1).mapToObj(idx -> "component" + idx).collect(Collectors.joining(", "));

            var implementation = ClassName.get("", CompositionN.NAME_PREFIX + n);

            return MethodSpec.methodBuilder(name)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addTypeVariables(typeVariables)
                    .addParameters(parameters)
                    .returns(returnTypeParameterized)
                    .addStatement("return %s.retrieve(() -> new $1T<>(%s, %s), %s)".formatted(target, target, parameterNames, parameterNames), implementation)
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
                            .mapToObj(idx -> FieldSpec.builder(Utils.clazz(typeVariables.get(idx - 1)), "component" + idx, Modifier.PRIVATE, Modifier.FINAL).build())
                            .toList()
                    : List.<FieldSpec>of();

            var parameters = IntStream.range(1, typeVariables.size() + 1)
                    .mapToObj(idx -> ParameterSpec.builder(Utils.clazz(typeVariables.get(idx - 1)), "component" + idx).build())
                    .toList();

            var parameterNames = parameters.stream().map(ParameterSpec::name).collect(Collectors.joining(", "));

            var constructorBody = CodeBlock.builder();
            constructorBody.addStatement("super(composition, %s)".formatted(parameterNames));

            if (n < maxParams) {
                for (int i = 1; i <= typeVariables.size(); i++) {
                    constructorBody.addStatement("this.component%d = component%d".formatted(i, i));
                }
            }

            var constructor = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PROTECTED)
                    .addParameter(BASE_COMPOSITION_IMPL, "composition")
                    .addParameters(parameters)
                    .addCode(constructorBody.build())
                    .build();

            var ofMethods = List.of(processEntity(n), process(n), inserted(n), removed(n));

            var retrieveMethods = BaseCompositionImpl.createRetrieveMethods("composition", "and", n + 1, maxParams);

            return TypeSpec.classBuilder(NAME_PREFIX + n)
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .addTypeVariables(typeVariables)
                    .superclass(ABSTRACT_COMPOSITION_N)
                    .addSuperinterface(parameterizedOf)
                    .addFields(fields)
                    .addMethod(constructor)
                    .addMethods(ofMethods)
                    .addMethods(retrieveMethods)
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
            methodBody.add("callback.consume(entityId");
            for (int i = 0; i < typeVariables.size(); i++) {
                methodBody.add(", get(entityId, %s)".formatted(i));
            }
            methodBody.add(")");

            return MethodSpec.methodBuilder("process")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(TypeName.INT, "entityId")
                    .addParameter(callback, "callback")
                    .addStatement(methodBody.build())
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
