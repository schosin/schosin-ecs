package de.schosin.ecs.buildtools.codegen.it;

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.buildtools.codegen.it.components.DockedTo;

public interface DockedToComponentSet extends ComponentSet {

    EntityRelation<DockedTo> dockedTo();

}
