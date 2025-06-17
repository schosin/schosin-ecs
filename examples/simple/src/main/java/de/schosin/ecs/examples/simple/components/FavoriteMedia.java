package de.schosin.ecs.examples.simple.components;

import de.schosin.ecs.api.components.Relation.Exclusive;

public class FavoriteMedia implements Exclusive {

    public String quote;

    public FavoriteMedia(String quote) {
        this.quote = quote;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FavoriteMedia [quote=").append(this.quote).append("]");
        return builder.toString();
    }

}
