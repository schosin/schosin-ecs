package de.schosin.ecs.buildtools.codegen.it;

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.buildtools.codegen.it.components.Location;
import de.schosin.ecs.buildtools.codegen.it.components.Position;

public interface LocationComponentSet extends ComponentSet {

    ComponentRelation<Location.Start, Position> startPos();

    ComponentRelation<Location.End, Position> endPos();

}
