package de.schosin.ecs.engine.events.builtin;

public sealed interface Event permits EntityEvent, EntitiesEvent, ProcessEvent {
}
