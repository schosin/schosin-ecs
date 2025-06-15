package de.schosin.ecs.integration.components;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.Exclusive;

public class Favorite implements Pooled, Exclusive {

    public String reason;

    public Favorite init(String reason) {
        this.reason = reason;

        return this;
    }

    @Override
    public void reset() {
        this.reason = null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Favorite [reason=").append(this.reason).append("]");
        return builder.toString();
    }

}
