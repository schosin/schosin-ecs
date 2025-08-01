package de.schosin.ecs.codegen.generators.plugins.composition;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;
import com.palantir.javapoet.WildcardTypeName;

import de.schosin.ecs.codegen.Utils;
import de.schosin.ecs.codegen.generators.plugins.datatypes.BaseDataTypeGenerator;

public class CompositionManagerGenerator {

    private static final ClassName COMPOSITION = ClassName.get("de.schosin.ecs.plugins.composition", "Composition");

    private static final ClassName COMPOSITION_MANAGER = ClassName.get("de.schosin.ecs.plugins.composition.manager", "CompositionManager");
    private static final ClassName COMPOSITION_DATA_IMPL = COMPOSITION_MANAGER.nestedClass("CompositionDataImpl");

    private static final ClassName COMPOSITION_MANAGER_HELPER = ClassName.get("", "CompositionManagerHelper");
    private static final ClassName ABSTRACT_COMPOSITION_DATA_N = ClassName.get("", "AbstractCompositionDataN");

    public static final ClassName DATA_TYPE = ClassName.get("de.schosin.ecs.plugins.data.types", "DataType");

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

        private static TypeSpec create(int maxParams) {
            var compositionsN = IntStream.range(2, maxParams + 1)
                    .mapToObj(n -> CompositionN.create(n))
                    .toList();

            return TypeSpec.classBuilder(COMPOSITION_MANAGER_HELPER)
                    .addAnnotation(Utils.SUPPRESS_UNCHECKED)
                    .addMethod(createCompositionData(maxParams))
                    .addType(abstractCompositionN())
                    .addTypes(compositionsN)
                    .build();
        }

        private static TypeSpec abstractCompositionN() {
            var dataR = TypeVariableName.get("R", CompositionPluginGenerator.DATA);
            var processorP = TypeVariableName.get("P", BaseDataTypeGenerator.dataProcessor(Utils.R));
            var compositionDataImpl = ParameterizedTypeName.get(COMPOSITION_DATA_IMPL, dataR, processorP);

            var dataType = ParameterizedTypeName.get(DATA_TYPE, Utils.WILDCARD, dataR, processorP);

            var constructor = MethodSpec.constructorBuilder()
                    .addParameter(COMPOSITION, "composition")
                    .addParameter(dataType, "dataType")
                    .addStatement("super(composition, dataType)")
                    .build();

            return TypeSpec.classBuilder(ABSTRACT_COMPOSITION_DATA_N)
                    .addModifiers(Modifier.STATIC, Modifier.ABSTRACT, Modifier.SEALED)
                    .addTypeVariable(dataR)
                    .addTypeVariable(processorP)
                    .superclass(compositionDataImpl)
                    .addMethod(constructor)
                    .build();
        }

        private static MethodSpec createCompositionData(int maxParams) {
            var dataR = TypeVariableName.get("R", CompositionPluginGenerator.DATA);
            var processor = BaseDataTypeGenerator.dataProcessor(Utils.R);
            var processorP = TypeVariableName.get("P", processor);

            var compositionDataImpl = ParameterizedTypeName.get(COMPOSITION_DATA_IMPL, dataR, processorP);
            var compositionData = ParameterizedTypeName.get(CompositionPluginGenerator.COMPOSITION_DATA, TypeVariableName.get("P"));
            var compositionDataD = TypeVariableName.get("D", compositionDataImpl, compositionData);

            var dataType = ParameterizedTypeName.get(DATA_TYPE, Utils.WILDCARD, dataR, processorP);

            var body = CodeBlock.builder();
            body.beginControlFlow("return (D) switch(dataType)");

            for (int i = 2; i <= maxParams; i++) {
                var typeVariables = IntStream.range(1, i + 1)
                        .mapToObj(Integer::valueOf)
                        .flatMap(n -> Stream.of(Utils.WILDCARD, Utils.WILDCARD))
                        .toArray(WildcardTypeName[]::new);

                var dataTypeN = ClassName.get("de.schosin.ecs.plugins.data.types", "DataType" + i);
                var parameterizedDataTypeN = ParameterizedTypeName.get(dataTypeN, typeVariables);

                body.add("case $1T type%1$d ->".formatted(i), parameterizedDataTypeN);
                body.addStatement("new $1T<>(composition, type%d)".formatted(i), ClassName.get("", "CompositionData%dImpl".formatted(i)));
            }

            body.endControlFlow();
            body.addStatement(""); // switch expression

            return MethodSpec.methodBuilder("createCompositionData")
                    .addModifiers(Modifier.STATIC)
                    .addTypeVariable(dataR)
                    .addTypeVariable(processorP)
                    .addTypeVariable(compositionDataD)
                    .addParameter(COMPOSITION, "composition")
                    .addParameter(dataType, "dataType")
                    .returns(TypeVariableName.get("D"))
                    .addCode(body.build())
                    .build();
        }

    }

    private static class CompositionN {

        private static TypeSpec create(int n) {
            var name = "CompositionData" + n + "Impl";

            var typeVariables = Utils.generateTypeVariables("R", n);
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

            var dataN = BaseDataTypeGenerator.dataN(typeVariables.size(), typeVariables);
            var processor = BaseDataTypeGenerator.dataProcessorN(typeVariables.size(), typeVariables);
            var superclass = ParameterizedTypeName.get(ABSTRACT_COMPOSITION_DATA_N, dataN, processor);

            var compositionDataN = ParameterizedTypeName.get(compositionDataN(n), typeVariablesArray);

            var dataTypeNVariables = typeVariables.stream()
                    .flatMap(type -> Stream.of(Utils.WILDCARD, type))
                    .toList();

            var dataTypeN = BaseDataTypeGenerator.dataTypeN(typeVariables.size(), dataTypeNVariables);

            var constructor = MethodSpec.constructorBuilder()
                    .addParameter(COMPOSITION, "composition")
                    .addParameter(dataTypeN, "dataType")
                    .addStatement("super(composition, dataType)")
                    .build();

            return TypeSpec.classBuilder(name)
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .addTypeVariables(typeVariables)
                    .superclass(superclass)
                    .addSuperinterface(compositionDataN)
                    .addMethod(constructor)
                    .build();
        }

    }

    static ClassName compositionDataN(int n) {
        return ClassName.get("de.schosin.ecs.plugins.composition", "CompositionData" + n);
    }

}
