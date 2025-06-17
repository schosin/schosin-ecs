package de.schosin.ecs.examples.simple.components;

import de.schosin.ecs.api.components.Relation.Exclusive;

public class Birth implements Exclusive {

    public int year;

    public Birth(int year) {
        this.year = year;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Birth [year=").append(this.year).append("]");
        return builder.toString();
    }
    
}
