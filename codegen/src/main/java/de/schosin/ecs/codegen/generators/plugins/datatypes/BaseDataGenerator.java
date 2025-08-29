package de.schosin.ecs.codegen.generators.plugins.datatypes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;
import com.palantir.javapoet.WildcardTypeName;

import de.schosin.ecs.codegen.Utils;

public class BaseDataGenerator {

    private static final ClassName BASE_DATA = ClassName.get("de.schosin.ecs.plugins.data.types", "BaseData");

    public static Iterable<JavaFile> generateFiles(TypeElement type, int maxParams) {
        var packageName = ClassName.get(type).packageName();

        var files = new ArrayList<JavaFile>(maxParams + 1);
        files.add(Data.create(packageName, maxParams));
        files.addAll(IntStream.range(2, maxParams + 1).mapToObj(n -> DataN.create(packageName, n)).toList());
        files.addAll(IntStream.range(2, maxParams + 1).mapToObj(n -> DataNImpl.create(packageName, n)).toList());
        files.addAll(IntStream.range(2, maxParams + 1).mapToObj(n -> DataAccessorN.create(packageName, n)).toList());

        return files;
    }

    private static class Data {

        public static JavaFile create(String packageName, int maxParams) {
            var methods = IntStream.range(2, maxParams + 1)
                    .mapToObj(i -> factoryMethod(i))
                    .toList();

            var permittedSubclasses = IntStream.range(2, maxParams + 1)
                    .mapToObj(i -> ClassName.get("", "Data" + i))
                    .toList();

            var type = TypeSpec.interfaceBuilder("Data")
                    .addModifiers(Modifier.PUBLIC, Modifier.SEALED)
                    .addSuperinterface(BASE_DATA)
                    .addPermittedSubclasses(permittedSubclasses)
                    .addMethods(methods)
                    .build();

            return JavaFile.builder(packageName, type)
                    .skipJavaLangImports(true)
                    .indent(Utils.INDENT)
                    .build();
        }

        private static MethodSpec factoryMethod(int n) {
            var typeVariables = Utils.generateTypeVariables("T", n);

            var dataN = BaseDataTypeGenerator.dataN(n, typeVariables);

            var parameters = new ArrayList<ParameterSpec>(n);
            var arguments = "";

            for (int i = 1; i <= n; i++) {
                if (i > 1) {
                    arguments += ", ";
                }
                arguments += "data" + i;

                parameters.add(ParameterSpec.builder(typeVariables.get(i - 1), "data" + i).build());
            }

            return MethodSpec.methodBuilder("get")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addTypeVariables(typeVariables)
                    .addParameters(parameters)
                    .returns(dataN)
                    .addStatement("return $1T.getInstance(%s)".formatted(arguments), ClassName.get("", "Data" + n))
                    .build();
        }
    }

    private static class DataN {

        public static JavaFile create(String packageName, int n) {
            var typeVariables = Utils.generateTypeVariables("T", n);

            var accessors = IntStream.range(1, n + 1)
                    .mapToObj(i -> dataAccessor(i, typeVariables.get(i - 1)))
                    .toList();

            var type = TypeSpec.interfaceBuilder("Data" + n)
                    .addTypeVariables(typeVariables)
                    .addModifiers(Modifier.PUBLIC, Modifier.SEALED)
                    .addSuperinterface(ClassName.get("", "Data"))
                    .addPermittedSubclass(ClassName.get("", "Data%dImpl".formatted(n)))
                    .addPermittedSubclass(ClassName.get("", "DataAccessor%d".formatted(n)))
                    .addMethod(dataGetInstance(n, typeVariables))
                    .addMethod(dataGetComponentAccessor(n, typeVariables))
                    .addMethod(dataFree(n))
                    .addMethods(accessors)
                    .build();

            return JavaFile.builder(packageName, type)
                    .skipJavaLangImports(true)
                    .indent(Utils.INDENT)
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

        private static MethodSpec dataGetComponentAccessor(int n, List<TypeVariableName> typeVariables) {
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);
            var dataType = ParameterizedTypeName.get(ClassName.get("", "Data" + n), typeVariablesArray);
            var dataTypeProcessor = ParameterizedTypeName.get(ClassName.get("", "DataType" + n).nestedClass("Processor" + n), typeVariablesArray);
            var dataAccessorN = ClassName.get("", "DataAccessor" + n);

            var arguments = "";

            var parameters = new ArrayList<ParameterSpec>(typeVariables.size());
            for (int i = 1; i <= n; i++) {
                if (i > 1) {
                    arguments += ", ";
                }
                arguments += "mapper%d".formatted(i);

                var mapper = Utils.components(Utils.WILDCARD, typeVariables.get(i - 1));
                parameters.add(ParameterSpec.builder(mapper, "mapper" + i).build());
            }

            parameters.add(ParameterSpec.builder(Utils.DATA_ACCESSOR, "accessor").build());
            arguments += ", accessor";

            var iterableComponentProccessor = ParameterizedTypeName.get(Utils.ITERABLE_COMPONENT_PROCESSOR, dataType, dataTypeProcessor);

            return MethodSpec.methodBuilder("getComponentAccessor")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addTypeVariables(typeVariables)
                    .addParameters(parameters)
                    .returns(iterableComponentProccessor)
                    .addStatement("return $1T.getComponentAccessor(%s)".formatted(arguments), dataAccessorN)
                    .build();
        }

        private static MethodSpec dataFree(int n) {
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

    }

    private static class DataAccessorN {

        public static JavaFile create(String packageName, int n) {
            return JavaFile.builder(packageName, dataAccessor(n))
                    .skipJavaLangImports(true)
                    .indent(Utils.INDENT)
                    .build();
        }

        static TypeSpec dataAccessor(int n) {
            var typeVariables = Utils.generateTypeVariables("T", n);
            var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);

            var className = ClassName.get("", "DataAccessor" + n);
            var dataType = ParameterizedTypeName.get(ClassName.get("", "Data" + n), typeVariablesArray);
            var dataTypeProcessor = ParameterizedTypeName.get(ClassName.get("", "DataType" + n).nestedClass("Processor" + n), typeVariablesArray);

            var iterableComponentProccessor = ParameterizedTypeName.get(Utils.ITERABLE_COMPONENT_PROCESSOR, dataType, dataTypeProcessor);

            var parameterizedPool = ParameterizedTypeName.get(Utils.POOL, className);
            var pool = FieldSpec.builder(parameterizedPool, "POOL", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$1T.unbounded($2T.class, $2T::new)", Utils.POOL, className)
                    .build();

            var componentAccessors = IntStream.range(1, n + 1)
                    .mapToObj(i -> componentAccessor(i, typeVariables.get(i - 1)))
                    .toList();

            return TypeSpec.classBuilder(className)
                    .addModifiers(Modifier.FINAL)
                    .addTypeVariables(typeVariables)
                    .addSuperinterface(iterableComponentProccessor)
                    .addSuperinterface(dataType)
                    .addSuperinterface(Utils.POOLED)
                    .addField(pool)
                    .addField(Utils.DATA_ACCESSOR, "accessor", Modifier.PRIVATE)
                    .addFields(componentAccessors)
                    .addMethods(ComponentAccessorImplementation.methods(n, typeVariables))
                    .addMethods(DataNImplementation.methods(n, typeVariables))
                    .addMethod(reset(n))
                    .addMethod(toString(n))
                    .build();
        }

        private static FieldSpec componentAccessor(int i, TypeVariableName typeR) {
            var type = ParameterizedTypeName.get(Utils.COMPONENT_ACCESSOR, typeR);
            return FieldSpec.builder(type, "accessor" + i, Modifier.PRIVATE).build();
        }

        private static class ComponentAccessorImplementation {

            static List<MethodSpec> methods(int n, List<TypeVariableName> typeVariables) {
                return List.of(
                        dataGetComponentAccessor(n, typeVariables),
                        getComponent(n, typeVariables),
                        process(n, typeVariables),
                        free());
            }

            private static MethodSpec dataGetComponentAccessor(int n, List<TypeVariableName> typeVariables) {
                var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);
                var dataType = ParameterizedTypeName.get(ClassName.get("", "Data" + n), typeVariablesArray);
                var dataTypeProcessor = ParameterizedTypeName.get(ClassName.get("", "DataType" + n).nestedClass("Processor" + n), typeVariablesArray);

                var iterableComponentProccessor = ParameterizedTypeName.get(Utils.ITERABLE_COMPONENT_PROCESSOR, dataType, dataTypeProcessor);

                var parameters = new ArrayList<ParameterSpec>(typeVariables.size());

                var body = CodeBlock.builder();
                body.addStatement("var instance = POOL.getInstance()");

                for (int i = 1; i <= n; i++) {
                    var mapper = Utils.components(Utils.WILDCARD, typeVariables.get(i - 1));
                    parameters.add(ParameterSpec.builder(mapper, "component" + i).build());

                    body.addStatement("instance.accessor%d = component%d.getComponentAccessor(accessor)".formatted(i, i));
                }

                body.addStatement("return instance");

                parameters.add(ParameterSpec.builder(Utils.DATA_ACCESSOR, "accessor").build());

                return MethodSpec.methodBuilder("getComponentAccessor")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addTypeVariables(typeVariables)
                        .addParameters(parameters)
                        .returns(iterableComponentProccessor)
                        .addCode(body.build())
                        .build();
            }

            private static MethodSpec getComponent(int n, List<TypeVariableName> typeVariables) {
                var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);
                var dataType = ParameterizedTypeName.get(ClassName.get("", "Data" + n), typeVariablesArray);

                return MethodSpec.methodBuilder("getComponent")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addParameter(Utils.DATA_ACCESSOR, "accessor")
                        .returns(dataType)
                        .addStatement("this.accessor = accessor")
                        .addStatement("return this")
                        .build();
            }

            private static MethodSpec process(int n, List<TypeVariableName> typeVariables) {
                var typeVariablesArray = typeVariables.toArray(TypeVariableName[]::new);
                var dataTypeProcessor = ParameterizedTypeName.get(ClassName.get("", "DataType" + n).nestedClass("Processor" + n), typeVariablesArray);

                var body = CodeBlock.builder();
                body.beginControlFlow("while(accessor.hasNext())");
                body.add("processor.process(accessor.next()");
                for (int i = 1; i <= n; i++) {
                    body.add(", " + System.lineSeparator()).indent().add("accessor%d.getComponent(accessor)".formatted(i)).unindent();
                }
                body.addStatement(")");
                body.endControlFlow();

                return MethodSpec.methodBuilder("process")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addParameter(Utils.ITERABLE_ACCESSOR, "accessor")
                        .addParameter(dataTypeProcessor, "processor")
                        .addCode(body.build())
                        .build();
            }

            private static MethodSpec free() {
                return MethodSpec.methodBuilder("free")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addStatement("POOL.free(this)")
                        .build();
            }

        }

        private static class DataNImplementation {

            static List<MethodSpec> methods(int n, List<TypeVariableName> typeVariables) {
                var result = new ArrayList<MethodSpec>();
                result.addAll(IntStream.range(1, n + 1).mapToObj(i -> dataAccessor(i, typeVariables.get(i - 1))).toList());

                return result;
            }

            private static MethodSpec dataAccessor(int i, TypeVariableName type) {
                return MethodSpec.methodBuilder("component" + i)
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(type)
                        .addStatement("return accessor%d.getComponent(accessor)".formatted(i))
                        .build();
            }

        }

        private static MethodSpec reset(int n) {
            var body = CodeBlock.builder();

            body.addStatement("this.accessor = null");
            for (int i = 1; i <= n; i++) {
                body.addStatement("this.accessor%d.free()".formatted(i));
                body.addStatement("this.accessor%d = null".formatted(i));
            }

            return MethodSpec.methodBuilder("reset")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addCode(body.build())
                    .build();
        }

        private static MethodSpec toString(int n) {
            var body = CodeBlock.builder();

            body.beginControlFlow("if (accessor == null)");
            body.addStatement("return \"Data%d(invalidated)\"".formatted(n));
            body.endControlFlow();

            body.add("return new $1T().append(\"Data%d(\")".formatted(n), StringBuilder.class);

            for (int i = 1; i <= n; i++) {
                if (i > 1) {
                    body.add(".append(\", \")");
                }

                body.add(System.lineSeparator());
                body.indent().add(".append(component%d())".formatted(i)).unindent();
            }

            body.add(System.lineSeparator()).indent().addStatement(".append(\")\").toString()").unindent();

            return MethodSpec.methodBuilder("toString")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(String.class)
                    .addCode(body.build())
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
            var interfaceName = ClassName.get("", "Data" + n);

            var typeVariables = Utils.generateTypeVariables("T", n);

            var parameterizedPool = ParameterizedTypeName.get(Utils.POOL, className);
            var pool = FieldSpec.builder(parameterizedPool, "POOL", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$1T.unbounded($2T.class, $2T::new)", Utils.POOL, className)
                    .build();

            var fields = IntStream.range(1, n + 1)
                    .mapToObj(i -> componentField(i))
                    .toList();

            var accessors = IntStream.range(1, n + 1)
                    .mapToObj(DataNImpl::dataAccessor)
                    .toList();

            return TypeSpec.classBuilder(className)
                    .addTypeVariables(typeVariables)
                    .addModifiers(Modifier.FINAL)
                    .addSuperinterface(interfaceName)
                    .addSuperinterface(Utils.POOLED)
                    .addField(pool)
                    .addFields(fields)
                    .addMethod(dataGetInstance(n, typeVariables))
                    .addMethod(dataFree(n))
                    .addMethod(free())
                    .addMethods(accessors)
                    .addMethod(dataReset(n))
                    .addMethod(toString(n))
                    .build();
        }

        private static MethodSpec dataGetInstance(int n, List<TypeVariableName> typeVariables) {
            var dataN = BaseDataTypeGenerator.dataN(n, typeVariables);

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

        private static MethodSpec dataFree(int n) {
            var typeVariablesArray = IntStream.range(0, n).mapToObj(i -> Utils.WILDCARD).toArray(WildcardTypeName[]::new);
            var wildcardDataN = ParameterizedTypeName.get(ClassName.get("", "Data" + n), typeVariablesArray);

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

        private static MethodSpec dataReset(int n) {
            var body = CodeBlock.builder();
            for (int i = 1; i <= n; i++) {
                body.addStatement("this.component%d = null".formatted(i));
            }

            return MethodSpec.methodBuilder("reset")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addCode(body.build())
                    .build();
        }

        private static MethodSpec toString(int n) {
            var body = CodeBlock.builder();

            body.add("return new $1T().append(\"Data%d(\")".formatted(n), StringBuilder.class);

            for (int i = 1; i <= n; i++) {
                if (i > 1) {
                    body.add(".append(\", \")");
                }

                body.add(System.lineSeparator());
                body.indent().add(".append(component%d)".formatted(i)).unindent();
            }

            body.add(System.lineSeparator()).indent().addStatement(".append(\")\").toString()").unindent();

            return MethodSpec.methodBuilder("toString")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(String.class)
                    .addCode(body.build())
                    .build();
        }

    }

}
