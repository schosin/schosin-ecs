package de.schosin.ecs.api.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentRelationType;
import de.schosin.ecs.api.components.types.ComponentType.RegularEntityRelationType;
import de.schosin.ecs.api.components.types.ComponentType.RelationComponentType;

public sealed interface Relation<R> {

    static <R, T> ComponentRelation<R, T> create(R relationship, T target) {
        return RelationHelper.create(relationship, target);
    }

    static <R> EntityRelation<R> create(R relationship, int target) {
        return RelationHelper.create(relationship, target, null);
    }

    static <R, T> EntityRelationData<R, T> create(R relationship, int target, T data) {
        return RelationHelper.create(relationship, target, data);
    }

    static void free(Relation<?> relation) {
        RelationHelper.free(relation);
    }

    RelationComponentType<R, ?, ?> type();

    R relationship();

    /**
     * Marker interface (trait) to enforce that a component type can only be used as a 
     * {@link ComponentRelation#relationship() component relationship}
     * or {@link EntityRelation#relationship() entity relationship}
     * 
     * <p>
     * Will cause an exception to be thrown when the component type is used as a regular component 
     * ,{@link ComponentRelation#target() component target} or {@link EntityRelation#target() entity target}.
     * </p>
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
     * </p>
     */
    interface Target {
    }

    /**
     * Marker interface (trait) to enforce that a component type can only be used as a 
     * {@link EntityRelation#relationship() relationship component}. 
     * 
     * <p>
     * Will cause an exception to be thrown when the component type is used as a regular component 
     * or in a {@link ComponentRelation}.
     * </p>
     */
    interface EntityRelationship extends Relationship {
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

    sealed interface ComponentRelation<R, T> extends Relation<R>, Pooled {

        @Override
        RegularComponentRelationType<R, T, ?> type();

        T target();

    }

    sealed interface EntityRelation<R> extends Relation<R>, Pooled {

        @Override
        RegularEntityRelationType<R, ?> type();

        int target();

    }

    sealed interface EntityRelationData<R, T> extends EntityRelation<R> {

        T data();

    }

}

class RelationHelper {

    private static final List<ComponentRelationImpl> COMPONENT_RELATIONS = new ArrayList<>(32);
    private static final List<EntityRelationImpl> ENTITY_RELATIONS = new ArrayList<>(32);

    @SuppressWarnings("unchecked")
    static synchronized <R, T> ComponentRelation<R, T> create(R relationship, T target) {
        var relation = COMPONENT_RELATIONS.isEmpty() ? new ComponentRelationImpl() : COMPONENT_RELATIONS.removeLast();

        relation.type = Exclusive.class.isAssignableFrom(relationship.getClass())
                ? ComponentType.exclusiveRelation(relationship.getClass().asSubclass(Exclusive.class), target.getClass())
                : ComponentType.relation(relationship.getClass(), target.getClass());

        relation.relationship = relationship;
        relation.target = target;

        return (ComponentRelation<R, T>) relation;
    }

    @SuppressWarnings("unchecked")
    static synchronized <R, T> EntityRelationData<R, T> create(R relationship, int target, T data) {
        var relation = ENTITY_RELATIONS.isEmpty() ? new EntityRelationImpl() : ENTITY_RELATIONS.removeLast();

        relation.type = Exclusive.class.isAssignableFrom(relationship.getClass())
                ? ComponentType.exclusiveRelation(relationship.getClass().asSubclass(Exclusive.class))
                : ComponentType.relation(relationship.getClass());

        relation.relationship = relationship;
        relation.target = target;
        relation.data = data;

        return (EntityRelationData<R, T>) relation;
    }

    static void free(Relation<?> relation) {
        if (relation instanceof ComponentRelationImpl impl) {
            impl.type = null;
            impl.relationship = null;
            impl.target = null;

            COMPONENT_RELATIONS.add(impl);
        }

        if (relation instanceof EntityRelationImpl impl) {
            impl.type = null;
            impl.relationship = null;
            impl.target = -1;
            impl.data = null;

            ENTITY_RELATIONS.add(impl);
        }
    }

    @SuppressWarnings("rawtypes")
    private static final class ComponentRelationImpl implements ComponentRelation {

        private RegularComponentRelationType type;
        private Object relationship;
        private Object target;

        @Override
        public RegularComponentRelationType type() {
            return type;
        }

        @Override
        public Object relationship() {
            return relationship;
        }

        @Override
        public Object target() {
            return target;
        }

        @Override
        public int hashCode() {
            return Objects.hash(relationship, target, type);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ComponentRelationImpl other = (ComponentRelationImpl) obj;
            return Objects.equals(this.relationship, other.relationship) && Objects.equals(this.target, other.target) && Objects.equals(this.type, other.type);
        }

        @Override
        public String toString() {
            return new StringBuilder()
                    .append("ComponentRelation(")
                    .append(this.relationship).append(" / ")
                    .append(this.target).append(")").toString();
        }

    }

    @SuppressWarnings("rawtypes")
    private static final class EntityRelationImpl implements EntityRelationData {

        private RegularEntityRelationType type;
        private Object relationship;
        private int target = -1;
        private Object data;

        @Override
        public RegularEntityRelationType type() {
            return type;
        }

        @Override
        public Object relationship() {
            return relationship;
        }

        @Override
        public int target() {
            return target;
        }

        @Override
        public Object data() {
            return data;
        }

        @Override
        public int hashCode() {
            return Objects.hash(relationship, target, type);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            EntityRelationImpl other = (EntityRelationImpl) obj;
            return Objects.equals(this.relationship, other.relationship) && this.target == other.target && Objects.equals(this.type, other.type);
        }

        @Override
        public String toString() {
            return new StringBuilder()
                    .append("EntityRelation(")
                    .append(this.relationship).append(" / ")
                    .append(this.target).append(")").toString();
        }

    }

}
