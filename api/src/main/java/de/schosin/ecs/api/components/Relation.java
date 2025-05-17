package de.schosin.ecs.api.components;

import de.schosin.ecs.api.Pooled;

public sealed interface Relation {

    /**
     * Marker interface (trait) to enforce that a component type can only be used as a 
     * {@link ComponentRelation#relationship() relationship component}. 
     * 
     * <p>
     * Will cause an exception to be thrown when the component type is used as a regular component 
     * or as a {@link ComponentRelation#target() target component}.
     * </o>
     */
    interface Relationship {
    }

    /**
     * Marker interface (trait) to enforce that a component type can only be used as a 
     * {@link ComponentRelation#target() target component}. 
     * 
     * <p>
     * Will cause an exception to be thrown when the component type is used as a regular component 
     * or as a {@link ComponentRelation#relationship() relationship component}.
     * </o>
     */
    interface Target {
    }

    /**
     * Marker interface (trait) for the ({@link ComponentRelation#relationship() relationship component}. When another
     * {@link ComponentRelation#target() target component} with the same exclusive relationshop is assigned to an entity, 
     * the existing relation will be overriden.
     * 
     * <p>
     * By default different targets can be added to same relationship. Use this marker on the relationship component type
     * to ensure at most one target is assigned at any given time.
     * </p>
     * 
     * <p>
     * <b>Note:</b> This interface extends {@link Relationship}. Components marked as exclusive cannot be used
     * as regular components.
     * </p>
     */
    interface Exclusive extends Relationship {
    }

    non-sealed interface ComponentRelation<R, T> extends Relation, Pooled {
        R relationship();

        T target();
    }

}
