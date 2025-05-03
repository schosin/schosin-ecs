package de.schosin.ecs.engine.utils;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ClassUtils {

    private static final Set<Class<?>> PRIMITIVE_WRAPPERS = Set.of(Boolean.class, Byte.class, Character.class, Short.class, Integer.class, Long.class, Float.class, Double.class);

    public enum ClassType {

        CLASS(true), ENUM(true), RECORD(true),
        GENERIC, INTERFACE, ANNOTATION, PRIMITIVE, PRIMITIVE_WRAPPER, ARRAY, SYNTHETIC, ABSTRACT, STRING;

        public static final List<String> ALLOWED_COMPONENT_TYPES = Arrays.stream(values())
                .filter(ClassType::isValidComponent)
                .map(valid -> valid.name().toLowerCase())
                .toList();

        private final boolean validComponent;

        private ClassType() {
            this(false);
        }

        private ClassType(boolean validComponent) {
            this.validComponent = validComponent;
        }

        public boolean isValidComponent() {
            return this.validComponent;
        }

    }

    public static ClassType detectType(Class<?> clazz) {
        if (clazz.isAnnotation()) {
            return ClassType.ANNOTATION;
        } else if (clazz.isInterface()) {
            return ClassType.INTERFACE;
        } else if (clazz.isEnum()) {
            return ClassType.ENUM;
        } else if (clazz.isPrimitive()) {
            return ClassType.PRIMITIVE;
        } else if (clazz.isArray()) {
            return ClassType.ARRAY;
        } else if (clazz.isRecord()) { // Java 14+
            return ClassType.RECORD;
        } else if (clazz.isSynthetic()) {
            return ClassType.SYNTHETIC;
        }

        var modifiers = clazz.getModifiers();
        if (Modifier.isAbstract(modifiers)) {
            return ClassType.ABSTRACT;
        }

        if (clazz.getTypeParameters().length > 0) {
            return ClassType.GENERIC;
        }

        if (PRIMITIVE_WRAPPERS.contains(clazz)) {
            return ClassType.PRIMITIVE_WRAPPER;
        }

        if (String.class.equals(clazz)) {
            return ClassType.STRING;
        }

        return ClassType.CLASS;
    }

}
