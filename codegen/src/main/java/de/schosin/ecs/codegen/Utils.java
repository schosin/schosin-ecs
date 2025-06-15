package de.schosin.ecs.codegen;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import javax.lang.model.element.Modifier;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ArrayTypeName;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import com.palantir.javapoet.WildcardTypeName;

public class Utils {

    public static final String INDENT = "    ";

    public static final ClassName ARRAY_UTILS = ClassName.get("de.schosin.ecs.engine.utils", "ArrayUtils");
    public static final ClassName POOL = ClassName.get("de.schosin.ecs.utils.collections", "Pool");
    public static final ClassName POOLED = ClassName.get("de.schosin.ecs.api", "Pooled");
    public static final ClassName BAG = ClassName.get("de.schosin.ecs.utils.collections", "Bag");
    public static final ClassName IMMUTABLE_BAG = ClassName.get("de.schosin.ecs.utils.collections", "ImmutableBag");
    public static final ClassName WORLD = ClassName.get("de.schosin.ecs.api", "World");
    public static final ClassName ABSTRACT_WORLD_TEST = ClassName.get("de.schosin.ecs.engine", "AbstractWorldTest");
    private static final ClassName ABSTRACT_ECS_TEST = ClassName.get("de.schosin.ecs.test", "AbstractEcsTest");

    public static final ClassName OBJECT = ClassName.get(Object.class);
    public static final ClassName CLASS = ClassName.get(Class.class);
    public static final ClassName SUPPLIER = ClassName.get(Supplier.class);

    public static final ClassName JUNIT_NESTED = ClassName.get("org.junit.jupiter.api", "Nested");
    public static final ClassName JUNIT_BEFORE_EACH = ClassName.get("org.junit.jupiter.api", "BeforeEach");

    public static final WildcardTypeName WILDCARD = WildcardTypeName.subtypeOf(Object.class);
    public static final ParameterizedTypeName WILDCARD_CLASS = ParameterizedTypeName.get(CLASS, WILDCARD);
    public static final ArrayTypeName WILDCARD_CLASS_ARRAY = ArrayTypeName.of(WILDCARD_CLASS);

    public static final ClassName COMPONENT_TYPE = ClassName.get("de.schosin.ecs.api.components.types", "ComponentType");
    public static final ParameterizedTypeName COMPONENT_TYPE_WILDCARD = componentType(WILDCARD);
    public static final ArrayTypeName COMPONENT_TYPE_WILDCARD_ARRAY = ArrayTypeName.of(COMPONENT_TYPE_WILDCARD);

    public static final ClassName REGULAR_COMPONENT_TYPE = COMPONENT_TYPE.nestedClass("RegularComponentType");
    public static final ParameterizedTypeName REGULAR_COMPONENT_TYPE_WILDCARD = regularComponentType(WILDCARD);
    public static final ArrayTypeName REGULAR_COMPONENT_TYPE_WILDCARD_ARRAY = ArrayTypeName.of(REGULAR_COMPONENT_TYPE_WILDCARD);

    public static final ClassName COMPONENTS = ClassName.get("de.schosin.ecs.api.components.mappers", "Components");

    public static final ClassName RELATION = ClassName.get("de.schosin.ecs.api.components", "Relation");
    public static final ClassName COMPONENT_RELATION = RELATION.nestedClass("ComponentRelation");
    public static final ClassName ENTITY_RELATION = RELATION.nestedClass("EntityRelation");

    public static final TypeVariableName T = TypeVariableName.get("T");
    public static final TypeVariableName R = TypeVariableName.get("R");

    public static final AnnotationSpec SUPPRESS_UNCHECKED = AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "\"unchecked\"").build();
    public static final AnnotationSpec SUPPRESS_RAWTYPES = AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "\"rawtypes\"").build();

    public static final MethodSpec CONVERT_COMPONENT_TYPE = MethodSpec.methodBuilder("convert")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addParameter(Utils.WILDCARD_CLASS_ARRAY, "classes")
            .returns(Utils.COMPONENT_TYPE_WILDCARD_ARRAY)
            .addStatement("return $1T.stream(classes).map($2T::component).toArray($3T[]::new)", Arrays.class, Utils.COMPONENT_TYPE, Utils.REGULAR_COMPONENT_TYPE_WILDCARD)
            .build();

    public static final MethodSpec CONVERT_REGULAR_COMPONENT_TYPE = MethodSpec.methodBuilder("convert")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addParameter(Utils.WILDCARD_CLASS_ARRAY, "classes")
            .returns(Utils.REGULAR_COMPONENT_TYPE_WILDCARD_ARRAY)
            .addStatement("return $1T.stream(classes).map($2T::component).toArray($3T[]::new)", Arrays.class, Utils.COMPONENT_TYPE, Utils.REGULAR_COMPONENT_TYPE_WILDCARD)
            .build();

    public static ParameterizedTypeName componentType(TypeName type) {
        return componentType(type, WILDCARD);
    }

    public static ParameterizedTypeName componentType(TypeName type, TypeName result) {
        return ParameterizedTypeName.get(COMPONENT_TYPE, type, result);
    }

    public static ParameterizedTypeName regularComponentType(TypeName type) {
        return regularComponentType(type, WILDCARD);
    }

    public static ParameterizedTypeName regularComponentType(TypeName type, TypeName result) {
        return ParameterizedTypeName.get(REGULAR_COMPONENT_TYPE, type, result);
    }

    public static ParameterizedTypeName components(TypeName type, TypeName result) {
        return ParameterizedTypeName.get(COMPONENTS, type, result);
    }

    public static ParameterizedTypeName abstractEcsTest() {
        return abstractEcsTest(WORLD);
    }

    public static ParameterizedTypeName abstractEcsTest(ClassName worldType) {
        return ParameterizedTypeName.get(ABSTRACT_ECS_TEST, worldType);
    }

    public static ParameterizedTypeName bag(TypeName type) {
        return ParameterizedTypeName.get(BAG, type);
    }

    public static ParameterizedTypeName immutableBag(TypeName type) {
        return ParameterizedTypeName.get(IMMUTABLE_BAG, type);
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

    public static FieldSpec createWorld(ClassName type) {
        return FieldSpec.builder(type, "world").build();
    }

    public static MethodSpec setupWorld(ClassName type) {
        return setupWorld(type, "setup" + type.simpleName());
    }

    public static MethodSpec setupWorld(ClassName type, String methodName) {
        return MethodSpec.methodBuilder(methodName)
                .addAnnotation(JUNIT_BEFORE_EACH)
                .addStatement("this.world = $1T.builder($2T.class).build()", WORLD, type)
                .build();
    }

}
