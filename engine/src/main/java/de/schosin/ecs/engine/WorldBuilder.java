package de.schosin.ecs.engine;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.World.Builder;

public class WorldBuilder implements World.Builder {

    int processLoops = 3;

    @Override
    public Builder processLoops(int loops) {
        this.processLoops = loops;
        return this;
    }

    @Override
    public @NonNull World build() {
        return new EngineWorld(this);
    }

}
