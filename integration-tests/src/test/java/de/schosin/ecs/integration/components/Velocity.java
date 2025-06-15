package de.schosin.ecs.integration.components;

import de.schosin.ecs.api.Pooled;

public class Velocity implements Pooled {

    public float vx;
    public float vy;

    public Velocity init(float vx, float vy) {
        this.vx = vx;
        this.vy = vy;

        return this;
    }

    @Override
    public void reset() {
        this.vx = 0f;
        this.vy = 0f;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Velocity [vx=").append(this.vx).append(", vy=").append(this.vy).append("]");
        return builder.toString();
    }

}
