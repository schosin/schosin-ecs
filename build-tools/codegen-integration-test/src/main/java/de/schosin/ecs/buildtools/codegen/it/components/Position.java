package de.schosin.ecs.buildtools.codegen.it.components;

import de.schosin.ecs.api.Pooled;

public class Position implements Pooled {
    public int x;
    public int y;

    public Position() {
    }

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }
}
