package de.schosin.ecs.buildtools.codegen.it;

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.Result.EntityRelationResult;
import de.schosin.ecs.buildtools.codegen.it.components.AlliedWith;

public interface AlliedWithComponentSet extends ComponentSet {

    EntityRelationResult<AlliedWith> alliedWith();

}
