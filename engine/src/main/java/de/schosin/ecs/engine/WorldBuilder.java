package de.schosin.ecs.engine;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.World.Builder;

public class WorldBuilder implements World.Builder {

    int processLoops = 3;
    Map<Class<?>, Object> singletons;

    @Override
    public Builder processLoops(int loops) {
        this.processLoops = loops;
        return this;
    }

    @Override
    public Builder singletons(Object... singletons) {
        if (this.singletons == null) {
            this.singletons = new HashMap<>();
        }

        for (var singleton : singletons) {
            if (this.singletons.put(singleton.getClass(), singleton) != null) {
                throw new IllegalArgumentException("Multiple singletons for class %s passed. Singletons must be unique".formatted(singleton.getClass()));
            }
        }

        return this;
    }

    @Override
    public @NonNull World build() {
        return new EngineWorld(this);
    }

}
