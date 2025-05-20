package de.schosin.ecs.buildtools.codegen.it.components;

import de.schosin.ecs.api.components.Relation.EntityRelationship;
import de.schosin.ecs.api.components.Relation.Exclusive;

public enum DockedTo implements EntityRelationship, Exclusive {
    DockerTo
}
