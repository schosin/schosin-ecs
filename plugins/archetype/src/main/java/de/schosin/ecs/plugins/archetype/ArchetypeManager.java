package de.schosin.ecs.plugins.archetype;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.Components.ComponentRelations;
import de.schosin.ecs.api.components.Components.PooledComponentMapper;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMask;
import de.schosin.ecs.engine.components.ComponentMaskManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.utils.ArrayUtils;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.ComponentRelationComponent;
import de.schosin.ecs.utils.collections.Pool;

@EcsCodegen
public class ArchetypeManager extends BaseArchetypeManager implements ArchetypePlugin {

    private final ComponentManager componentManager;
    private final ComponentMaskManager componentMaskManager;
    private final EntityManager entityManager;
    private final ComponentMapperManager componentMapperManager;

    private final Map<Class<?>, PooledComponentMapper<?>> mappers = new ConcurrentHashMap<>();
    private final Map<RelationKey, ComponentRelations<?, ?, ?>> relationMappers = new ConcurrentHashMap<>();

    private final Pool<InitializeImpl> initializePool = Pool.unbounded(InitializeImpl.class, this::createInitialize);
    private final Pool<RelationKey> relationKeyPool = Pool.unbounded(RelationKey.class, RelationKey::new);

    public ArchetypeManager(World world) {
        world.addSingleton(this);

        this.componentManager = world.getSingleton(ComponentManager.class);
        this.componentMaskManager = world.getSingleton(ComponentMaskManager.class);
        this.entityManager = world.getSingleton(EntityManager.class);
        this.componentMapperManager = world.getSingleton(ComponentMapperManager.class);
    }

    private InitializeImpl createInitialize() {
        return new InitializeImpl(this);
    }

    private <T extends Pooled> T getInstance(Class<T> component) {
        var mapper = mappers.computeIfAbsent(component, key -> componentMapperManager.getPooledComponents(component));

        return component.cast(mapper.getInstance());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <R, T> ComponentRelation<R, T> getRelation(R relationship, T target) {
        var mapper = (ComponentRelations) relationKeyPool.withInstance(key -> {
            key.init(relationship.getClass(), target.getClass());

            var existing = relationMappers.get(key);
            if (existing != null) {
                return existing;
            }

            return relationMappers.computeIfAbsent(key.copy(), ignore -> componentMapperManager.getComponentRelations(relationship.getClass(), target.getClass()));
        });

        return mapper.getInstance(relationship, target);
    }

    static abstract class AbstractInitImpl implements Archetype.Initialize.Init {

        private final ArchetypeManager manager;

        protected AbstractInitImpl(ArchetypeManager manager) {
            this.manager = manager;
        }

        @Override
        public <T extends Pooled> T get(Class<T> component) {
            return manager.getInstance(component);
        }

        @Override
        public <R, T> ComponentRelation<R, T> relation(R relationship, T target) {
            return manager.getRelation(relationship, target);
        }

    }

    static abstract class AbstractArchetypeImpl implements Archetype {

        protected final ArchetypeManager manager;

        private final ComponentMask componentMask;

        private final Object[] fixed;
        private final Component<?, ?>[] dataLookup;
        private final int expected;

        protected AbstractArchetypeImpl(BaseArchetypeManager manager, Object[] fixed, AbstractArchetypeImpl parent) {
            this.manager = (ArchetypeManager) manager;

            validateNoPooledComponents(fixed);

            this.dataLookup = Stream.concat(Arrays.stream(parent.dataLookup), Arrays.stream(fixed).map(component -> (Component<?, ?>) this.manager.componentManager.getComponent(component)))
                    .toArray(Component<?, ?>[]::new);

            validateNoDuplicateComponents(this.dataLookup);

            this.fixed = parent.fixed != null ? ArrayUtils.concat(Object.class, parent.fixed, fixed) : fixed;
            this.componentMask = this.manager.componentMaskManager.getComponentMask(this.dataLookup);
            this.expected = this.dataLookup.length - (this.fixed != null ? this.fixed.length : 0);
        }

        private void validateNoPooledComponents(Object[] components) {
            for (var component : components) {
                if (component instanceof Pooled) {
                    throw new IllegalArgumentException("Component '%s' passed via 'with(...)' cannot implement Pooled.".formatted(component.getClass().getSimpleName()));
                }
            }
        }

        protected AbstractArchetypeImpl(BaseArchetypeManager manager, RegularComponentType<?, ?>... components) {
            this.manager = (ArchetypeManager) manager;

            this.dataLookup = Arrays.stream(components)
                    .map(this.manager.componentManager::getComponent)
                    .toArray(Component<?, ?>[]::new);

            validateNoDuplicateComponents(this.dataLookup);

            this.fixed = null;
            this.componentMask = this.manager.componentMaskManager.getComponentMask(this.dataLookup);
            this.expected = this.dataLookup.length;
        }

        private void validateNoDuplicateComponents(Component<?, ?>[] components) {
            var set = HashSet.<Component<?, ?>>newHashSet(components.length);
            for (int i = 0, s = components.length; i < s; i++) {
                var component = components[i];

                if (!set.add(component) && !(component instanceof ComponentRelationComponent<?, ?, ?>)) {
                    throw new IllegalArgumentException("Component '%s' already defined, cannot add duplicates.".formatted(component.display()));
                }
            }
        }

        @Override
        public <T extends Pooled> T getInstance(Class<T> clazz) {
            return manager.getInstance(clazz);
        }

        @Override
        public <R, T> ComponentRelation<R, T> getRelation(R relationship, T target) {
            return manager.getRelation(relationship, target);
        }

        protected final int createEntity(Object... components) {
            if (components.length != expected) {
                throw new IllegalArgumentException("Expected %d added components, but got %d.".formatted(expected, components.length));
            }

            for (int i = TYPESAFE_COUNT, s = components.length; i < s; i++) {
                var component = components[i];
                var expectedMetadata = this.dataLookup[i];

                var metadata = manager.componentManager.getComponent(component);
                if (metadata != expectedMetadata) {
                    throw new IllegalArgumentException("Expected component %d to be of type '%s', but was '%s'.".formatted(i + 1, expectedMetadata.type(), metadata.type()));
                }
            }

            if (fixed != null) {
                components = ArrayUtils.concat(Object.class, components, fixed);
            }

            return manager.entityManager.create(componentMask, components);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        protected final int[] createEntities(int count, Archetype.Initialize initialize) {
            return manager.initializePool.withInstance(init -> {
                init.size = fixed != null
                        ? this.componentMask.getComponents().length - fixed.length
                        : this.componentMask.getComponents().length;

                init.added = 0;

                var data = fixed != null
                        ? new Object[init.size + fixed.length][count]
                        : new Object[init.size][count];

                for (int i = 0; i < count; i++) {
                    init.valid = false;
                    initialize.initialize(i, init);

                    if (!init.valid) {
                        throw new IllegalStateException("Initialization callback not called for entity %d/%d".formatted(i + 1, count));
                    }

                    if (init.added != init.size) {
                        throw new IllegalStateException("Expected %d added components, but got %d for entity %d/%d".formatted(init.size, init.added, i + 1, count));
                    }

                    for (int c = 0; c < init.added; c++) {
                        data[c][i] = init.components.get(c);
                    }

                    if (fixed != null) {
                        for (int c = 0, s = fixed.length; c < s; c++) {
                            data[init.size + c][i] = fixed[c];
                        }
                    }
                }

                return manager.entityManager.createEntities(this.componentMask, data, this.dataLookup);
            });
        }

    }

    private class RelationKey implements Pooled {

        private Class<?> relationship;
        private Class<?> target;

        public RelationKey init(Class<?> relationship, Class<?> target) {
            this.relationship = relationship;
            this.target = target;

            return this;
        }

        public RelationKey copy() {
            var copy = new RelationKey();
            copy.relationship = this.relationship;
            copy.target = this.target;

            return copy;
        }

        @Override
        public void reset() {
            this.relationship = null;
            this.target = null;
        }

    }

}
