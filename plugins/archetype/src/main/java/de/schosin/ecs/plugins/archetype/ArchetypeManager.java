package de.schosin.ecs.plugins.archetype;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.mappers.ComponentMapper.PooledComponentMapper;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType;
import de.schosin.ecs.api.data.DataProvider;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.engine.events.builtin.ProcessEvent;
import de.schosin.ecs.engine.utils.ArrayUtils;
import de.schosin.ecs.plugins.data.DataTypePlugin;
import de.schosin.ecs.plugins.data.types.Data;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.entities.ComponentMask;
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

    private final Pool<Bag<Object>> bagPool = Pool.unbounded(Bag.class, () -> new Bag<>(Object.class, 64), Bag::clear);
    private final Pool<Bag<Bag<Object>>> bagsPool = Pool.unbounded(Bag.class, () -> new Bag<>(Bag.class, 64), Bag::clear);
    private final Bag<Bag<Bag<Object>>> lentBags = new Bag<>(Bag.class, 8);

    public ArchetypeManager(World world, DataTypePlugin dataTypePlugin) {
        world.addSingleton(this);

        this.storageEngine = world.getSingleton(StorageEngine.class);

        this.entityManager = world.getSingleton(EntityManager.class);
        this.componentMapperManager = world.getSingleton(ComponentMapperManager.class);

        var eventManager = world.getSingleton(EventManager.class);
        eventManager.registerEventHandler(ProcessEvent.Process.class, this::handleProcess);
    }

    private void handleProcess(ProcessEvent event) {
        var data = this.lentBags.getData();
        for (int i = 0, s = this.lentBags.getSize(); i < s; i++) {
            var bag = data[i];

            var bagData = bag.getData();
            for (int j = 0, js = bag.getSize(); j < js; j++) {
                bagPool.free(bagData[j]);
            }

            bagsPool.free(data[i]);
        }
    }

    private <T extends Pooled> T getInstance(Class<T> component) {
        var mapper = mappers.computeIfAbsent(component, key -> componentMapperManager.getPooledComponents(component));

        return component.cast(mapper.getInstance());
    }

    private Bag<Bag<Object>> getDataBags(int entities) {
        var bags = this.bagsPool.getInstance();
        this.lentBags.add(bags);

        for (int i = bags.getSize(); i < entities; i++) {
            bags.add(bagPool.getInstance());
        }

        return bags;
    }

    static abstract class AbstractBaseArchetypeImpl<P extends DataProvider<?>> implements BaseArchetype<P> {

        protected final ArchetypeManager manager;

        private final ComponentMask componentMask;

        private final Object[] fixed;
        private final ImmutableBag<RegularComponentType<?, ?>> componentTypes;
        private final int size;

        private final Pool<Object[]> pool;

        protected AbstractBaseArchetypeImpl(BaseArchetypeManager manager, Object[] fixed, AbstractBaseArchetypeImpl<P> parent) {
            this.manager = (ArchetypeManager) manager;

            validateNoPooledComponents(fixed);

            var componentTypes = new Bag<>(parent.componentTypes);
            for (var component : fixed) {
                componentTypes.add(ComponentType.detectComponentType(component));
            }

            this.componentTypes = componentTypes;

            validateNoDuplicateComponents(this.componentTypes);

            this.fixed = parent.fixed != null ? ArrayUtils.concat(Object.class, parent.fixed, fixed) : fixed;
            this.componentMask = this.manager.storageEngine.getComponentMask(this.componentTypes.stream().toArray(RegularComponentType<?, ?>[]::new));
            this.size = this.componentTypes.getSize() - (this.fixed != null ? this.fixed.length : 0);

            var componentsSize = this.componentTypes.getSize();
            this.pool = Pool.unbounded(Object[].class, () -> new Object[componentsSize], array -> Arrays.fill(array, null));
        }

        private void validateNoPooledComponents(Object[] components) {
            for (var component : components) {
                if (component instanceof Pooled) {
                    throw new IllegalArgumentException("Component '%s' passed via 'with(...)' cannot implement Pooled.".formatted(component.getClass().getSimpleName()));
                }
            }
        }

        protected AbstractBaseArchetypeImpl(BaseArchetypeManager manager, RegularComponentType<?, ?>... components) {
            this.manager = (ArchetypeManager) manager;

            this.componentTypes = new Bag<>(components);

            validateNoDuplicateComponents(this.componentTypes);

            this.fixed = null;
            this.componentMask = this.manager.storageEngine.getComponentMask(this.componentTypes.stream().toArray(RegularComponentType<?, ?>[]::new));
            this.size = components.length;

            this.pool = Pool.unbounded(Object[].class, () -> new Object[size], array -> Arrays.fill(array, null));
        }

        private void validateNoDuplicateComponents(ImmutableBag<RegularComponentType<?, ?>> components) {
            var set = HashSet.<RegularComponentType<?, ?>>newHashSet(components.getSize());
            for (int i = 0, s = components.getSize(); i < s; i++) {
                var component = components.get(i);

                if (!set.add(component) && !(component instanceof RelationComponentType<?, ?, ?>)) {
                    throw new IllegalArgumentException("Component '%s' already defined, cannot add duplicates.".formatted(component));
                }
            }
        }

        @Override
        public int create(P provider) {
            var data = provider.getData();

            return pool.withInstance(components -> {
                // Fill components from data and fixed
                fillComponents(data, components);

                // Free data instance
                if (data instanceof Data d) {
                    d.free();
                }

                // Create entity
                return manager.entityManager.create(componentMask, componentTypes, components);
            });
        }

        @Override
        public ImmutableIntBag createIndexed(int count, IntFunction<P> provider) {
            var data = manager.getDataBags(count);

            for (int i = 0; i < count; i++) {
                var components = data.get(i);

                var provided = provider.apply(i).getData();

                fillComponents(provided, components.getData());
                components.set(componentTypes.getSize() - 1, components.get(componentTypes.getSize() - 1)); // ensure correct size

                // Free provided instance
                if (provided instanceof Data d) {
                    d.free();
                }
            }

            return manager.entityManager.createEntities(this.componentMask, data, this.componentTypes);
        }

        private void fillComponents(Object data, Object[] components) {
            if (data == null) {
                var message = size == 1
                        ? "Component cannot be null"
                        : "Components cannot be null. Return result of invoking 'create' on factory.";

                throw new IllegalArgumentException(message);
            }

            // Add components from provider
            if (data instanceof Data d) {
                var dataComponents = d.getComponents();

                for (int i = 0; i < size; i++) {
                    var expectedType = this.componentTypes.get(i);

                    var component = dataComponents.get(i);
                    if (component == null) {
                        throw new IllegalArgumentException("Component %d to be of type '%s' cannot be null.".formatted(i + 1, expectedType));
                    }

                    components[i] = component;
                }
            } else {
                components[0] = data;
            }

            // Add fixed components
            if (fixed != null) {
                System.arraycopy(fixed, 0, components, size, fixed.length);
            }
        }

        @Override
        public <T extends Pooled> T getInstance(Class<T> clazz) {
            return manager.getInstance(clazz);
        }

    }

}
