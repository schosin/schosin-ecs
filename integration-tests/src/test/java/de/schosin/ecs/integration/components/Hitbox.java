package de.schosin.ecs.integration.components;

import de.schosin.ecs.api.Pooled;

public class Hitbox implements Pooled {

    // bottom left
    public float x1;
    public float y1;
    
    // top right
    public float x2;
    public float y2;

    @Override
    public void reset() {
        this.x1 = 0f;
        this.y1 = 0f;
        this.x2 = 0f;
        this.y2 = 0f;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Hitbox [x1=").append(this.x1).append(", y1=").append(this.y1).append(", x2=").append(this.x2).append(", y2=").append(this.y2).append("]");
        return builder.toString();
    }

}
