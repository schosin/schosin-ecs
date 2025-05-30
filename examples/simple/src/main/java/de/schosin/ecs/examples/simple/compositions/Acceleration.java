package de.schosin.ecs.examples.simple.compositions;

public class Acceleration {

    public int ax, ay;

    public Acceleration(int ax, int ay) {
        this.ax = ax;
        this.ay = ay;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Acceleration [ax=").append(this.ax).append(", ay=").append(this.ay).append("]");
        return builder.toString();
    }

}
