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

public class BaseDataTypeGenerator {

    public record TypeVariables(
            List<TypeVariableName> typeVariablesT, List<TypeVariableName> typeVariablesR, List<TypeVariableName> typeVariables,
            TypeVariableName[] typeVariablesArrayT, TypeVariableName[] typeVariablesArrayR, TypeVariableName[] typeVariablesArray) {

        public TypeVariables(List<TypeVariableName> typeVariablesT, List<TypeVariableName> typeVariablesR, List<TypeVariableName> typeVariables) {
            this(typeVariablesT, typeVariablesR, typeVariables,
                    typeVariablesT.toArray(TypeVariableName[]::new), typeVariablesR.toArray(TypeVariableName[]::new), typeVariables.toArray(TypeVariableName[]::new));
        }
    }

    public static final ClassName DATA_PROCESSOR = ClassName.get("de.schosin.ecs.plugins.data.types", "DataProcessor");
    public static final ClassName DATA_PROVIDER = ClassName.get("de.schosin.ecs.plugins.data.types", "DataProvider");

    public static ParameterizedTypeName dataProcessor(TypeName name) {
        return ParameterizedTypeName.get(DATA_PROCESSOR, name);
    }

    public static ParameterizedTypeName dataProvider(TypeName name) {
        return ParameterizedTypeName.get(DATA_PROVIDER, name);
    }

    public static ParameterizedTypeName dataTypeN(int n, List<? extends TypeName> typeVariables) {
        var typeVariablesArray = typeVariables.toArray(TypeName[]::new);
        return ParameterizedTypeName.get(ClassName.get("de.schosin.ecs.plugins.data.types", "DataType" + n), typeVariablesArray);
    }

    public static ParameterizedTypeName dataN(int n, List<TypeVariableName> typeVariables) {
        var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);
        return ParameterizedTypeName.get(ClassName.get("de.schosin.ecs.plugins.data.types", "DataType" + n).nestedClass("Data" + n), typeVariablesArray);
    }

    public static ParameterizedTypeName dataFactory(int n, List<TypeVariableName> typeVariables) {
        var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);
        return ParameterizedTypeName.get(ClassName.get("de.schosin.ecs.plugins.data.types", "DataType" + n).nestedClass("Factory" + n), typeVariablesArray);
    }

    public static ParameterizedTypeName dataProviderN(int n, List<TypeVariableName> typeVariables) {
        var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);
        return ParameterizedTypeName.get(ClassName.get("de.schosin.ecs.plugins.data.types", "DataType" + n).nestedClass("Provider" + n), typeVariablesArray);
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
        files.addAll(IntStream.range(2, maxParams + 1).mapToObj(n -> DataNImpl.create(packageName, n)).toList());

        return files;
    }

    private static class DataTypes {

        public static JavaFile create(String packageName, int maxParams) {
            var methods = IntStream.range(2, maxParams + 1)
                    .mapToObj(i -> factoryMethod(i))
                    .toList();

            var dataT = TypeVariableName.get("T", ClassName.get("", "BaseDataType.Data"));
            var providerS = TypeVariableName.get("S", ParameterizedTypeName.get(DATA_PROVIDER, dataT));
            var dataR = TypeVariableName.get("R", ClassName.get("", "BaseDataType.Data"));
            var processorP = TypeVariableName.get("P", ParameterizedTypeName.get(DATA_PROCESSOR, dataR));

            var parameterizedSuperinterface = ParameterizedTypeName.get(ClassName.get("", "BaseDataType"),
                    TypeVariableName.get("T"), TypeVariableName.get("S"), TypeVariableName.get("R"), TypeVariableName.get("P"));

            var permittedSubclasses = IntStream.range(2, maxParams + 1)
                    .mapToObj(i -> ClassName.get("", "DataType" + i))
                    .toList();

            var type = TypeSpec.interfaceBuilder("DataType")
                    .addModifiers(Modifier.PUBLIC, Modifier.SEALED)
                    .addTypeVariables(List.of(dataT, providerS, dataR, processorP))
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
            var provider = ParameterizedTypeName.get(className.nestedClass("Provider" + n), variables.typeVariablesT.toArray(TypeVariableName[]::new));
            var dataR = dataN(n, variables.typeVariablesR);
            var processor = ParameterizedTypeName.get(className.nestedClass("Processor" + n), variables.typeVariablesR.toArray(TypeVariableName[]::new));

            var superinterface = ClassName.get("", "DataType");
            var parameterizedSuperinterface = ParameterizedTypeName.get(superinterface, dataT, provider, dataR, processor);

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
                    .addType(dataType(n, variables.typeVariablesT))
                    .addType(processorType(n, variables.typeVariablesR))
                    .addType(factoryType(n, variables.typeVariablesT))
                    .addType(providerType(n, variables.typeVariablesT))
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

        static TypeSpec dataType(int n, List<TypeVariableName> typeVariables) {
            var accessors = IntStream.range(1, n + 1)
                    .mapToObj(i -> dataAccessor(i, typeVariables.get(i - 1)))
                    .toList();

            return TypeSpec.interfaceBuilder("Data" + n)
                    .addTypeVariables(typeVariables)
                    .addModifiers(Modifier.PUBLIC)
                    .addSuperinterface(ClassName.get("", "DataType.Data"))
                    .addMethod(dataGetInstance(n, typeVariables))
                    .addMethod(dataFree(n, typeVariables))
                    .addMethods(accessors)
                    .build();
        }

        private static MethodSpec dataGetInstance(int n, List<TypeVariableName> typeVariables) {
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);
            var dataType = ParameterizedTypeName.get(ClassName.get("", "Data" + n), typeVariablesArray);
            var dataNImpl = ClassName.get("", "Data" + n + "Impl");

            var arguments = "";

            var parameters = new ArrayList<ParameterSpec>(typeVariables.size());
            for (int i = 1; i <= n; i++) {
                if (i > 1) {
                    arguments += ", ";
                }
                arguments += "component%d".formatted(i);

                parameters.add(ParameterSpec.builder(typeVariables.get(i - 1), "component" + i).build());
            }

            return MethodSpec.methodBuilder("getInstance")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addTypeVariables(typeVariables)
                    .addParameters(parameters)
                    .returns(dataType)
                    .addStatement("return $1T.getInstance(%s)".formatted(arguments), dataNImpl)
                    .build();
        }

        private static MethodSpec dataFree(int n, List<TypeVariableName> typeVariables) {
            var typeVariablesArray = IntStream.range(0, n).mapToObj(i -> Utils.WILDCARD).toArray(WildcardTypeName[]::new);
            var dataType = ParameterizedTypeName.get(ClassName.get("", "Data" + n), typeVariablesArray);
            var dataNImpl = ClassName.get("", "Data" + n + "Impl");

            return MethodSpec.methodBuilder("free")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(dataType, "data")
                    .addStatement("$1T.free(data)", dataNImpl)
                    .build();
        }

        private static MethodSpec dataAccessor(int i, TypeVariableName type) {
            return MethodSpec.methodBuilder("component" + i)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(type)
                    .build();
        }

        static TypeSpec processorType(int n, List<TypeVariableName> typeVariables) {
            var dataN = dataN(n, typeVariables);

            var superinterface = dataProcessor(dataN);

            var overrideMethodBody = CodeBlock.builder()
                    .add("process(entityId");

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

        static TypeSpec providerType(int n, List<TypeVariableName> typeVariables) {
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);
            var dataN = dataN(n, typeVariables);

            var superinterface = dataProvider(dataN);

            var factoryType = ParameterizedTypeName.get(ClassName.get("", "Factory" + n), typeVariablesArray);

            var getData = MethodSpec.methodBuilder("getData")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .returns(dataN)
                    .addStatement("return $1T.requireNonNull(provide($2T::getInstance), \"return value cannot be null\")", Objects.class, ClassName.get("", "Data" + n))
                    .build();

            var provide = MethodSpec.methodBuilder("provide")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(dataN)
                    .addParameter(factoryType, "factory")
                    .build();

            return TypeSpec.interfaceBuilder("Provider" + n)
                    .addAnnotation(FunctionalInterface.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addTypeVariables(typeVariables)
                    .addSuperinterface(superinterface)
                    .addMethod(getData)
                    .addMethod(provide)
                    .build();
        }

    }

    private static class DataNImpl {

        public static JavaFile create(String packageName, int n) {
            return JavaFile.builder(packageName, dataImpl(n))
                    .skipJavaLangImports(true)
                    .indent(Utils.INDENT)
                    .build();
        }

        static TypeSpec dataImpl(int n) {
            var className = ClassName.get("", "Data" + n + "Impl");
            var interfaceName = ClassName.get("", "DataType" + n).nestedClass("Data" + n);

            var typeVariables = Utils.generateTypeVariables("T", n);

            var parameterizedPool = ParameterizedTypeName.get(Utils.POOL, className);
            var pool = FieldSpec.builder(parameterizedPool, "POOL", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$1T.unbounded($2T.class, $2T::new)", Utils.POOL, className)
                    .build();

            var components = FieldSpec.builder(Utils.bag(ClassName.get(Object.class)), "components", Modifier.PRIVATE, Modifier.FINAL)
                    .initializer("new $1T<>($2T.class, %d)".formatted(n), Utils.BAG, Object.class)
                    .build();

            var fields = IntStream.range(1, n + 1)
                    .mapToObj(i -> componentField(i))
                    .toList();

            var accessors = IntStream.range(1, n + 1)
                    .mapToObj(DataNImpl::dataAccessor)
                    .toList();

            return TypeSpec.classBuilder(className)
                    .addTypeVariables(typeVariables)
                    .addModifiers(Modifier.PUBLIC)
                    .addSuperinterface(interfaceName)
                    .addSuperinterface(Utils.POOLED)
                    .addField(pool)
                    .addField(components)
                    .addFields(fields)
                    .addMethod(dataGetInstance(n, typeVariables))
                    .addMethod(dataFree(n, typeVariables))
                    .addMethod(free())
                    .addMethods(accessors)
                    .addMethod(getComponents(n))
                    .addMethod(dataReset(n))
                    .build();
        }

        private static MethodSpec dataGetInstance(int n, List<TypeVariableName> typeVariables) {
            var dataN = dataN(n, typeVariables);

            var body = CodeBlock.builder()
                    .addStatement("var instance = POOL.getInstance()");

            var parameters = new ArrayList<ParameterSpec>(typeVariables.size());
            for (int i = 1; i <= n; i++) {
                parameters.add(ParameterSpec.builder(typeVariables.get(i - 1), "component" + i).build());

                body.addStatement("instance.component%1$d = component%1$d".formatted(i));
            }

            body.addStatement("return ($1T) instance", dataN);

            return MethodSpec.methodBuilder("getInstance")
                    .addAnnotation(Utils.SUPPRESS_UNCHECKED)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addTypeVariables(typeVariables)
                    .addParameters(parameters)
                    .returns(dataN)
                    .addCode(body.build())
                    .build();
        }

        private static MethodSpec dataFree(int n, List<TypeVariableName> typeVariables) {
            var typeVariablesArray = IntStream.range(0, n).mapToObj(i -> Utils.WILDCARD).toArray(WildcardTypeName[]::new);
            var wildcardDataN = ParameterizedTypeName.get(ClassName.get("", "DataType" + n + ".Data" + n), typeVariablesArray);

            var body = CodeBlock.builder()
                    .beginControlFlow("if (data instanceof $1T impl)", ClassName.get("", "Data" + n + "Impl"))
                    .addStatement("POOL.free(impl)")
                    .endControlFlow()
                    .build();

            return MethodSpec.methodBuilder("free")
                    .addModifiers(Modifier.STATIC)
                    .addParameter(wildcardDataN, "data")
                    .addCode(body)
                    .build();
        }

        private static MethodSpec free() {
            return MethodSpec.methodBuilder("free")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("POOL.free(this)")
                    .build();
        }

        private static FieldSpec componentField(int i) {
            return FieldSpec.builder(Object.class, "component" + i, Modifier.PRIVATE)
                    .build();
        }

        private static MethodSpec dataAccessor(int i) {
            return MethodSpec.methodBuilder("component" + i)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(Object.class)
                    .addStatement("return component%d".formatted(i))
                    .build();
        }

        private static MethodSpec getComponents(int n) {
            var body = CodeBlock.builder();
            body.addStatement("this.components.clear()");

            for (int i = 1; i <= n; i++) {
                body.addStatement("this.components.set(%d, component%d)".formatted(i - 1, i));
            }

            body.addStatement("return this.components");

            return MethodSpec.methodBuilder("getComponents")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(Utils.immutableBag(ClassName.get(Object.class)))
                    .addCode(body.build())
                    .build();
        }

        private static MethodSpec dataReset(int n) {
            var body = CodeBlock.builder();
            body.addStatement("this.components.clear()");

            for (int i = 1; i <= n; i++) {
                body.addStatement("this.component%d = null".formatted(i));
            }

            return MethodSpec.methodBuilder("reset")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addCode(body.build())
                    .build();
        }

    }

}
