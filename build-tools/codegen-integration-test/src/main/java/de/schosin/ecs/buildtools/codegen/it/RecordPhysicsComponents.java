package de.schosin.ecs.buildtools.codegen.it;

import de.schosin.ecs.buildtools.codegen.ComponentSetConfig;
import de.schosin.ecs.buildtools.codegen.it.components.Position;
import de.schosin.ecs.buildtools.codegen.it.components.Velocity;

@ComponentSetConfig
public record RecordPhysicsComponents(int entityId, Position pos, Velocity velocity) /*implements RecordPhysicsComponentsSet*/ {
}
