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
    private static final ClassName ABSTRACT_COMPOSITION_N = COMPOSITION_MANAGER.nestedClass("AbstractCompositionN");

    private static final ClassName COMPOSITION_MANAGER_HELPER = ClassName.get("", "CompositionManagerHelper");

    private static final ClassName ACCESSOR = ClassName.get("de.schosin.ecs.api.data", "IterableAccessor");
    private static final ClassName CONVERTER = ClassName.get("de.schosin.ecs.engine.components.mappers", "ComponentConverter");
    
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
                    .addTypes(compositionsN)
                    .build();
        }

        private static MethodSpec createCompositionData(int maxParams) {
            var dataT = TypeVariableName.get("T", CompositionPluginGenerator.DATA);
            var processor = BaseDataTypeGenerator.dataProcessor(Utils.T);
            var processorT = TypeVariableName.get("P", processor);

            var compositionData = ParameterizedTypeName.get(CompositionPluginGenerator.COMPOSITION_DATA, TypeVariableName.get("P"));
            var compositionDataD = TypeVariableName.get("D", compositionData);

            var dataType = ParameterizedTypeName.get(DATA_TYPE, Utils.WILDCARD, Utils.WILDCARD, dataT, processorT);

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
                    .addTypeVariable(dataT)
                    .addTypeVariable(processorT)
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
            var superclass = ParameterizedTypeName.get(ABSTRACT_COMPOSITION_N, dataN, processor);

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
                    .addMethod(processAccessor(n, processor))
                    .build();
        }

        private static MethodSpec processAccessor(int n, ParameterizedTypeName processor) {
            var methodBody = CodeBlock.builder();

            for (int i = 1; i <= n; i++) {
                var typeR = TypeVariableName.get("R" + i);
                var converter = ParameterizedTypeName.get(CONVERTER, typeR);

                methodBody.addStatement("var converter%d = ($1T) converters.get(%d)".formatted(i, i - 1), converter);
            }

            methodBody.beginControlFlow("while(accessor.hasNext())");
            methodBody.addStatement("var entityId = accessor.next()");

            methodBody.addStatement("// retrieve components");
            for (int i = 1; i <= n; i++) {
                methodBody.addStatement("var component%d = converter%d.getComponent(accessor)".formatted(i, i));
            }

            methodBody.addStatement("// process");
            var processStatement = "processor.process(entityId";
            for (int i = 1; i <= n; i++) {
                processStatement += ", component%d".formatted(i);
            }
            processStatement += ")";

            methodBody.addStatement(processStatement);

            methodBody.addStatement("// free components");
            for (int i = 1; i <= n; i++) {
                methodBody.addStatement("converter%d.free(component%d)".formatted(i, i));
            }

            methodBody.endControlFlow(); // while

            var converters = Utils.immutableBag(ParameterizedTypeName.get(CONVERTER, Utils.OBJECT));

            return MethodSpec.methodBuilder("process")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addParameter(processor, "processor")
                    .addParameter(ACCESSOR, "accessor")
                    .addParameter(converters, "converters")
                    .addCode(methodBody.build())
                    .build();
        }

    }

    static ClassName compositionDataN(int n) {
        return ClassName.get("de.schosin.ecs.plugins.composition", "CompositionData" + n);
    }

}
