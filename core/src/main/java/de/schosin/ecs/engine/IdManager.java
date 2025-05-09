package de.schosin.ecs.engine;

import java.util.concurrent.atomic.AtomicInteger;

import de.schosin.ecs.engine.IdManager.Id.ComponentId;
import de.schosin.ecs.engine.IdManager.Id.EntityId;
import de.schosin.ecs.engine.utils.collections.Bag;
import de.schosin.ecs.engine.utils.collections.IntBag;

public class IdManager {

    public sealed interface Id {

        int id();

        int flags();

        sealed interface SimpleId extends Id {
        }

        sealed interface EntityId extends SimpleId {
        }

        sealed interface ComponentId extends SimpleId {
        }

    }

    private static final int RESERVED_COMPONENT_IDS_COUNT = 1 << 8;

    private final BagManager bagManager;

    private final Bag<Id> ids = new Bag<>(Id.class, 1 << 12);

    private final AtomicInteger nextEntityId = new AtomicInteger(1);
    private final AtomicInteger nextComponentId = new AtomicInteger(1);
    private final IntBag reservedComponentIds = createReservedComponentIds();

    public IdManager(BagManager bagManager) {
        this.bagManager = bagManager;
    }

    public boolean isEntity(int id) {
        return ids.get(id) instanceof EntityId;
    }

    public boolean isComponent(int id) {
        return ids.get(id) instanceof ComponentId;
    }

    public int getFlags(int id) {
        return ids.get(id).flags();
    }

    public EntityId createEntityId() {
        var id = new EntityIdImpl(getNextEntityId(), 0);
        ids.set(id.id(), id);

        return id;
    }

    public ComponentId createComponentId() {
        var id = new ComponentIdImpl(getNextComponentId(), 0);
        ids.set(id.id(), id);

        return id;
    }

    private int getNextEntityId() {
        var id = nextEntityId.getAndIncrement();

        this.bagManager.ensureEntitySize(id);
        return id;
    }

    private synchronized int getNextComponentId() {
        int id = -1;

        if (reservedComponentIds.isEmpty()) {
            id = nextComponentId.getAndIncrement();
        } else {
            id = reservedComponentIds.removeLast();
        }

        this.bagManager.ensureComponentSize(id);
        return id;
    }

    private IntBag createReservedComponentIds() {
        var reservedComponentIds = new IntBag(RESERVED_COMPONENT_IDS_COUNT);
        for (int i = 0; i < RESERVED_COMPONENT_IDS_COUNT; i++) {
            var componentId = nextComponentId.getAndIncrement();

            reservedComponentIds.set(i, RESERVED_COMPONENT_IDS_COUNT - componentId);
        }

        return reservedComponentIds;
    }

    private static abstract sealed class SimpleIdImpl implements Id {

        private final int id;
        private int flags;

        private SimpleIdImpl(int id, int flags) {
            this.id = id;
            this.flags = flags;
        }

        @Override
        public final int id() {
            return id;
        }

        @Override
        public final int flags() {
            return flags;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(this.getClass().getSimpleName()).append("(id = ").append(this.id);
            if (flags > 0) {
                builder.append(", flags = ").append(this.flags);
            }
            return builder.append(")").toString();
        }

    }

    private static final class EntityIdImpl extends SimpleIdImpl implements EntityId {
        private EntityIdImpl(int id, int flags) {
            super(id, flags);
        }
    }

    private static final class ComponentIdImpl extends SimpleIdImpl implements ComponentId {
        private ComponentIdImpl(int id, int flags) {
            super(id, flags);
        }
    }

}
