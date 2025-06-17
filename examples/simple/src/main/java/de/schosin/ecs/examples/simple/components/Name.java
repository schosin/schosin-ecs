package de.schosin.ecs.examples.simple.components;

public class Name {

    public String name;

    public Name(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Name [name=").append(this.name).append("]");
        return builder.toString();
    }

}
