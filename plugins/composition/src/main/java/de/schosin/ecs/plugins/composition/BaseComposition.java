package de.schosin.ecs.plugins.composition;

import java.util.HashSet;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.Components;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.plugins.composition.Composition.Builder;

/**
 * A composition describes the component composition for entities. Entities can be limited by the following aspects:
 * 
 * <ul>
 *  <li>all: An entity must have all components of a given set</li>
 *  <li>one: An entity must have atleast one component of a given set</li>
 *  <li>none: An entity must not have any components of a given set</li>
 * </ul>
 * 
 * Note that calls to {@link #all(Class...)} and {@link #none(Class...)} append further classes, but calls to
 * {@link #one(Class...)} will be independently checked.
 * 
 * The composition can then be used to listen to lifecycle events of entities matching the composition.
 * Use {@link #inserted(IntConsumer)} to be notified when a relevant entity is created or modified,
 * and {@link #removed(IntConsumer)} when an earlier {@link #inserted(IntConsumer) inserted} entity is
 * removed from the world or its component composition is changed such that it does not match this composition
 * anymore.
 * 
 * <p>
 * The method {@link #process(IntConsumer)} can be used to process all entities matching this composition.
 * Note that this will still return {@link World#deleteEntity(int) deleted} or {@link Components#remove(int) modified} 
 * entities until the {@link World#process() world is processed}.
 * </p>
 */
@NullMarked
@EcsCodegen
public interface BaseComposition {

    /**
     * Returns true if this composition is atleast as strict as the {@link Spec spec}.
     * The following criteria must be met:
     * 
     * <ul>
     * <li>If {@code spec} defines {@code all}, {@code this} must define the same components as {@code all}</li>
     * <li>If {@code spec} defines {@code one}, {@code this} must define the same components as {@code one}</li>
     * <li>If {@code spec} defines {@code none}, {@code this} must define the same components as {@code none}</li>
     * </ul>
     * 
     * <p>
     * This composition may be more strict and contain additional restrictings in each aspect (all, one, none).
     * Note that restrictions for one means fewer types, whereas for all and none it means more types.
     * </p>
     * 
     * <p>
     * <b>Note:</b> {@link Composition} extends {@link Spec} and can be used as well.
     * </p>
     * 
     * @param spec spec to test against
     * @return true if this composition is atleast as strict as the spec
     */
    boolean matches(Spec spec);

    /**
     * Callback for whenever an entity matching this composition is created
     * or modified in such a way that it matches this composition.
     * 
     * The callback will only be called once for an entities lifecycle unless
     * it has been {@link #remove() removed} due to composition changes.
     * 
     * *Attention:* Since entity ids may be reused after an entity was removed
     * from the world, any caching of entity ids (e.g. lookup maps) should be 
     * cleaned up via {@link #removed(IntConsumer)}
     * 
     * @param inserted callback
     */
    void inserted(@NonNull IntConsumer inserted);

    /**
     * Callback for whenever an entity matching this composition is removed from
     * the world or modified in such a way that it no longer matches this composition.
     * 
     * @param removed callback
     */
    void removed(@NonNull IntConsumer removed);

    /**
     * Returns the number of entities matching this composition.
     * 
     * @return number of entities
     */
    int getCount();

    /**
     * Returns true if this composition does not contain any entities.
     * 
     * @return true if no entities with this composition exist
     */
    boolean isEmpty();

    /**
     * Process all entities matching this composition.
     * 
     * <p>
     * <b>Attention:</b> If this method is running while another thread is {@link World#process() processing the world},
     * entities might be skipped or the callback might see zeros (0) for the entity id.
     * </p>
     * 
     * @param process callback
     */
    void process(@NonNull IntConsumer process);

    /**
     * Returns a stream containing all entity ids for this composition.
     * 
     * <p>
     * Prefer {@link #process(IntConsumer)} as that method is allocation free.
     * </p>
     * 
     * <p>
     * <b>Attention:</b> If this method is running while another thread is {@link World#process() processing the world},
     * entities might be skipped or the callback might see zeros (0) for the entity id.
     * </p>
     * 
     * @return stream for processing entities
     */
    IntStream stream();

    /**
     * Returns a parallel stream containing all entity ids for this composition.
     * 
     * <p>
     * Prefer {@link #process(IntConsumer)} as that method is allocation free.
     * </p>
     * 
     * <p>
     * <b>Attention:</b> If this method is running while another thread is {@link World#process() processing the world},
     * entities might be skipped or the callback might see zeros (0) for the entity id.
     * </p>
     * 
     * @return parallel stream for processing entities
     */
    IntStream parallelStream();

    
    public record Group(Set<RegularComponentType<?, ?>> components, Set<Builder> builders) {

        Group() {
            this(new HashSet<>(), new HashSet<>());
        }

        public Group copy() {
            var copy = new Group();
            copy.components.addAll(this.components);
            copy.builders.addAll(this.builders);

            return copy;
        }

        public Group add(Class<?>... classes) {
            for (var clazz : classes) {
                this.components.add(ComponentType.component(clazz));
            }

            return this;
        }

        public Group add(RegularComponentType<?, ?>... components) {
            for (var component : components) {
                this.components.add(component);
            }

            return this;
        }

        public Group add(Builder... builders) {
            for (var builder : builders) {
                this.builders.add(builder);
            }

            return this;
        }

        public boolean isEmpty() {
            return components.isEmpty() && builders.isEmpty();
        }

    }

}
