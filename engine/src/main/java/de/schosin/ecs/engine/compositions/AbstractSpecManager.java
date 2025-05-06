package de.schosin.ecs.engine.compositions;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import de.schosin.ecs.api.components.Composition;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.utils.collections.BitVector;

public abstract class AbstractSpecManager {

    private final ComponentManager componentManager;

    protected AbstractSpecManager(ComponentManager componentManager) {
        this.componentManager = componentManager;
    }

    protected EngineSpec buildSpec(Composition.Builder builder) {
        var all = buildSpec(builder.getAll(), AllSpec::new);
        var ones = builder.getOnes().isEmpty() ? null : builder.getOnes().stream().map(one -> buildSpec(one, OneSpec::new)).collect(Collectors.toSet());
        var none = buildSpec(builder.getNone(), NoneSpec::new);

        if (all == null && ones == null && none == null) {
            return MatchAll.INSTANCE;
        }

        if (all == null && ones == null) {
            return none;
        }

        if (ones == null && none == null) {
            return all;
        }

        if (all == null && none == null && ones != null && ones.size() == 1) {
            return ones.iterator().next();
        }

        return new EngineSpecImpl(all, ones, none);
    }

    private EngineSpec buildSpec(Composition.Builder.Group group, BiFunction<BitVector, Set<EngineSpec>, EngineSpec> constructor) {
        var components = buildComponents(group);
        var specs = buildSpecs(group);

        return components != null || specs != null
                ? constructor.apply(components, specs)
                : null;
    }

    private BitVector buildComponents(Composition.Builder.Group group) {
        var classes = group.classes();

        if (classes.isEmpty()) {
            return null;
        }

        var vector = new BitVector(classes.size());
        for (var clazz : classes) {
            var componentId = componentManager.getData(clazz).id();
            vector.set(componentId);
        }

        return vector;
    }

    private Set<EngineSpec> buildSpecs(Composition.Builder.Group group) {
        var builders = group.builders();
        if (builders.isEmpty()) {
            return null;
        }

        return builders.stream()
                .map(this::buildSpec)
                .collect(Collectors.toSet());
    }

}
