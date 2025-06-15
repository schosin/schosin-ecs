package de.schosin.ecs.buildtools.codegen.it;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.ComponentSetConfig;
import de.schosin.ecs.buildtools.codegen.it.components.Position;
import de.schosin.ecs.buildtools.codegen.it.components.Velocity;

public class PhysicsSystem {

    private int entityId;

    private Position position;
    private Velocity velocity;

    public PhysicsSystem(World world) {
        var mapper = world.getComponents(MethodBasedPhysicsComponentSet.TYPE);

        this.position = new Position(1, 2);
        this.velocity = new Velocity();
        this.entityId = world.createEntity(position, velocity);

        var components = mapper.get(entityId);
        if (components == null) {
            throw new IllegalStateException("Expected non-null component set");
        }

        MethodBasedPhysicsComponentSet.Processor processor = this::processEntities;
        processor.process(entityId, components);
    }

    @ComponentSetConfig("MethodBasedPhysicsComponentSet")
    private void processEntities(int entityId, Position position, Velocity velocity) {
        if (this.entityId != entityId) {
            throw new IllegalStateException("Expected entity %d, but got %d".formatted(this.entityId, entityId));
        }

        // identity check
        if (this.position != position) {
            throw new IllegalStateException("Expected position %s (%s), but got %s (%s)"
                    .formatted(this.position, System.identityHashCode(this.position), position, System.identityHashCode(position)));
        }

        // identity check
        if (this.velocity != velocity) {
            throw new IllegalStateException("Expected velocity %s (%s), but got %s (%s)"
                    .formatted(this.velocity, System.identityHashCode(this.velocity), velocity, System.identityHashCode(velocity)));
        }
    }

}
