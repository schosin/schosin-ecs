package de.schosin.ecs.codegen.generators.plugins.composition;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;

import de.schosin.ecs.codegen.Utils;
import de.schosin.ecs.codegen.generators.plugins.datatypes.BaseDataTypeGenerator;

public class CompositionGenerator {

    private static final ClassName BASE_COMPOSITION = ClassName.get("", "BaseComposition");

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
            return TypeSpec.interfaceBuilder(BASE_COMPOSITION)
                    .addModifiers(Modifier.PUBLIC)
                    .addMethods(createRetrieveMethods(maxParams))
                    .build();
        }

        private static List<MethodSpec> createRetrieveMethods(int maxParams) {
            var methods = new ArrayList<MethodSpec>(maxParams);
            methods.add(createComponentRetrieveMethod());
            methods.addAll(IntStream.range(2, maxParams + 1).mapToObj(n -> createDataTypeRetrieveMethod(n)).toList());

            return methods;
        }

        private static MethodSpec createComponentRetrieveMethod() {
            var compositionData = ParameterizedTypeName.get(ClassName.get("", "CompositionData1"), Utils.R);

            return MethodSpec.methodBuilder("retrieve")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addTypeVariable(Utils.R)
                    .addParameter(Utils.componentType(Utils.WILDCARD, Utils.R), "type")
                    .returns(compositionData)
                    .build();
        }

        private static MethodSpec createDataTypeRetrieveMethod(int n) {
            var typeVariables = Utils.generateTypeVariables("R", n);
            var compositionData = ParameterizedTypeName.get(ClassName.get("", "CompositionData" + n), typeVariables.toArray(TypeVariableName[]::new));

            var dataTypeNVariables = typeVariables.stream()
                    .flatMap(type -> Stream.of(Utils.WILDCARD, type))
                    .toList();

            var dataTypeN = BaseDataTypeGenerator.dataTypeN(typeVariables.size(), dataTypeNVariables);

            return MethodSpec.methodBuilder("retrieve")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addTypeVariables(typeVariables)
                    .addParameter(dataTypeN, "dataType")
                    .returns(compositionData)
                    .build();
        }

    }

}
