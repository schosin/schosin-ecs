package de.schosin.ecs.codegen.generators.plugins.archetype;

import java.util.List;
import java.util.function.IntFunction;
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
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;

import de.schosin.ecs.codegen.Utils;

public class ArchetypeManagerTestGenerator {

    public static JavaFile generateFile(TypeElement type, int maxParams) {
        System.out.println("Process ArchetypeManagerTest with %d parameters: %s".formatted(maxParams, type));

        var baseArchetypeManagerTest = BaseArchetypeManagerTest.create(maxParams);

        var className = ClassName.get(type);
        return JavaFile.builder(className.packageName(), baseArchetypeManagerTest)
                .addStaticImport(Utils.ARRAY_UTILS, "concat")
                .skipJavaLangImports(true)
                .indent(Utils.INDENT)
                .build();
    }

    private static class BaseArchetypeManagerTest {

        private static final ClassName ARCHETYPE_WORLD = ClassName.get("de.schosin.ecs.plugins.archetype", "ArchetypeWorld");
        private static final ClassName NAME = ClassName.get("", "BaseArchetypeManagerTest");

        public static TypeSpec create(int maxParams) {
            var records = IntStream.range(1, maxParams + 1).mapToObj(n -> recordBuilder("C" + n).build()).toList();

            return TypeSpec.classBuilder(NAME)
                    .superclass(Utils.abstractEcsTest(ARCHETYPE_WORLD))
                    .addField(noOpInitialize())
                    .addType(BaseArchetypeNTest.baseArchetypeNTest(maxParams))
                    .addTypes(records)
                    .build();
        }

        private static FieldSpec noOpInitialize() {
            return FieldSpec.builder(ArchetypeManagerGenerator.ARCHETYPE.nestedClass("Initialize"), "NO_OP", Modifier.STATIC, Modifier.FINAL)
                    .addAnnotation(Utils.SUPPRESS_RAWTYPES)
                    .initializer("(i, init) -> {}")
                    .build();
        }

        private static TypeSpec.Builder recordBuilder(String name) {
            return TypeSpec.recordBuilder(name).addModifiers(Modifier.PUBLIC);
        }

    }

    private static class BaseArchetypeNTest {

        private static final ClassName ARCHETYPE_DATA = ClassName.get("", "ArchetypeData");
        private static final ClassName ARCHETYPE_DATA_IMPL = ClassName.get("", "ArchetypeDataImpl");

        private static TypeSpec baseArchetypeNTest(int maxParams) {
            return TypeSpec.classBuilder("BaseArchetypeNTest")
                    .addModifiers(Modifier.ABSTRACT)
                    .addField(emptyArray())
                    .addMethod(createArchetypeN())
                    .addMethod(createArchetypeNWith(maxParams))
                    .addMethod(createEntity())
                    .addMethod(createEntityNull(maxParams))
                    .addMethod(createBatch())
                    .addMethod(createBatchNull(maxParams))
                    .addMethod(n())
                    .addTypes(archetypeData(maxParams))
                    .build();
        }

        private static FieldSpec emptyArray() {
            return FieldSpec.builder(Object[].class, "EMPTY", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("new Object[0]")
                    .build();
        }

        private static MethodSpec createArchetypeN() {
            return MethodSpec.methodBuilder("createArchetypeN")
                    .addModifiers(Modifier.PROTECTED)
                    .addParameter(Utils.WILDCARD_CLASS_ARRAY, "others").varargs()
                    .returns(ARCHETYPE_DATA)
                    .addStatement("return createArchetypeN(EMPTY, others)")
                    .build();
        }

        private static MethodSpec createArchetypeNWith(int maxParams) {
            var components = IntStream.range(1, maxParams + 1).mapToObj(n -> "C" + n + ".class").collect(Collectors.joining(", "));

            var body = CodeBlock.builder()
                    .addStatement("var archetype = world.createArchetype(%s, others)".formatted(components))
                    .beginControlFlow("if (with.length > 0)")
                    .addStatement("archetype = archetype.with(with)")
                    .endControlFlow()
                    .addStatement("return new ArchetypeDataImpl(archetype, concat(Class.class, new Class<?>[] { %s }, others))".formatted(components))
                    .build();

            return MethodSpec.methodBuilder("createArchetypeN")
                    .addModifiers(Modifier.PROTECTED)
                    .addParameter(Object[].class, "with")
                    .addParameter(Utils.WILDCARD_CLASS_ARRAY, "others").varargs()
                    .returns(ARCHETYPE_DATA)
                    .addCode(body)
                    .build();
        }

        private static MethodSpec createEntity() {
            return MethodSpec.methodBuilder("createEntity")
                    .addModifiers(Modifier.PROTECTED)
                    .addParameter(ARCHETYPE_DATA, "data")
                    .addParameter(Object[].class, "others").varargs()
                    .returns(TypeName.INT)
                    .addStatement("return createEntityNull(data, -1, others)")
                    .build();
        }

        private static MethodSpec createEntityNull(int maxParams) {
            var body = CodeBlock.builder();
            body.addStatement("var archetype = (($1T) data).archetype", ARCHETYPE_DATA_IMPL);

            var returnStatement = new StringBuilder();
            returnStatement.append("return archetype.create(").append(System.lineSeparator());
            for (int i = 1; i <= maxParams; i++) {
                returnStatement.append(Utils.INDENT).append("n(new C%d(), nullIndex == %d)".formatted(i, i - 1)).append(", ").append(System.lineSeparator());
            }
            returnStatement.append(Utils.INDENT).append("others)");

            body.addStatement(returnStatement.toString());

            return MethodSpec.methodBuilder("createEntityNull")
                    .addModifiers(Modifier.PROTECTED)
                    .addParameter(ARCHETYPE_DATA, "data")
                    .addParameter(TypeName.INT, "nullIndex")
                    .addParameter(Object[].class, "others").varargs()
                    .returns(TypeName.INT)
                    .addCode(body.build())
                    .build();
        }

        private static MethodSpec createBatch() {
            var others = ParameterizedTypeName.get(IntFunction.class, Object[].class);

            return MethodSpec.methodBuilder("createBatch")
                    .addModifiers(Modifier.PROTECTED)
                    .addParameter(ARCHETYPE_DATA, "data")
                    .addParameter(TypeName.INT, "count")
                    .addParameter(others, "others")
                    .returns(ArrayTypeName.of(TypeName.INT))
                    .addStatement("return createBatchNull(data, -1, count, others)")
                    .build();
        }

        private static MethodSpec createBatchNull(int maxParams) {
            var others = ParameterizedTypeName.get(IntFunction.class, Object[].class);

            var body = CodeBlock.builder();
            body.addStatement("var archetype = (($1T) data).archetype", ARCHETYPE_DATA_IMPL);

            var returnStatement = new StringBuilder();
            returnStatement.append("return archetype.createBatch(count, (i, init) -> init.initialize(").append(System.lineSeparator());
            for (int i = 1; i <= maxParams; i++) {
                returnStatement.append(Utils.INDENT).append("n(new C%d(), nullIndex == %d)".formatted(i, i - 1)).append(", ").append(System.lineSeparator());
            }
            returnStatement.append(Utils.INDENT).append("others.apply(i)))");

            body.addStatement(returnStatement.toString());

            return MethodSpec.methodBuilder("createBatchNull")
                    .addModifiers(Modifier.PROTECTED)
                    .addParameter(ARCHETYPE_DATA, "data")
                    .addParameter(TypeName.INT, "nullIndex")
                    .addParameter(TypeName.INT, "count")
                    .addParameter(others, "others")
                    .returns(ArrayTypeName.of(TypeName.INT))
                    .addCode(body.build())
                    .build();
        }

        private static MethodSpec n() {
            return MethodSpec.methodBuilder("n")
                    .addModifiers(Modifier.PRIVATE)
                    .addTypeVariable(Utils.T)
                    .addParameter(Utils.T, "instance")
                    .addParameter(TypeName.BOOLEAN, "returnNull")
                    .returns(Utils.T)
                    .addStatement("return returnNull ? null : instance")
                    .build();
        }

        private static Iterable<TypeSpec> archetypeData(int maxParams) {
            return List.of(archetypeInterface(), archetypeRecord(maxParams));
        }

        private static TypeSpec archetypeInterface() {
            var archetype = MethodSpec.methodBuilder("archetype")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(ArchetypeManagerGenerator.ARCHETYPE)
                    .build();

            var components = MethodSpec.methodBuilder("components")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(Utils.WILDCARD_CLASS_ARRAY)
                    .build();

            return TypeSpec.interfaceBuilder(ARCHETYPE_DATA)
                    .addModifiers(Modifier.SEALED, Modifier.STATIC)
                    .addMethod(archetype)
                    .addMethod(components)
                    .build();
        }

        private static TypeSpec archetypeRecord(int maxParams) {
            var typeVariablesArray = Utils.generateTypeVariables("C", maxParams).toArray(TypeVariableName[]::new);

            var archetypeN = ArchetypeManagerGenerator.ARCHETYPE.nestedClass("OfN");
            var parameterizedArchetypeN = ParameterizedTypeName.get(archetypeN, typeVariablesArray);

            var constructor = MethodSpec.constructorBuilder()
                    .addParameter(parameterizedArchetypeN, "archetype")
                    .addParameter(Utils.WILDCARD_CLASS_ARRAY, "components")
                    .build();

            return TypeSpec.recordBuilder(ARCHETYPE_DATA_IMPL)
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                    .addSuperinterface(ClassName.get("", "ArchetypeData"))
                    .recordConstructor(constructor)
                    .build();
        }

    }

}
