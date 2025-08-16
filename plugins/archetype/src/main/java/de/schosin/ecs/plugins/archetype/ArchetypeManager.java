package de.schosin.ecs.plugins.archetype;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ObjIntConsumer;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relations;
import de.schosin.ecs.api.components.mappers.ComponentMapper.PooledComponentMapper;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.utils.ArrayUtils;
import de.schosin.ecs.plugins.archetype.BaseArchetype.ArchetypeBatch;
import de.schosin.ecs.plugins.archetype.BaseArchetype.ArchetypeConsumer;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.ImmutableIntBag;
import de.schosin.ecs.utils.collections.Pool;

@EcsCodegen
public class ArchetypeManager extends BaseArchetypeManager implements ArchetypePlugin {

    private final StorageEngine storageEngine;

    private final EntityManager entityManager;
    private final ComponentMapperManager componentMapperManager;

    private final Map<Class<?>, PooledComponentMapper<?>> mappers = new ConcurrentHashMap<>();

    public ArchetypeManager(World world) {
        world.addSingleton(this);

        this.storageEngine = world.getSingleton(StorageEngine.class);

        this.entityManager = world.getSingleton(EntityManager.class);
        this.componentMapperManager = world.getSingleton(ComponentMapperManager.class);
    }

    private <T extends Pooled> T getInstance(Class<T> component) {
        var mapper = mappers.computeIfAbsent(component, key -> componentMapperManager.getPooledComponents(component));

        return component.cast(mapper.getInstance());
    }

    static abstract class AbstractBaseArchetypeImpl<C extends ArchetypeConsumer> implements BaseArchetype<C> {

        protected final ArchetypeManager manager;

        private final Archetype archetype;

        private final Object[] fixed;
        private final ImmutableBag<RegularComponentType<?, ?>> componentTypes;
        private final int size;
        private final int[] mapping;

        private final Pool<Object[]> pool;

        protected AbstractBaseArchetypeImpl(BaseArchetypeManager manager, Object[] fixed, AbstractBaseArchetypeImpl<C> parent) {
            this.manager = (ArchetypeManager) manager;

            validateNoPooledComponents(fixed);

            var componentTypes = new Bag<>(parent.componentTypes);
            for (var component : fixed) {
                componentTypes.add(ComponentType.detectComponentType(component));
            }

            this.componentTypes = componentTypes;

            validateNoDuplicateComponents(this.componentTypes);

            var componentTypesArray = Arrays.copyOf(componentTypes.getData(), componentTypes.getSize());
            this.archetype = this.manager.storageEngine.getArchetype(componentTypesArray);

            this.fixed = parent.fixed != null ? ArrayUtils.concat(Object.class, parent.fixed, fixed) : fixed;
            this.size = this.componentTypes.getSize();

            this.mapping = new int[size];
            for (int i = 0; i < size; i++) {
                this.mapping[i] = archetype.getComponentTypes().indexOf(componentTypesArray[i]);
            }

            this.pool = Pool.unbounded(Object[].class, () -> new Object[size], array -> Arrays.fill(array, null));
        }

        private void validateNoPooledComponents(Object[] components) {
            for (var component : components) {
                if (component instanceof Relation<?>) {
                    throw new IllegalArgumentException("RegularComponent '%s' passed via 'with(...)' cannot implement Relation.".formatted(component));
                }
                if (component instanceof Relations<?>) {
                    throw new IllegalArgumentException("RegularComponent '%s' passed via 'with(...)' cannot implement Relations.".formatted(component));
                }
                if (component instanceof Pooled) {
                    throw new IllegalArgumentException("RegularComponent '%s' passed via 'with(...)' cannot implement Pooled.".formatted(component.getClass().getSimpleName()));
                }
            }
        }

        protected AbstractBaseArchetypeImpl(BaseArchetypeManager manager, RegularComponentType<?, ?>... components) {
            this.manager = (ArchetypeManager) manager;

            this.componentTypes = new Bag<>(components);

            validateNoDuplicateComponents(this.componentTypes);

            this.fixed = null;
            this.archetype = this.manager.storageEngine.getArchetype(components);
            this.size = components.length;

            this.mapping = new int[size];
            for (int i = 0; i < size; i++) {
                this.mapping[i] = archetype.getComponentTypes().indexOf(components[i]);
            }

            this.pool = Pool.unbounded(Object[].class, () -> new Object[size], array -> Arrays.fill(array, null));
        }

        private void validateNoDuplicateComponents(ImmutableBag<RegularComponentType<?, ?>> components) {
            var set = HashSet.<RegularComponentType<?, ?>>newHashSet(components.getSize());
            for (int i = 0, s = components.getSize(); i < s; i++) {
                var component = components.get(i);

                if (!set.add(component)) {
                    throw new IllegalArgumentException("RegularComponent '%s' already defined, cannot add duplicates.".formatted(component));
                }
            }
        }

        @Override
        public int create(C consumer) {
            var components = pool.getInstance();
            consumer.accept(components, 0, mapping);

            if (fixed != null) {
                var start = size - fixed.length;
                for (int f = 0; f < fixed.length; f++) {
                    components[mapping[start + f]] = fixed[f];
                }
            }

            var entityId = manager.entityManager.createEntity(archetype, components);

            pool.free(components);
            return entityId;
        }

        @Override
        public final ImmutableIntBag createBatch(int count, C consumer) {
            return manager.entityManager.createEntities(archetype, count, createConsumer(consumer));
        }

        @Override
        public ArchetypeBatch bind(C consumer) {
            return new ArchetypeBatchImpl(manager.entityManager, archetype, createConsumer(consumer));
        }

        private ObjIntConsumer<Object[]> createConsumer(C consumer) {
            if (fixed == null) {
                return (components, i) -> consumer.accept(components, i, mapping);
            }

            return (components, i) -> {
                consumer.accept(components, i, mapping);

                var start = size - fixed.length;
                for (int f = 0; f < fixed.length; f++) {
                    components[mapping[start + f]] = fixed[f];
                }
            };
        }

        @Override
        public <T extends Pooled> T getInstance(Class<T> clazz) {
            return manager.getInstance(clazz);
        }

    }

    private record ArchetypeBatchImpl(EntityManager entityManager, Archetype archetype, ObjIntConsumer<Object[]> componentsConsumer) implements ArchetypeBatch {

        @Override
        public ImmutableIntBag createBatch(int count) {
            return entityManager.createEntities(archetype, count, componentsConsumer);
        }

    }

}
