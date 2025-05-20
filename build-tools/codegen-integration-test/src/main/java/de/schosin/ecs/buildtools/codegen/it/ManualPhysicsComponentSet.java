package de.schosin.ecs.buildtools.codegen.it;

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.buildtools.codegen.it.components.Position;
import de.schosin.ecs.buildtools.codegen.it.components.Velocity;

public sealed interface ManualPhysicsComponentSet extends ComponentSet {

    static ManualPhysicsComponentSet get(int entityId, Object[] components) {
        return get(entityId, (Position) components[0], (Velocity) components[1]);
    }

    static ManualPhysicsComponentSet get(int entityId, Position pos, Velocity velocity) {
        var instance = new ManualPhysicsComponentSetImpl();
        instance.entityId = entityId;
        instance.pos = pos;
        instance.velocity = velocity;

        return instance;
    }

    Position pos();

    Velocity velocity();

    public static final class ManualPhysicsComponentSetImpl implements ManualPhysicsComponentSet {

        private int entityId;
        private Position pos;
        private Velocity velocity;

        @Override
        public int entityId() {
            return entityId;
        }

        @Override
        public Position pos() {
            return pos;
        }

        @Override
        public Velocity velocity() {
            return velocity;
        }

        @Override
        public void reset() {
            this.entityId = -1;
            this.pos = null;
            this.velocity = null;
        }

    }

}