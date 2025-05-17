package de.schosin.ecs.utils.junit5;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * {@code SealedSubclassesSource} is a {@link ArgumentsSource}
 * for subclasses of a {@link Class#isSealed() sealed} {@link Class}.
 * 
 * <p>The subclasses will be provided as arguments to the annotated
 * {@code @ParameterizedTest} method.
 * 
 * <p>The sealed class can be specified explicitly using the {@link #value}
 * attribute. Otherwise, the declared type of the first parameter of the
 * {@code @ParameterizedTest} method is used.
 * 
 * <p>The returned subclasses in the sealed hierarchy can be restrichted
 * via the {@link #mode} attribute.
 * 
 * @see org.junit.jupiter.params.provider.ArgumentsSource
 * @see org.junit.jupiter.params.ParameterizedTest
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ArgumentsSource(SealedSubclassesArgumentsProvider.class)
public @interface SealedSubclassesSource {

    /**
     * The sealed class that serves as the source of the subclasses.
     * 
     * <p>If this attribute is not set explicitly, the declared class type
     * of the first parameter of the {@code @ParameterizedTest} method is used. 
     */
    Class<?> value() default Object.class;

    /**
     * The subclass selection mode.
     * 
     * <p>The mode decides which subclasses will be used.
     * 
     * <p>Defaults to {@link Mode#FINAL}
     * 
     * @see Mode#DIRECT
     * @see Mode#FINAL
     * @see Mode#NON_SEALED
     * @see Mode#LEAF
     */
    Mode mode() default Mode.FINAL;

    /**
     * Enumeration of modes for selecting subclasses in the sealed hierarchy.
     */
    enum Mode {

        /**
         * Returns the direct subclasses.
         */
        DIRECT,

        /**
         * Traverses the sealed hierarchy and returns all final subclasses extending a sealed type.
         * 
         * <p>In particular, no classes extending a {@link #NON_SEALED non-sealed} type will be returned.
         */
        FINAL,

        /**
         * Traverses the sealed hierarchy and returns all non-sealed subclasses.
         */
        NON_SEALED,

        /**
         * Traverses the sealed hierarchy and returns all {@link #FINAL final} and {@link #NON_SEALED non-sealed} subclasses.
         */
        LEAF;

    }

}
