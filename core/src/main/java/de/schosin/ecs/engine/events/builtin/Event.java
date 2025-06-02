package de.schosin.ecs.engine.events.builtin;

import de.schosin.ecs.api.Pooled;

public sealed interface Event extends Pooled permits EntityEvent, EntitiesEvent, ProcessEvent {

    void free();

}
