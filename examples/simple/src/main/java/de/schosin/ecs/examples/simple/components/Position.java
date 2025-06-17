package de.schosin.ecs.examples.simple.components;

public class Position {

    public int x, y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Position [x=").append(this.x).append(", y=").append(this.y).append("]");
        return builder.toString();
    }

}
