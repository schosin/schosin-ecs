package de.schosin.ecs.codegen;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ArrayTypeName;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import com.palantir.javapoet.WildcardTypeName;

public class Utils {

    public static final String INDENT = "    ";

    public static final ClassName ARRAY_UTILS = ClassName.get("de.schosin.ecs.api.utils", "ArrayUtils");
    public static final ClassName POOLED = ClassName.get("de.schosin.ecs.api", "Pooled");
    public static final ClassName BAG = ClassName.get("de.schosin.ecs.engine.utils.collections", "Bag");
    public static final ClassName WORLD = ClassName.get("de.schosin.ecs.api", "World");
    public static final ClassName ABSTRACT_WORLD_TEST = ClassName.get("de.schosin.ecs.engine", "AbstractWorldTest");

    public static final ClassName OBJECT = ClassName.get(Object.class);
    public static final ClassName CLASS = ClassName.get(Class.class);
    public static final ClassName SUPPLIER = ClassName.get(Supplier.class);

    public static final ClassName JUNIT_NESTED = ClassName.get("org.junit.jupiter.api", "Nested");

    public static final ParameterizedTypeName WILDCARD_CLASS = ParameterizedTypeName.get(CLASS, WildcardTypeName.subtypeOf(Object.class));
    public static final ArrayTypeName WILDCARD_CLASS_ARRAY = ArrayTypeName.of(WILDCARD_CLASS);

    public static final TypeVariableName T = TypeVariableName.get("T");

    public static final AnnotationSpec SUPPRESS_UNCHECKED = AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "\"unchecked\"").build();
    public static final AnnotationSpec SUPPRESS_RAWTYPES = AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "\"rawtypes\"").build();

    public static ParameterizedTypeName bag(TypeName type) {
        return ParameterizedTypeName.get(BAG, type);
    }

    public static ParameterizedTypeName clazz(TypeName type) {
        return ParameterizedTypeName.get(CLASS, type);
    }

    public static ParameterizedTypeName supplier(TypeName type) {
        return ParameterizedTypeName.get(SUPPLIER, type);
    }

    public static List<TypeVariableName> generateTypeVariables(String prefix, int count) {
        return generateTypeVariables(prefix, 1, count);
    }

    public static List<TypeVariableName> generateTypeVariables(String prefix, int start, int count) {
        return IntStream.range(start, count + 1).mapToObj(idx -> TypeVariableName.get(prefix + idx)).toList();
    }

}
