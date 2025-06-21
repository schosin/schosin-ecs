package de.schosin.ecs.plugins.experimental.system.systems;

import java.util.function.IntConsumer;

import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.CompositionPlugin;

/**
 * Iterating system based on {@link Composition Compositions}. Allows processing matching entities one by one.
 * 
 * <p>
 * Override {@link #inserted(int)} and {@link #removed(int)} for the corresponding {@link Composition#inserted(IntConsumer)}
 * and {@link Composition#removed(IntConsumer)} callbacks.
 * </p>
 */
public abstract class IteratingSystem extends AbstractSystem {

    protected final Composition composition;

    public IteratingSystem(CompositionPlugin world, Composition.Builder builder) {
        this.composition = world.createComposition(builder);

        var inserted = getDeclaringClass(this.getClass(), "inserted", int.class);
        if (inserted != null && inserted != IteratingSystem.class) {
            this.composition.inserted(this::inserted);
        }

        var removed = getDeclaringClass(this.getClass(), "removed", int.class);
        if (removed != null && removed != IteratingSystem.class) {
            this.composition.removed(this::removed);
        }
    }

    @Override
    protected final void processSystem() {
        this.composition.process(this::processEntity);
    }

    /**
     * Called when an entity is created or modified such that it matches this systems composition.
     * 
     * @param entityId id of inserted entity
     */
    protected void inserted(int entityId) {
        throw new UnsupportedOperationException("inserted(%d) must only be called when overridden by implementing system".formatted(entityId));
    }

    /**
     * Process system logic for the entity.
     * 
     * @param entityId id of entity
     */
    protected abstract void processEntity(int entityId);

    /**
     * Called when an entity is deleted or modified such that it no longer matches this systems composition.
     * 
     * @param entityId id of inserted entity
     */
    protected void removed(int entityId) {
        throw new UnsupportedOperationException("removed(%d) must only be called when overridden by implementing system".formatted(entityId));
    }

    private static Class<?> getDeclaringClass(Class<?> owner, String name, Class<?>... parameterTypes) {
        try {
            var method = owner.getDeclaredMethod(name, parameterTypes);
            return method.getDeclaringClass();
        } catch (NoSuchMethodException ex) {
            return null;
        } catch (SecurityException ex) {
            throw new IllegalStateException("Internal error: Failed to detect declarering class of method %s: %s".formatted(name, ex.getMessage()), ex);
        }
    }

}
