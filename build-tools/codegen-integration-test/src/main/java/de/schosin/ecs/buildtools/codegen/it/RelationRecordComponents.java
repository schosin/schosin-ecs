package de.schosin.ecs.buildtools.codegen.it;

import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.buildtools.codegen.ComponentSetConfig;
import de.schosin.ecs.buildtools.codegen.it.components.DockedTo;
import de.schosin.ecs.buildtools.codegen.it.components.Location;
import de.schosin.ecs.buildtools.codegen.it.components.Position;

@ComponentSetConfig
public record RelationRecordComponents(
        EntityRelation<DockedTo> dockedTo,
        ComponentRelation<Location.Start, Position> startPos,
        ComponentRelation<Location.End, Position> endPos) {

}
