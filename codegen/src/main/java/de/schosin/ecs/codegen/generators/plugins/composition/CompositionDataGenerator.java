package de.schosin.ecs.codegen.generators.plugins.composition;

import java.util.ArrayList;
import java.util.stream.IntStream;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;

import de.schosin.ecs.codegen.Utils;
import de.schosin.ecs.codegen.generators.plugins.datatypes.BaseDataTypeGenerator;

public class CompositionDataGenerator {

    public static final ClassName COMPOSITION_DATA = ClassName.get("de.schosin.ecs.plugins.composition", "CompositionData");

    public static Iterable<JavaFile> generateFiles(TypeElement type, int maxParams) {
        var packageName = ClassName.get(type).packageName();

        var files = new ArrayList<JavaFile>(maxParams + 1);
        files.add(CompositionData1.create(packageName));
        files.addAll(IntStream.range(2, maxParams + 1).mapToObj(n -> CompositionDataN.create(packageName, n)).toList());

        return files;
    }

    private static class CompositionData1 {

        public static JavaFile create(String packageName) {
            return JavaFile.builder(packageName, createCompositionData1())
                    .addStaticImport(Utils.COMPONENT_TYPE, "component")
                    .skipJavaLangImports(true)
                    .indent(Utils.INDENT)
                    .build();
        }

        private static TypeSpec createCompositionData1() {
            var processor = BaseDataTypeGenerator.dataProcessor(Utils.R);
            var superinterface = ParameterizedTypeName.get(COMPOSITION_DATA, processor);

            return TypeSpec.interfaceBuilder("CompositionData1")
                    .addModifiers(Modifier.PUBLIC)
                    .addTypeVariable(Utils.R)
                    .addSuperinterface(superinterface)
                    .build();
        }

    }

    private static class CompositionDataN {

        public static JavaFile create(String packageName, int n) {
            return JavaFile.builder(packageName, createCompositionDataN(n))
                    .addStaticImport(Utils.COMPONENT_TYPE, "component")
                    .skipJavaLangImports(true)
                    .indent(Utils.INDENT)
                    .build();
        }

        private static TypeSpec createCompositionDataN(int n) {
            var typeVariables = Utils.generateTypeVariables("R", n);

            var processor = BaseDataTypeGenerator.dataProcessorN(n, typeVariables);
            var superinterface = ParameterizedTypeName.get(COMPOSITION_DATA, processor);

            return TypeSpec.interfaceBuilder("CompositionData" + n)
                    .addModifiers(Modifier.PUBLIC)
                    .addTypeVariables(typeVariables)
                    .addSuperinterface(superinterface)
                    .build();
        }

    }

}
