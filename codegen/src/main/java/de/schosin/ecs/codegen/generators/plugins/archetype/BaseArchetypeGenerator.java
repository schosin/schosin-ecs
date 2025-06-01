package de.schosin.ecs.codegen.generators.plugins.archetype;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;

import de.schosin.ecs.codegen.Utils;
import de.schosin.ecs.codegen.generators.plugins.datatypes.BaseDataTypeGenerator;

public class BaseArchetypeGenerator {

    public static final ClassName ARCHETYPE = ClassName.get("de.schosin.ecs.plugins.archetype", "BaseArchetype");
    public static final ClassName DATA_TYPE = ClassName.get("de.schosin.ecs.plugins.data.types", "DataType");
    public static final ClassName IMMUTABLE_INT_BAG = ClassName.get("de.schosin.ecs.utils.collections", "ImmutableIntBag");

    public static ClassName dataTypeN(int n) {
        if (n <= 1) {
            throw new IllegalArgumentException("n must be 2 or greater");
        }

        return ClassName.get("de.schosin.ecs.plugins.data.types", "DataType" + n);
    }

    public static List<JavaFile> generateFiles(TypeElement archetype, int maxParams) {
        var packageName = ClassName.get(archetype).packageName();

        var files = new ArrayList<JavaFile>(maxParams + 1);
        files.add(ArchetypeCreator.create(packageName, maxParams));
        files.add(Archetype1.create(packageName));
        files.addAll(IntStream.range(2, maxParams + 1).mapToObj(n -> ArchetypeN.create(packageName, n)).toList());

        return files;
    }

    private static class ArchetypeCreator {

        public static JavaFile create(String packageName, int maxParams) {
            return JavaFile.builder(packageName, createArchetypeCreator(maxParams))
                    .addStaticImport(Utils.COMPONENT_TYPE, "component")
                    .skipJavaLangImports(true)
                    .indent(Utils.INDENT)
                    .build();
        }

        public static TypeSpec createArchetypeCreator(int maxParams) {
            return TypeSpec.interfaceBuilder("ArchetypeCreator")
                    .addModifiers(Modifier.PUBLIC)
                    .addMethods(createFactoryMethods(maxParams))
                    .build();
        }

        private static Iterable<MethodSpec> createFactoryMethods(int maxParams) {
            var methods = new ArrayList<MethodSpec>(maxParams);
            methods.add(createClassFactoryMethod());
            methods.addAll(IntStream.range(2, maxParams + 1).mapToObj(n -> createClassFactoryMethod(n)).toList());
            methods.add(createComponentTypeFactoryMethod());
            methods.addAll(IntStream.range(2, maxParams + 1).mapToObj(n -> createComponentTypeFactoryMethod(n)).toList());

            return methods;
        }

        private static MethodSpec createClassFactoryMethod() {
            var archetype = ParameterizedTypeName.get(ClassName.get("", "Archetype1"), Utils.T);

            return MethodSpec.methodBuilder("createArchetype")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addTypeVariable(Utils.T)
                    .addParameter(Utils.clazz(Utils.T), "clazz")
                    .returns(archetype)
                    .addStatement("return createArchetype(component(clazz))")
                    .build();
        }

        private static MethodSpec createComponentTypeFactoryMethod() {
            var componentType = Utils.regularComponentType(Utils.T, Utils.WILDCARD);
            var archetype = ParameterizedTypeName.get(ClassName.get("", "Archetype1"), Utils.T);

            return MethodSpec.methodBuilder("createArchetype")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addTypeVariable(Utils.T)
                    .addParameter(componentType, "componentType")
                    .returns(archetype)
                    .build();
        }

        private static MethodSpec createClassFactoryMethod(int n) {
            var variables = BaseDataTypeGenerator.getTypeVariables(n);
            var typeVariables = variables.typeVariablesT();

            var parameters = IntStream.range(1, typeVariables.size() + 1)
                    .mapToObj(i -> ParameterSpec.builder(Utils.clazz(typeVariables.get(i - 1)), "component" + i).build())
                    .toList();

            var archetype = ParameterizedTypeName.get(ClassName.get("", "Archetype" + n), variables.typeVariablesArrayT());

            var components = "";
            for (int i = 1; i <= typeVariables.size(); i++) {
                if (i > 1) {
                    components += ", ";
                }

                components += "component(component%d)".formatted(i);
            }

            return MethodSpec.methodBuilder("createArchetype")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addTypeVariables(variables.typeVariablesT())
                    .addParameters(parameters)
                    .returns(archetype)
                    .addStatement("return createArchetype(%s)".formatted(components))
                    .build();
        }

        private static MethodSpec createComponentTypeFactoryMethod(int n) {
            var variables = BaseDataTypeGenerator.getTypeVariables(n);
            var typeVariables = variables.typeVariablesT();

            var parameters = IntStream.range(1, typeVariables.size() + 1)
                    .mapToObj(i -> ParameterSpec.builder(Utils.regularComponentType(typeVariables.get(i - 1), Utils.WILDCARD), "component" + i).build())
                    .toList();

            var archetype = ParameterizedTypeName.get(ClassName.get("", "Archetype" + n), variables.typeVariablesArrayT());

            return MethodSpec.methodBuilder("createArchetype")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addTypeVariables(variables.typeVariablesT())
                    .addParameters(parameters)
                    .returns(archetype)
                    .build();
        }

    }

    private static class Archetype1 {

        public static JavaFile create(String packageName) {
            return JavaFile.builder(packageName, createArchetype1())
                    .skipJavaLangImports(true)
                    .indent(Utils.INDENT)
                    .build();
        }

        static TypeSpec createArchetype1() {
            var dataProvider = BaseDataTypeGenerator.dataProvider(Utils.T);
            var archetype = ParameterizedTypeName.get(ARCHETYPE, dataProvider);

            return TypeSpec.interfaceBuilder("Archetype1")
                    .addModifiers(Modifier.PUBLIC)
                    .addTypeVariable(Utils.T)
                    .addSuperinterface(archetype)
                    .addMethod(create())
                    .addMethod(createIndexedBatch())
                    .addMethod(with())
                    .build();
        }

        private static MethodSpec create() {
            return MethodSpec.methodBuilder("create")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addParameter(Utils.T, "component1")
                    .returns(TypeName.INT)
                    .addStatement("return create(() -> component1)")
                    .build();
        }

        private static MethodSpec createIndexedBatch() {
            var function = ParameterizedTypeName.get(ClassName.get(IntFunction.class), Utils.T);

            return MethodSpec.methodBuilder("createIndexedBatch")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addParameter(TypeName.INT, "count")
                    .addParameter(function, "function")
                    .returns(IMMUTABLE_INT_BAG)
                    .addStatement("return createIndexed(count, i -> () -> function.apply(i))")
                    .build();
        }

        private static MethodSpec with() {
            var archetype = ParameterizedTypeName.get(ClassName.get("", "Archetype1"), Utils.T);

            return MethodSpec.methodBuilder("with")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(Object[].class, "components").varargs()
                    .returns(archetype)
                    .build();
        }

    }

    private static class ArchetypeN {

        public static JavaFile create(String packageName, int n) {
            return JavaFile.builder(packageName, createArchetypeN(n))
                    .skipJavaLangImports(true)
                    .indent(Utils.INDENT)
                    .build();
        }

        static TypeSpec createArchetypeN(int n) {
            var variables = BaseDataTypeGenerator.getTypeVariables(n);

            var dataProviderN = BaseDataTypeGenerator.dataProviderN(n, variables.typeVariablesT());
            var archetype = ParameterizedTypeName.get(ARCHETYPE, dataProviderN);

            return TypeSpec.interfaceBuilder("Archetype" + n)
                    .addModifiers(Modifier.PUBLIC)
                    .addTypeVariables(variables.typeVariablesT())
                    .addSuperinterface(archetype)
                    .addMethod(create(variables.typeVariablesT()))
                    .addMethod(createBatch(variables.typeVariablesT()))
                    .addMethod(createIndexedBatch(variables.typeVariablesT()))
                    .addMethod(with(variables.typeVariablesT()))
                    .build();
        }

        private static MethodSpec create(List<TypeVariableName> typeVariables) {
            var parameters = IntStream.range(1, typeVariables.size() + 1)
                    .mapToObj(i -> ParameterSpec.builder(typeVariables.get(i - 1), "component" + i).build())
                    .toList();

            var components = "";
            for (int i = 1; i <= typeVariables.size(); i++) {
                if (i > 1) {
                    components += ", ";
                }

                components += "component" + i;
            }

            return MethodSpec.methodBuilder("create")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addParameters(parameters)
                    .returns(TypeName.INT)
                    .addStatement("return create(factory -> factory.create(%s))".formatted(components))
                    .build();
        }

        private static MethodSpec createBatch(List<TypeVariableName> typeVariables) {
            var dataN = BaseDataTypeGenerator.dataN(typeVariables.size(), typeVariables);
            var supplier = Utils.supplier(dataN);

            var factoryN = BaseDataTypeGenerator.dataFactory(typeVariables.size(), typeVariables);

            return MethodSpec.methodBuilder("createBatch")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addParameter(TypeName.INT, "count")
                    .addParameter(supplier, "supplier")
                    .returns(IMMUTABLE_INT_BAG)
                    .addStatement("return createBatch(count, ($1T factory) -> supplier.get())", factoryN)
                    .build();
        }

        private static MethodSpec createIndexedBatch(List<TypeVariableName> typeVariables) {
            var dataN = BaseDataTypeGenerator.dataN(typeVariables.size(), typeVariables);
            var function = ParameterizedTypeName.get(ClassName.get(IntFunction.class), dataN);

            return MethodSpec.methodBuilder("createIndexedBatch")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addParameter(TypeName.INT, "count")
                    .addParameter(function, "function")
                    .returns(IMMUTABLE_INT_BAG)
                    .addStatement("return createIndexed(count, i -> factory -> function.apply(i))")
                    .build();
        }

        private static MethodSpec with(List<TypeVariableName> typeVariables) {
            var archetype = ParameterizedTypeName.get(ClassName.get("", "Archetype" + typeVariables.size()), typeVariables.toArray(TypeVariableName[]::new));

            return MethodSpec.methodBuilder("with")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(Object[].class, "components").varargs()
                    .returns(archetype)
                    .build();
        }

    }

}
