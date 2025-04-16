package de.schosin.ecs.engine.compositions;

import de.schosin.ecs.api.components.Composition;
import de.schosin.ecs.api.components.Spec;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.entities.EntityManager;

public class SpecManager extends AbstractSpecManager implements Spec.Creator {

    private final EntityManager entityManager;

    public SpecManager(ComponentManager componentManager, EntityManager entityManager) {
        super(componentManager);

        this.entityManager = entityManager;
    }

    @Override
    public Spec createSpec(Composition.Builder builder) {
        var spec = buildSpec(builder);

        return new SpecImpl(spec);
    }

    class SpecImpl implements Spec {

        final EngineSpec spec;

        protected SpecImpl(EngineSpec spec) {
            this.spec = spec;
        }

        @Override
        public boolean isInterested(int entityId) {
            var componentMask = entityManager.getComponentMask(entityId);

            return spec.isInterested(componentMask.getMask());
        }

    }

}
