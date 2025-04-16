package de.schosin.ecs.engine.compositions;

import de.schosin.ecs.api.components.Composition;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.utils.collections.BitVector;

public abstract class AbstractSpecManager {

    private final ComponentManager componentManager;

    protected AbstractSpecManager(ComponentManager componentManager) {
        this.componentManager = componentManager;
    }

    protected EngineSpec buildSpec(Composition.Builder builder) {
        BitVector allVector = null;
        if (!builder.getAll().isEmpty()) {
            allVector = new BitVector();
            for (var clazz : builder.getAll()) {
                allVector.set(componentManager.getData(clazz).id());
            }
        }

        BitVector oneVector = null;
        if (!builder.getOne().isEmpty()) {
            oneVector = new BitVector();
            for (var clazz : builder.getOne()) {
                oneVector.set(componentManager.getData(clazz).id());
            }
        }

        BitVector noneVector = null;
        if (!builder.getNone().isEmpty()) {
            noneVector = new BitVector();
            for (var clazz : builder.getNone()) {
                noneVector.set(componentManager.getData(clazz).id());
            }
        }

        return EngineSpec.create(allVector, oneVector, noneVector);
    }

}
