package de.schosin.ecs.examples.simple.compositions;

public class Velocity {

    public int vx, vy;

    public Velocity(int vx, int vy) {
        this.vx = vx;
        this.vy = vy;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Velocity [vx=").append(this.vx).append(", vy=").append(this.vy).append("]");
        return builder.toString();
    }

}
