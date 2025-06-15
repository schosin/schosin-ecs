package de.schosin.ecs.integration.components;

import de.schosin.ecs.api.Pooled;

public class Position implements Pooled {

    public float x;
    public float y;

    public Position init(float x, float y) {
        this.x = x;
        this.y = y;

        return this;
    }

    @Override
    public void reset() {
        this.x = 0f;
        this.y = 0f;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Position [x=").append(this.x).append(", y=").append(this.y).append("]");
        return builder.toString();
    }

}
