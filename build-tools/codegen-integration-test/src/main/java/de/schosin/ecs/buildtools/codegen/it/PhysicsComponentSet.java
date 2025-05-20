package de.schosin.ecs.buildtools.codegen.it;

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.buildtools.codegen.it.components.Position;
import de.schosin.ecs.buildtools.codegen.it.components.Velocity;

public interface PhysicsComponentSet extends ComponentSet {

    Position pos();

    Velocity velocity();

}
