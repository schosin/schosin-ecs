package de.schosin.ecs.codegen.generators.plugins.datatypes;

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

public class DataTypeMapperGenerator {

    static final ClassName DATA_TYPE = ClassName.get("de.schosin.ecs.plugins.data.types", "DataType");
    static final ClassName DATA = ClassName.get("de.schosin.ecs.plugins.data.types", "Data");

    public static JavaFile generateFile(TypeElement type, int maxParams) {
        var packageName = ClassName.get(type).packageName();

        var helper = TypeSpec.classBuilder("DataTypeMapperHelper")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(getInstance(maxParams))
                .build();

        return JavaFile.builder(packageName, helper)
                .skipJavaLangImports(true)
                .indent(Utils.INDENT)
                .build();
    }

    private static MethodSpec getInstance(int maxParams) {
        var dataR = TypeVariableName.get("R", DATA);
        var dataType = ParameterizedTypeName.get(DATA_TYPE, Utils.WILDCARD, Utils.WILDCARD, dataR, Utils.WILDCARD);

        var body = CodeBlock.builder()
                .beginControlFlow("return (R) switch(dataType)");

        for (int i = 2; i <= maxParams; i++) {
            var typeVariables = IntStream.range(1, i + 1)
                    .mapToObj(Integer::valueOf)
                    .flatMap(n -> Stream.of(Utils.WILDCARD, Utils.WILDCARD))
                    .toArray(WildcardTypeName[]::new);

            var dataTypeN = ClassName.get("de.schosin.ecs.plugins.data.types", "DataType" + i);
            var parameterizedDataTypeN = ParameterizedTypeName.get(dataTypeN, typeVariables);
            var dataN = ClassName.get("de.schosin.ecs.plugins.data.types", "Data" + i);

            var arguments = "";
            for (int j = 0; j < i; j++) {
                if (j > 0) {
                    arguments += ", ";
                }

                arguments += "components[%d]".formatted(j);
            }

            body.add("case $1T type%1$d ->".formatted(i), parameterizedDataTypeN);
            body.addStatement("$1T.getInstance(%s)".formatted(arguments), dataN);
        }

        body.endControlFlow();
        body.addStatement(""); // switch expression

        return MethodSpec.methodBuilder("getInstance")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addTypeVariable(dataR)
                .addParameter(dataType, "dataType")
                .addParameter(Object[].class, "components").varargs()
                .returns(Utils.R)
                .addCode(body.build())
                .build();
    }

}
