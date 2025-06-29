package de.schosin.ecs.integration.components;

import de.schosin.ecs.api.Pooled;

public class EntityId implements Pooled {

    public volatile int entityId = -1;

    public EntityId init(int entityId) {
        this.entityId = entityId;

        return this;
    }

    @Override
    public void reset() {
        this.entityId = -1;
    }

}
