package de.schosin.ecs.api.components.mappers;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.mappers.Components.RegularComponents;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;

/**
 * {@link Components Component mapper} for {@link ClassType} components. Contains additional subtypes providing
 * additional functionality
 * 
 * <ol>
 * <li><b>{@link EnumComponentMapper}:</b> Mapper that allows assigning a default enum instance that is used by {@link EnumComponentMapper#add(int) add(int)}</li>
 * <li><b>{@link PooledComponentMapper}:</b> Mapper for components implementing {@link Pooled}</li>
 * </ol>
 * 
 * @param <T> type of component
 */
public non-sealed interface ComponentMapper<T> extends RegularComponents<T, T> {

    @Override
    ClassType<T> componentType();

    interface EnumComponentMapper<T extends Enum<T>> extends ComponentMapper<T> {

        /**
         * Returns the current component or adds the enum instance defined
         * at {@link Creator#getEnumComponents(Enum) creation}.
         * 
         * @param entityId
         * @return existing or predefined component instance
         */
        @NonNull
        T add(int entityId);

        /**
         * Returns the default instance defined at {@link Creator#getEnumComponents(Enum) creation}.
         * 
         * @return default instance
         */
        @NonNull
        T getDefault();

    }

    interface PooledComponentMapper<T extends Pooled> extends ComponentMapper<T> {

        /**
         * Returns the current component or adds a new component from a pool. 
         * 
         * <p>
         * <b>Attention:</b> Using this method requires a public default constructor.
         * </p>
         * 
         * <p>
         * <b>Note:</b> Components implementing {@link Pooled} will be pooled and reused to reduce GC pressure before 
         * creating an instance via reflection.
         * </p>
         * 
         * @param entityId id of entity
         * @return existing, new, or reused component
         */
        @NonNull
        T add(int entityId);

        /**
         * Returns an unused component from a pool or creates a new instance. 
         * Can be used for {@link Components#add(int, Object)},
         * Archetype, {@link Transmuter}, or {@link World#createEntity(Object...)}.
         * 
         * <p>
         * <b>Attention:</b> Using this method requires a public default constructor.
         * </p>
         * 
         * <p>
         * If the component is given to an entity, the instance will
         * be returned to the pool if the component is removed from
         * the entity or the entity is {@link World#deleteEntity(int) deleted}.
         * </p>
         * 
         * <p>
         * Components implementing {@link Pooled} will be {@link Pooled#reset() reset}
         * when reusing instances from the pool. Newly created instances won't be
         * reset.
         * </p>
         *         
         * @return 
         */
        @NonNull
        T getInstance();

    }

    interface Creator {

        /**
         * Retrieves the mapper of the given component class. This can be used to access components and 
         * to add or remove components from entities.
         * 
         * <p>
         * <b>Note:</b> If T extends Pooled, the returned instance will also implement and
         * can be cast to {@link PooledComponentMapper}. Prefer using {@link #getPooledComponents(Class)}
         * for pooled components.
         * </p>
         *  
         * @param clazz {@link Class} of the component
         * @return class to manage the components defined by the clazz argument 
         */
        default <T> ComponentMapper<T> getComponents(@NonNull Class<T> clazz) {
            return getComponents(ComponentType.component(clazz));
        }

        /**
         * Retrieves the mapper of the given component type. This can be used to access components and 
         * to add or remove components from entities.
         * 
         * @param type {@link RegularComponentType} of the component
         * @return class to manage the components defined by the type argument
         */
        <T> ComponentMapper<T> getComponents(@NonNull ClassType<T> type);

        /**
         * Retrieves the mapper of the given enum class defined by the {@literal defaultComponent}.
         * This can be used to access components and to add or remove components from entities.
         * 
         * <p>
         * {@link EnumComponentMapper#add(int)} will add the default component to the entity an can be
         * used for marker components (singletons) that require no starte.
         * </p>
         * 
         * @param defaultComponent default component for {@link EnumComponentMapper#add(int)}
         * @return class to manage the components defined by the class of {@literal defaultComponent}
         */
        @NonNull
        <T extends Enum<T>> EnumComponentMapper<T> getEnumComponents(@NonNull T defaultComponent);

        /**
         * Retrieves the mapper of the given component class. This can be used to access components and 
         * to add or remove components from entities.
         * 
         * @param clazz {@link Class} of the component
         * @return class to manage the components defined by the clazz argument 
         */
        default <T extends Pooled> PooledComponentMapper<T> getPooledComponents(@NonNull Class<T> clazz) {
            return getPooledComponents(ComponentType.component(clazz));
        }

        /**
         * Retrieves the mapper of the given component type. This can be used to access components and 
         * to add or remove components from entities.
         * 
         * @param type {@link RegularComponentType} of the component
         * @return class to manage the components defined by the type argument
         */
        <T extends Pooled> PooledComponentMapper<T> getPooledComponents(@NonNull ClassType<T> type);

    }

}
