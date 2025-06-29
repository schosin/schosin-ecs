package de.schosin.ecs.integration.components;

import de.schosin.ecs.api.Pooled;

public class Acceleration implements Pooled {

    public float ax;
    public float ay;

    public Acceleration init(float ax, float ay) {
        this.ax = ax;
        this.ay = ay;

        return this;
    }

    @Override
    public void reset() {
        this.ax = 0f;
        this.ay = 0f;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Acceleration [ax=").append(this.ax).append(", ay=").append(this.ay).append("]");
        return builder.toString();
    }

}
