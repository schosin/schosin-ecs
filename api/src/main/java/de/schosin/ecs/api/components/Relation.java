package de.schosin.ecs.api.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Result.ComponentRelationResult;
import de.schosin.ecs.api.components.mappers.ComponentRelations.ComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.ComponentRelations.ExclusiveComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.EntityFetchRelations.EntityRelationFetchMapper;
import de.schosin.ecs.api.components.mappers.EntityRelations.EntityRelationMapper;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularEntityRelationType;

/**
 * Sealed base type of component and entity relations. A relation is a type of component
 * that can be added to entities and consist of a relationship component and a target.
 * 
 * <h3>Component relations</h3>
 * 
 * <p>
 * The relationship component describes the type of relation the owning entity has with the target.
 * For example a relationship {@code Location} could be an enum with {@code Start} and {@code End}.
 * Using this relationship, a entity could have more than one {@code Position} component by combinding
 * it with the relationship:
 * 
 * {@snippet:
 * var entityId = world.createEntity(
 *         Relation.create(Location.Start, new Position(0, 0)), 
 *         Relation.create(Location.End, new Position(100, 0));
 * }
 * </p>
 * 
 * <p>
 * A system could then query for entities that have the Relation {@code (Location, Position)} 
 * using {@link ComponentRelationMapper} and would retrieve a 
 * {@link ComponentRelationResult ComponentRelationResult<Location, Position>} that allows the system
 * to iterate through the the assigned relations.
 * </p>
 * 
 * <h3>Entity relations</h3>
 * 
 * <p>
 * Same as with component relations, the relationship component describes the type of relation the owning
 * entity has with the target. Unlike component relations, the target is not a component, but another entity.
 * Given a relationship component {@code Parent}, multiple parent entities could be assigned to an entity:
 * 
 * {@snippet:
 * var motherId = world.createEntity();
 * var fatherId = world.createEntity();
 * 
 * var entityId = world.createEntity(
 *         Relation.create(Parent.Mother, motherId), 
 *         Relation.create(Parent.Father, fatherId);
 * }
 * </p>
 * 
 * <p>
 * A system could then query for these relations by using a {@link EntityRelationMapper}.
 * </p>
 * 
 * <h4>Fetching target data</h4>
 * 
 * <p>
 * Using a {@link EntityRelationFetchMapper} instead of {@link EntityRelationMapper} allows a
 * system to additionally fetch component data for the target entities. When creating a mapper
 * using {@link World#getEntityFetchRelations(de.schosin.ecs.api.components.types.RelationFetchType.EntityRelationFetchType)},
 * only one {@link ComponentType} can be passed. If more than one component must be fetched, consider using a
 * {@link ComponentSet} or a {@link de.schosin.ecs.plugins.data.types.DataType DataType} from the plugin
 * "{@code de.schosin.ecs.plugins:ecs-plugins-data-types}", which are included by default when using the composition plugin.
 * </p>
 * 
 * <h3>Marker interfaces<h3>
 * 
 * <p>
 * In addition to the relation types, additional marker interfaces for the relationship and target components are included.
 * {@link Relationship}, {@link EntityRelationship} and {@link Target} limit the usage of a component to the specific role,
 * whereas {@link Exclusive} limits to number of relations an entity can have to just one, allowing the corresponding mappers
 * {@link ExclusiveComponentRelationMapper} and {@link ExclusiveEntityRelationMapper} to directly work on the relation instead
 * of the {@link Result} types.
 * </p>
 * 
 * @param <R> type of relationship component
 */
public sealed interface Relation<R> {

    /**
     * Creates an instance of a component relation.
     */
    static <R, T> ComponentRelation<R, T> create(R relationship, T target) {
        return RelationHelper.create(relationship, target);
    }

    /**
     * Creates an instance of a entity relation.
     */
    static <R> EntityRelation<R> create(R relationship, int target) {
        return RelationHelper.create(relationship, target, null);
    }

    /**
     * Creates an instance of a entity fetch relation.
     */
    static <R, T> EntityRelationData<R, T> create(R relationship, int target, T data) {
        return RelationHelper.create(relationship, target, data);
    }

    /**
     * Returns a relation to its pool, allowing it to be reused.
     */
    static void free(Relation<?> relation) {
        RelationHelper.free(relation);
    }

    /**
     * Corresponding {@link ComponentType} of the relation.
     */
    RelationComponentType<R, ?, ?> type();

    /**
     * Returns the relationship component of the relation.
     */
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

    /**
     * Data holder for a component relation.
     * 
     * @param <R> type of relationship component
     * @param <T> type of target component
     */
    sealed interface ComponentRelation<R, T> extends Relation<R>, Pooled {

        @Override
        RegularComponentRelationType<R, T, ?> type();

        /**
         * Returns the target component of the relation.
         */
        T target();

    }

    /**
     * Data holder for a entity relation.
     * 
     * @param <R> type of relationship component
     */
    sealed interface EntityRelation<R> extends Relation<R>, Pooled {

        @Override
        RegularEntityRelationType<R, ?> type();

        /**
         * Returns the id of the target entity of the relation.
         */
        int target();

    }

    /**
     * Data holder for a entity fetch relation.
     * 
     * @param <R> type of relationship component
     * @param <T> type of fetched data 
     */
    sealed interface EntityRelationData<R, T> extends EntityRelation<R> {

        /**
         * Returns the data of the target entity.
         */
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
