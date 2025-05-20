package de.schosin.ecs.buildtools.codegen.it.components;

import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Relation.Relationship;

public sealed interface Location extends Relationship, Exclusive {
    enum Start implements Location {
        Start
    }

    enum End implements Location {
        End
    }
}
