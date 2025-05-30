package de.schosin.ecs.plugins.archetype;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.mappers.ComponentMapper.PooledComponentMapper;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMask;
import de.schosin.ecs.engine.components.ComponentMaskManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.utils.ArrayUtils;
import de.schosin.ecs.plugins.data.DataTypePlugin;
import de.schosin.ecs.plugins.data.types.BaseDataType.Data;
import de.schosin.ecs.plugins.data.types.DataProvider;
import de.schosin.ecs.plugins.data.types.DataType;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.RelationComponent;
import de.schosin.ecs.utils.collections.Pool;

@EcsCodegen
public class ArchetypeManager extends BaseArchetypeManager implements ArchetypePlugin {

    private final ComponentManager componentManager;
    private final ComponentMaskManager componentMaskManager;
    private final EntityManager entityManager;
    private final ComponentMapperManager componentMapperManager;

    private final Map<Class<?>, PooledComponentMapper<?>> mappers = new ConcurrentHashMap<>();

    public ArchetypeManager(World world, DataTypePlugin dataTypePlugin) {
        world.addSingleton(this);

        this.componentManager = world.getSingleton(ComponentManager.class);
        this.componentMaskManager = world.getSingleton(ComponentMaskManager.class);
        this.entityManager = world.getSingleton(EntityManager.class);
        this.componentMapperManager = world.getSingleton(ComponentMapperManager.class);
    }

    private <T extends Pooled> T getInstance(Class<T> component) {
        var mapper = mappers.computeIfAbsent(component, key -> componentMapperManager.getPooledComponents(component));

        return component.cast(mapper.getInstance());
    }

    static abstract class AbstractBaseArchetypeImpl<P extends DataProvider<?>> implements BaseArchetype<P> {

        protected final ArchetypeManager manager;

        private final ComponentMask componentMask;

        private final Object[] fixed;
        private final Component<?, ?>[] dataLookup;
        private final int size;

        private final Pool<Object[]> pool;

        protected AbstractBaseArchetypeImpl(BaseArchetypeManager manager, Object[] fixed, AbstractBaseArchetypeImpl<P> parent) {
            this.manager = (ArchetypeManager) manager;

            validateNoPooledComponents(fixed);

            this.dataLookup = Stream.concat(Arrays.stream(parent.dataLookup), Arrays.stream(fixed).map(component -> (Component<?, ?>) this.manager.componentManager.getComponent(component)))
                    .toArray(Component<?, ?>[]::new);

            validateNoDuplicateComponents(this.dataLookup);

            this.fixed = parent.fixed != null ? ArrayUtils.concat(Object.class, parent.fixed, fixed) : fixed;
            this.componentMask = this.manager.componentMaskManager.getComponentMask(this.dataLookup);
            this.size = this.dataLookup.length - (this.fixed != null ? this.fixed.length : 0);

            var componentsSize = this.dataLookup.length;
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

            this.dataLookup = Arrays.stream(components)
                    .map(this.manager.componentManager::getComponent)
                    .toArray(Component<?, ?>[]::new);

            validateNoDuplicateComponents(this.dataLookup);

            this.fixed = null;
            this.componentMask = this.manager.componentMaskManager.getComponentMask(this.dataLookup);
            this.size = components.length;

            this.pool = Pool.unbounded(Object[].class, () -> new Object[size], array -> Arrays.fill(array, null));
        }

        private void validateNoDuplicateComponents(Component<?, ?>[] components) {
            var set = HashSet.<Component<?, ?>>newHashSet(components.length);
            for (int i = 0, s = components.length; i < s; i++) {
                var component = components[i];

                if (!set.add(component) && !(component instanceof RelationComponent<?, ?, ?>)) {
                    throw new IllegalArgumentException("Component '%s' already defined, cannot add duplicates.".formatted(component.display()));
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
                return manager.entityManager.create(componentMask, components);
            });
        }

        @Override
        public int[] createIndexed(int count, IntFunction<P> provider) {
            var data = fixed != null
                    ? new Object[size + fixed.length][count]
                    : new Object[size][count];

            var components = pool.getInstance();

            for (int i = 0; i < count; i++) {
                var provided = provider.apply(i).getData();
                fillComponents(provided, components);

                for (int c = 0, s = components.length; c < s; c++) {
                    data[c][i] = components[c];
                }

                // Free provided instance
                if (provided instanceof Data d) {
                    d.free();
                }
            }

            pool.free(components);

            return manager.entityManager.createEntities(this.componentMask, data, this.dataLookup);
        }

        private void fillComponents(Object data, Object[] components) {
            if (data == null) {
                var message = size == 1
                        ? "Component cannot be null"
                        : "Components cannot be null. Return result of invoking 'create' on factory.";

                throw new IllegalArgumentException(message);
            }

            // Add components from provider
            if (data instanceof DataType.Data d) {
                var dataComponents = d.getComponents();

                for (int i = 0; i < size; i++) {
                    var expectedMetadata = this.dataLookup[i];

                    var component = dataComponents.get(i);
                    if (component == null) {
                        throw new IllegalArgumentException("Component %d to be of type '%s' cannot be null.".formatted(i + 1, expectedMetadata.type()));
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
