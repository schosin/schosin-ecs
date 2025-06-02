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
                    .addMethod(dataGetInstance(n, typeVariables))
                    .addMethod(dataFree(n, typeVariables))
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
                    .addModifiers(Modifier.FINAL)
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

        private static MethodSpec dataFree(int n, List<TypeVariableName> typeVariables) {
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
