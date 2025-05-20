package de.schosin.ecs.buildtools.codegen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.Relation;

/**
 * This annotation is used to generate a {@link ComponentSet} based on
 * a {@link Record record type}. To enable the code generation, make sure
 * that "{@code de.schosin.ecs.buildtools:codegen}" is included as an annotation processor.
 * 
 * <p>
 * When this annotation is used on record, the record must an "int entityId" and atleast one component. 
 * All components of the record must be valid for use as a {@link RegularComponentType}.
 * As such, only non-generic classes and {@link Relation Relations} are supported.
 * </p>
 * 
 * <p>
 * <b>Example</b>
 * 
 * {@snippet:
 *  @ComponentSetConfig
 *  record PhysicsComponents(Position pos, Velocity velocity, EntityRelation<Trainer> trainer) {
 *  }
 * }
 * 
 * The generated {@link ComponentSet} will be named "{@code PhysicsComponentsSet}".
 * To alter the generated name, set the name via {@link #value()}. 
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface ComponentSetConfig {

    /**
     * Name of the generated type. Will default to the annotated
     * type's {@link Class#getSimpleName() simple name} with the 
     * {@link #suffix()} declared by {@link DiscoverComponentSets#suffix()}.
     */
    String value() default "";

}
