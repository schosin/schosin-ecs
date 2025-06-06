package de.schosin.ecs.integration.components;

import de.schosin.ecs.api.Pooled;

public class Size implements Pooled {

    public int width;
    public int height;

    public Size init(int width, int height) {
        this.width = width;
        this.height = height;

        return this;
    }

    @Override
    public void reset() {
        this.width = 0;
        this.height = 0;
    }

}
