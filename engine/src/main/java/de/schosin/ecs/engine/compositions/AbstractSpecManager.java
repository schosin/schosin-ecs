package de.schosin.ecs.engine.compositions;

import java.util.HashSet;
import java.util.Set;

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

        Set<BitVector> oneVectors = null;
        if (!builder.getOnes().isEmpty()) {
            for (var one : builder.getOnes()) {
                if (one.isEmpty()) {
                    continue;
                }
                
                if (oneVectors == null) {
                    oneVectors = new HashSet<>();
                }
                
                var oneVector = new BitVector();
                for (var clazz : one) {
                    oneVector.set(componentManager.getData(clazz).id());
                }
                oneVectors.add(oneVector);
            }
        }

        BitVector noneVector = null;
        if (!builder.getNone().isEmpty()) {
            noneVector = new BitVector();
            for (var clazz : builder.getNone()) {
                noneVector.set(componentManager.getData(clazz).id());
            }
        }

        return EngineSpec.create(allVector, oneVectors, noneVector);
    }

}
