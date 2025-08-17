package de.schosin.ecs.plugins.composition.manager.components.records;

import de.schosin.ecs.api.components.Relation.Exclusive;

public record ExclusiveRelationship(int value) implements Exclusive {
}
