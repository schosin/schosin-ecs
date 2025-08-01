package de.schosin.ecs.codegen.generators.plugins.datatypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;

import de.schosin.ecs.codegen.Utils;

public class BaseDataTypeGenerator {

    public record TypeVariables(
            List<TypeVariableName> typeVariablesT, List<TypeVariableName> typeVariablesR, List<TypeVariableName> typeVariables,
            TypeVariableName[] typeVariablesArrayT, TypeVariableName[] typeVariablesArrayR, TypeVariableName[] typeVariablesArray) {

        public TypeVariables(List<TypeVariableName> typeVariablesT, List<TypeVariableName> typeVariablesR, List<TypeVariableName> typeVariables) {
            this(typeVariablesT, typeVariablesR, typeVariables,
                    typeVariablesT.toArray(TypeVariableName[]::new), typeVariablesR.toArray(TypeVariableName[]::new), typeVariables.toArray(TypeVariableName[]::new));
        }
    }

    public static final ClassName DATA_PROCESSOR = ClassName.get("de.schosin.ecs.api.data", "DataProcessor");

    public static ParameterizedTypeName dataProcessor(TypeName name) {
        return ParameterizedTypeName.get(DATA_PROCESSOR, name);
    }

    public static ParameterizedTypeName dataTypeN(int n, List<? extends TypeName> typeVariables) {
        var typeVariablesArray = typeVariables.toArray(TypeName[]::new);
        return ParameterizedTypeName.get(ClassName.get("de.schosin.ecs.plugins.data.types", "DataType" + n), typeVariablesArray);
    }

    public static ParameterizedTypeName dataN(int n, List<TypeVariableName> typeVariables) {
        var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);
        return ParameterizedTypeName.get(ClassName.get("de.schosin.ecs.plugins.data.types", "Data" + n), typeVariablesArray);
    }

    public static ParameterizedTypeName dataFactory(int n, List<TypeVariableName> typeVariables) {
        var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);
        return ParameterizedTypeName.get(ClassName.get("de.schosin.ecs.plugins.data.types", "DataType" + n).nestedClass("Factory" + n), typeVariablesArray);
    }

    public static ParameterizedTypeName dataProcessorN(int n, List<TypeVariableName> typeVariables) {
        var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);
        return ParameterizedTypeName.get(ClassName.get("de.schosin.ecs.plugins.data.types", "DataType" + n).nestedClass("Processor" + n), typeVariablesArray);
    }

    public static TypeVariables getTypeVariables(int n) {
        var typeVariablesT = Utils.generateTypeVariables("T", n);
        var typeVariablesR = Utils.generateTypeVariables("R", n);
        var typeVariables = IntStream.range(0, n)
                .mapToObj(Integer::valueOf)
                .flatMap(i -> Stream.of(typeVariablesT.get(i), typeVariablesR.get(i)))
                .toList();

        return new TypeVariables(typeVariablesT, typeVariablesR, typeVariables);
    }

    public static Iterable<JavaFile> generateFiles(TypeElement type, int maxParams) {
        var packageName = ClassName.get(type).packageName();

        var files = new ArrayList<JavaFile>(maxParams + 1);
        files.add(DataTypes.create(packageName, maxParams));
        files.addAll(IntStream.range(2, maxParams + 1).mapToObj(n -> DataTypeN.create(packageName, n)).toList());
        // files.addAll(IntStream.range(2, maxParams + 1).mapToObj(n -> DataNImpl.create(packageName, n)).toList());

        return files;
    }

    private static class DataTypes {

        public static JavaFile create(String packageName, int maxParams) {
            var methods = IntStream.range(2, maxParams + 1)
                    .mapToObj(i -> factoryMethod(i))
                    .toList();

            var dataT = TypeVariableName.get("T", ClassName.get("", "Data"));
            var dataR = TypeVariableName.get("R", ClassName.get("", "Data"));
            var processorP = TypeVariableName.get("P", ParameterizedTypeName.get(DATA_PROCESSOR, dataR));

            var parameterizedSuperinterface = ParameterizedTypeName.get(ClassName.get("", "BaseDataType"),
                    TypeVariableName.get("T"), TypeVariableName.get("R"), TypeVariableName.get("P"));

            var permittedSubclasses = IntStream.range(2, maxParams + 1)
                    .mapToObj(i -> ClassName.get("", "DataType" + i))
                    .toList();

            var type = TypeSpec.interfaceBuilder("DataType")
                    .addModifiers(Modifier.PUBLIC, Modifier.SEALED)
                    .addTypeVariables(List.of(dataT, dataR, processorP))
                    .addSuperinterface(parameterizedSuperinterface)
                    .addPermittedSubclasses(permittedSubclasses)
                    .addMethods(methods)
                    .build();

            return JavaFile.builder(packageName, type)
                    .skipJavaLangImports(true)
                    .indent(Utils.INDENT)
                    .build();
        }

        private static MethodSpec factoryMethod(int n) {
            var variables = getTypeVariables(n);

            var dataTypeN = dataTypeN(n, variables.typeVariables);

            var parameters = new ArrayList<ParameterSpec>(n);
            var arguments = "";

            for (int i = 1; i <= n; i++) {
                if (i > 1) {
                    arguments += ", ";
                }
                arguments += "type" + i;

                var componentType = ParameterizedTypeName.get(Utils.COMPONENT_TYPE, variables.typeVariablesT.get(i - 1), variables.typeVariablesR.get(i - 1));
                parameters.add(ParameterSpec.builder(componentType, "type" + i).build());
            }

            return MethodSpec.methodBuilder("get")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addTypeVariables(variables.typeVariables)
                    .addParameters(parameters)
                    .returns(dataTypeN)
                    .addStatement("return new $1T<>(%s)".formatted(arguments), ClassName.get("", "DataType" + n))
                    .build();
        }

    }

    private static class DataTypeN {

        public static JavaFile create(String packageName, int n) {
            var className = ClassName.get("", "DataType" + n);

            var variables = getTypeVariables(n);

            var dataT = dataN(n, variables.typeVariablesT);
            var dataR = dataN(n, variables.typeVariablesR);
            var processor = ParameterizedTypeName.get(className.nestedClass("Processor" + n), variables.typeVariablesR.toArray(TypeVariableName[]::new));

            var superinterface = ClassName.get("", "DataType");
            var parameterizedSuperinterface = ParameterizedTypeName.get(superinterface, dataT,  dataR, processor);

            var recordConstructor = MethodSpec.constructorBuilder();
            var constructor = MethodSpec.compactConstructorBuilder().addModifiers(Modifier.PUBLIC);

            for (int i = 1; i <= n; i++) {
                var type = Utils.componentType(variables.typeVariablesT.get(i - 1), variables.typeVariablesR.get(i - 1));
                recordConstructor.addParameter(type, "type" + i);
                constructor.addStatement("$1T.requireNonNull(%s, \"%s cannot be null\")".formatted("type" + i, "type" + i, "type" + i), Objects.class);
            }

            var type = TypeSpec.recordBuilder(className)
                    .addModifiers(Modifier.PUBLIC)
                    .addTypeVariables(variables.typeVariables)
                    .addSuperinterface(parameterizedSuperinterface)
                    .recordConstructor(recordConstructor.build())
                    .addMethod(constructor.build())
                    .addMethod(getComponentTypes(n))
                    .addType(processorType(n, variables.typeVariablesR))
                    .addType(factoryType(n, variables.typeVariablesT))
                    .build();

            return JavaFile.builder(packageName, type)
                    .skipJavaLangImports(true)
                    .indent(Utils.INDENT)
                    .build();
        }

        private static MethodSpec getComponentTypes(int n) {
            var components = "";
            for (int i = 1; i <= n; i++) {
                if (i > 1) {
                    components += ", ";
                }

                components += "type" + i;
            }

            return MethodSpec.methodBuilder("getComponentTypes")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(Utils.COMPONENT_TYPE_WILDCARD_ARRAY)
                    .addStatement("return new $1T<?, ?>[] { %s }".formatted(components), Utils.COMPONENT_TYPE)
                    .build();
        }

        static TypeSpec processorType(int n, List<TypeVariableName> typeVariables) {
            var dataN = dataN(n, typeVariables);

            var superinterface = dataProcessor(dataN);

            var overrideMethodBody = CodeBlock.builder();

            overrideMethodBody.beginControlFlow("if (data == null)");
            overrideMethodBody.add("process(entityId");
            for (int i = 1; i <= n; i++) {
                overrideMethodBody.add(", null");
            }
            overrideMethodBody.add(");");
            overrideMethodBody.add("return;");
            overrideMethodBody.endControlFlow();

            overrideMethodBody.add("process(entityId");

            var processMethod = MethodSpec.methodBuilder("process")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(TypeName.INT, "entityId");

            for (int i = 1; i <= n; i++) {
                overrideMethodBody.add(", data.component%d()".formatted(i));
                processMethod.addParameter(typeVariables.get(i - 1), "component" + i);
            }

            overrideMethodBody.add(")");

            var overrideMethod = MethodSpec.methodBuilder("process")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addStatement(overrideMethodBody.build())
                    .addParameter(TypeName.INT, "entityId")
                    .addParameter(dataN, "data")
                    .build();

            return TypeSpec.interfaceBuilder("Processor" + n)
                    .addAnnotation(FunctionalInterface.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addTypeVariables(typeVariables)
                    .addSuperinterface(superinterface)
                    .addMethod(overrideMethod)
                    .addMethod(processMethod.build())
                    .build();
        }

        static TypeSpec factoryType(int n, List<TypeVariableName> typeVariables) {
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);
            var dataType = ParameterizedTypeName.get(ClassName.get("", "Data" + n), typeVariablesArray);

            var create = MethodSpec.methodBuilder("create")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(dataType);

            for (int i = 1; i <= n; i++) {
                create.addParameter(typeVariables.get(i - 1), "component" + i);
            }

            return TypeSpec.interfaceBuilder("Factory" + n)
                    .addAnnotation(FunctionalInterface.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addTypeVariables(typeVariables)
                    .addMethod(create.build())
                    .build();
        }

    }

}
