package de.schosin.ecs.plugins.archetype;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Stream;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.engine.components.Component;
import de.schosin.ecs.engine.components.Component.PooledComponent;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMask;
import de.schosin.ecs.engine.components.ComponentMaskManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.utils.ArrayUtils;
import de.schosin.ecs.engine.utils.collections.Pool;

@EcsCodegen
public class ArchetypeManager extends BaseArchetypeManager implements ArchetypePlugin {

    private final ComponentManager componentManager;
    private final ComponentMaskManager componentMaskManager;
    private final EntityManager entityManager;

    private final Pool<InitializeImpl> initializePool = Pool.unbounded(InitializeImpl.class, this::createInitialize);

    public ArchetypeManager(World world) {
        world.addSingleton(this);

        this.componentManager = world.getSingleton(ComponentManager.class);
        this.componentMaskManager = world.getSingleton(ComponentMaskManager.class);
        this.entityManager = world.getSingleton(EntityManager.class);
    }

    private InitializeImpl createInitialize() {
        return new InitializeImpl(componentManager);
    }

    static abstract class AbstractArchetypeImpl implements Archetype {

        protected final ArchetypeManager manager;

        private final ComponentMask componentMask;

        private final Object[] fixed;
        private final Component<?>[] dataLookup;

        protected AbstractArchetypeImpl(BaseArchetypeManager manager, Object[] fixed, AbstractArchetypeImpl parent) {
            this.manager = (ArchetypeManager) manager;

            validateNoPooledComponents(fixed);

            // Create components from parent, validate no duplicates
            var components = Stream.concat(Arrays.stream(parent.dataLookup), Arrays.stream(fixed).map(Object::getClass).map(this.manager.componentManager::getComponent))
                    .toArray(Component<?>[]::new);

            validateNoDuplicateComponents(components);

            // Set fields
            this.componentMask = this.manager.componentMaskManager.getComponentMask(components);

            this.fixed = parent.fixed != null ? ArrayUtils.concat(Object.class, parent.fixed, fixed) : fixed;
            this.dataLookup = Arrays.stream(components).toArray(Component<?>[]::new);
        }

        private void validateNoPooledComponents(Object[] components) {
            for (var component : components) {
                if (component instanceof Pooled) {
                    throw new IllegalArgumentException("Component '%s' passed via 'with(...)' cannot implement Pooled.".formatted(component.getClass().getSimpleName()));
                }
            }
        }

        protected AbstractArchetypeImpl(BaseArchetypeManager manager, Class<?>... components) {
            this.manager = (ArchetypeManager) manager;

            validateNoDuplicateComponents(components);

            this.componentMask = this.manager.componentMaskManager.getComponentMask(components);

            this.fixed = null;
            this.dataLookup = Arrays.stream(components)
                    .map(this.manager.componentManager::getComponent)
                    .toArray(Component<?>[]::new);
        }

        private void validateNoDuplicateComponents(Component<?>[] components) {
            var set = new HashSet<Component<?>>(components.length);
            for (var component : components) {
                if (set.contains(component)) {
                    throw new IllegalArgumentException("Component '%s' already defined, cannot add duplicates.".formatted(component.display()));
                }

                set.add(component);
            }
        }

        private void validateNoDuplicateComponents(Class<?>[] components) {
            var set = new HashSet<Class<?>>(components.length);
            for (var component : components) {
                if (set.contains(component)) {
                    throw new IllegalArgumentException("Component '%s' already defined, cannot add duplicates.".formatted(component.getSimpleName()));
                }

                set.add(component);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Pooled> T getInstance(Class<T> clazz) {
            var component = (PooledComponent<T>) manager.componentManager.getComponent(clazz);
            return component.getInstance();
        }

        protected final int createEntity(Object... components) {
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

                    if (init.added > init.size) {
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

}
