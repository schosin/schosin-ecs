package de.schosin.ecs.codegen.generators.plugins.transmuter;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;

import de.schosin.ecs.codegen.Utils;

public class TransmutationManagerTestGenerator {

    public static JavaFile generateFile(TypeElement type, int maxParams) {
        System.out.println("Process TransmutationManagerTest with %d parameters: %s".formatted(maxParams, type));

        var baseTransmutationManagerTest = BaseTransmutationManagerTest.create(maxParams);

        var className = ClassName.get(type);
        return JavaFile.builder(className.packageName(), baseTransmutationManagerTest)
                .skipJavaLangImports(true)
                .indent(Utils.INDENT)
                .build();
    }

    private static class BaseTransmutationManagerTest {

        private static final ClassName TRANSMUTER_WORLD = ClassName.get("de.schosin.ecs.plugins.transmuter", "TransmuterWorld");
        private static final ClassName NAME = ClassName.get("", "BaseTransmuterManagerTest");

        public static TypeSpec create(int maxParams) {
            var records = IntStream.range(1, maxParams + 3).mapToObj(n -> recordBuilder("C" + n).build()).toList();

            return TypeSpec.classBuilder(NAME)
                    .superclass(Utils.abstractEcsTest(TRANSMUTER_WORLD))
                    .addType(BaseCachedTransmuterTest.baseCachedTransmuterTest(maxParams))
                    .addType(BaseAddTest.baseAddTest(maxParams))
                    .addType(BaseRemoveTest.baseRemoveTest(maxParams))
                    .addTypes(records)
                    .addType(recordBuilder("D1").addSuperinterface(Utils.POOLED).build())
                    .addType(recordBuilder("D2").addSuperinterface(Utils.POOLED).build())
                    .build();
        }

        private static TypeSpec.Builder recordBuilder(String name) {
            return TypeSpec.recordBuilder(name).addModifiers(Modifier.PUBLIC);
        }

    }

    private static class BaseCachedTransmuterTest {

        private static TypeSpec baseCachedTransmuterTest(int maxParams) {
            return TypeSpec.classBuilder("BaseCachedTransmuterTest")
                    .addModifiers(Modifier.ABSTRACT)
                    .addMethod(getTransmuterN(maxParams))
                    .build();
        }

        private static MethodSpec getTransmuterN(int maxParams) {
            var typeVariables = Utils.generateTypeVariables("C", maxParams);
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

            var addN = TransmutationManagerGenerator.TRANSMUTER.nestedClass(TransmuterGenerator.ADD_PREFIX + "N");
            var parameterizedAddN = ParameterizedTypeName.get(addN, typeVariablesArray);

            var createArguments = IntStream.range(1, maxParams + 3).mapToObj(n -> "C%d.class".formatted(n)).collect(Collectors.joining(", "));

            return MethodSpec.methodBuilder("getTransmuterN")
                    .returns(parameterizedAddN)
                    .addStatement("return world.createTransmuter($1T.add(%s))".formatted(createArguments), TransmutationManagerGenerator.TRANSMUTER)
                    .build();
        }

    }

    private static class BaseAddTest {

        private static TypeSpec baseAddTest(int maxParams) {
            return TypeSpec.classBuilder("BaseAddTest")
                    .addModifiers(Modifier.ABSTRACT)
                    .addMethod(getTransmuterN(maxParams))
                    .addMethod(applyTransmuterN(maxParams))
                    .build();
        }

        private static MethodSpec getTransmuterN(int maxParams) {
            var typeVariables = Utils.generateTypeVariables("C", maxParams);
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

            var addN = TransmutationManagerGenerator.TRANSMUTER.nestedClass(TransmuterGenerator.ADD_PREFIX + "N");
            var parameterizedAddN = ParameterizedTypeName.get(addN, typeVariablesArray);

            var createArguments = IntStream.range(1, maxParams + 3).mapToObj(n -> "C%d.class".formatted(n)).collect(Collectors.joining(", "));

            return MethodSpec.methodBuilder("getTransmuterN")
                    .returns(parameterizedAddN)
                    .addStatement("return world.createTransmuter($1T.add(%s))".formatted(createArguments), TransmutationManagerGenerator.TRANSMUTER)
                    .build();
        }

        private static MethodSpec applyTransmuterN(int maxParams) {
            var applyArguments = IntStream.range(1, maxParams + 3).mapToObj(n -> "new C%d()".formatted(n)).collect(Collectors.joining(", "));
            var arrayArguments = IntStream.range(1, maxParams + 3).mapToObj(n -> "C%d.class".formatted(n)).collect(Collectors.joining(", "));

            var body = CodeBlock.builder()
                    .addStatement("var transmuter = getTransmuterN()")
                    .addStatement("transmuter.apply(entityId, %s)".formatted(applyArguments))
                    .addStatement("return new Class<?>[] { %s }".formatted(arrayArguments))
                    .build();

            return MethodSpec.methodBuilder("applyTransmuterN")
                    .addParameter(TypeName.INT, "entityId")
                    .returns(Utils.WILDCARD_CLASS_ARRAY)
                    .addCode(body)
                    .build();
        }

    }

    private static class BaseRemoveTest {

        private static TypeSpec baseRemoveTest(int maxParams) {
            var components = IntStream.range(1, maxParams + 3).mapToObj(n -> "C%d.class".formatted(n)).collect(Collectors.joining(", "));
            var added = FieldSpec.builder(Utils.WILDCARD_CLASS_ARRAY, "ADDED", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("{ %s }".formatted(components))
                    .build();

            return TypeSpec.classBuilder("BaseRemoveTest")
                    .addModifiers(Modifier.ABSTRACT)
                    .addField(added)
                    .addMethod(applyTransmuterN(maxParams))
                    .build();
        }

        private static MethodSpec applyTransmuterN(int maxParams) {
            var createArguments = IntStream.range(1, maxParams + 3).mapToObj(n -> "C%d.class".formatted(n)).collect(Collectors.joining(", "));
            var applyArguments = IntStream.range(1, maxParams + 3).mapToObj(n -> "new C%d()".formatted(n)).collect(Collectors.joining(", "));

            var body = CodeBlock.builder()
                    .addStatement("var transmuter = world.createTransmuter($1T.add(%s).remove(remove))".formatted(createArguments), TransmutationManagerGenerator.TRANSMUTER)
                    .addStatement("transmuter.apply(entityId, %s)".formatted(applyArguments))
                    .addStatement("return ADDED")
                    .build();

            return MethodSpec.methodBuilder("applyTransmuterN")
                    .addParameter(TypeName.INT, "entityId")
                    .addParameter(Utils.WILDCARD_CLASS_ARRAY, "remove").varargs()
                    .returns(Utils.WILDCARD_CLASS_ARRAY)
                    .addCode(body)
                    .build();
        }

    }

}
