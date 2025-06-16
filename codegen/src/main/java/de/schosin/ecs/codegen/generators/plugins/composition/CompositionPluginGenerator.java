package de.schosin.ecs.codegen.generators.plugins.composition;

import java.util.stream.IntStream;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;

import de.schosin.ecs.codegen.Utils;
import de.schosin.ecs.codegen.generators.plugins.datatypes.BaseDataTypeGenerator;

public class CompositionPluginGenerator {

    public static final ClassName COMPOSITION = ClassName.get("", "Composition");
    public static final ClassName COMPOSITION_DATA = ClassName.get("de.schosin.ecs.plugins.composition", "CompositionData");

    public static final ClassName DATA = ClassName.get("de.schosin.ecs.plugins.data.types", "Data");

    public static final ClassName BUILDER = COMPOSITION.nestedClass("Builder");

    public static JavaFile generateFile(TypeElement type, int maxParams) {
        var className = ClassName.get(type);
        return JavaFile.builder(className.packageName(), compositionCreator(maxParams))
                .addStaticImport(Utils.COMPONENT_TYPE, "component")
                .skipJavaLangImports(true)
                .indent(Utils.INDENT)
                .build();
    }

    static TypeSpec compositionCreator(int maxParams) {
        var interfaceBuilder = TypeSpec.interfaceBuilder("CompositionCreator")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        interfaceBuilder.addMethod(createComposition());
        interfaceBuilder.addMethod(createDataTypeComposition());

        interfaceBuilder.addMethod(createClassComposition());
        interfaceBuilder.addMethod(createComponentTypeComposition());

        for (int i = 2; i <= maxParams; i++) {
            interfaceBuilder.addMethod(createClassComposition(i));
            interfaceBuilder.addMethod(createComponentTypeComposition(i));
        }

        return interfaceBuilder.build();
    }

    private static MethodSpec createComposition() {
        return MethodSpec.methodBuilder("createComposition")
                .addJavadoc(Javadoc.CREATE_COMPOSITION)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(BUILDER, "builder")
                .returns(COMPOSITION)
                .build();
    }

    private static MethodSpec createDataTypeComposition() {
        var dataT = TypeVariableName.get("T", DATA);
        var processor = BaseDataTypeGenerator.dataProcessor(Utils.T);
        var processorT = TypeVariableName.get("P", processor);

        var compositionData = ParameterizedTypeName.get(COMPOSITION_DATA, TypeVariableName.get("P"));
        var compositionDataD = TypeVariableName.get("D", compositionData);

        var dataType = ParameterizedTypeName.get(CompositionManagerGenerator.DATA_TYPE, Utils.WILDCARD, Utils.WILDCARD, dataT, processorT);

        return MethodSpec.methodBuilder("createComposition")
                .addJavadoc(Javadoc.CREATE_COMPOSITION)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addTypeVariable(dataT)
                .addTypeVariable(processorT)
                .addTypeVariable(compositionDataD)
                .addParameter(BUILDER, "builder")
                .addParameter(dataType, "dataType")
                .returns(compositionDataD)
                .build();
    }

    private static MethodSpec createClassComposition() {
        var compositionData = ParameterizedTypeName.get(ClassName.get("", "CompositionData1"), Utils.R);

        return MethodSpec.methodBuilder("createComposition")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addTypeVariable(Utils.R)
                .addParameter(BUILDER, "builder")
                .addParameter(Utils.clazz(Utils.R), "component")
                .returns(compositionData)
                .addStatement("return createComposition(builder, component(component))")
                .build();
    }

    private static MethodSpec createComponentTypeComposition() {
        var compositionData = ParameterizedTypeName.get(ClassName.get("", "CompositionData1"), Utils.R);

        return MethodSpec.methodBuilder("createComposition")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addTypeVariable(Utils.R)
                .addParameter(BUILDER, "builder")
                .addParameter(Utils.componentType(Utils.WILDCARD, Utils.R), "component")
                .returns(compositionData)
                .build();
    }

    private static MethodSpec createClassComposition(int n) {
        var typeVariables = Utils.generateTypeVariables("R", n);

        var compositionData = ParameterizedTypeName.get(ClassName.get("", "CompositionData" + n), typeVariables.toArray(TypeVariableName[]::new));

        var parameters = IntStream.range(1, n + 1)
                .mapToObj(i -> ParameterSpec.builder(Utils.clazz(typeVariables.get(i - 1)), "type" + i).build())
                .toList();

        var body = CodeBlock.builder();
        body.add("return createComposition(builder, ");
        body.add("$1T.get(", CompositionManagerGenerator.DATA_TYPE);

        for (int i = 1; i <= n; i++) {
            if (i > 1) {
                body.add(", ");
            }

            body.add("component(type%d)".formatted(i));
        }

        body.addStatement("))");

        return MethodSpec.methodBuilder("createComposition")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addTypeVariables(typeVariables)
                .addParameter(BUILDER, "builder")
                .addParameters(parameters)
                .returns(compositionData)
                .addCode(body.build())
                .build();
    }

    private static MethodSpec createComponentTypeComposition(int n) {
        var typeVariables = Utils.generateTypeVariables("R", n);
        var compositionData = ParameterizedTypeName.get(ClassName.get("", "CompositionData" + n), typeVariables.toArray(TypeVariableName[]::new));

        var parameters = IntStream.range(1, n + 1)
                .mapToObj(i -> ParameterSpec.builder(Utils.componentType(Utils.WILDCARD, typeVariables.get(i - 1)), "type" + i).build())
                .toList();

        var body = CodeBlock.builder();
        body.add("return createComposition(builder, ");
        body.add("$1T.get(", CompositionManagerGenerator.DATA_TYPE);

        for (int i = 1; i <= n; i++) {
            if (i > 1) {
                body.add(", ");
            }

            body.add("type%d".formatted(i));
        }

        body.addStatement("))");

        return MethodSpec.methodBuilder("createComposition")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addTypeVariables(typeVariables)
                .addParameter(BUILDER, "builder")
                .addParameters(parameters)
                .returns(compositionData)
                .addCode(body.build())
                .build();
    }

    private static class Javadoc {

        private static final String CREATE_COMPOSITION = """
                Create a composition that can be used to process entities and to register lifecycle callbacks for entities
                that match the composition defined by the builder.

                <p>
                See the other overloads of this methods if access to components is required.
                </p>
                """;

    }

}
