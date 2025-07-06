package de.schosin.ecs.plugins.composition.manager;

import java.util.Set;
import java.util.stream.Collectors;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.Spec;

public abstract class AbstractSpecManager implements Spec.SpecCreator {

    private interface Creator {
        EngineSpec apply(Set<ComponentType<?, ?>> types, Set<EngineSpec> specs);
    }

    protected final ComponentManager componentManager;
    protected final EntityManager entityManager;

    protected AbstractSpecManager(World world) {
        this.componentManager = world.getSingleton(ComponentManager.class);
        this.entityManager = world.getSingleton(EntityManager.class);
    }

    @Override
    public final Spec createSpec(Composition.Builder builder) {
        var spec = buildSpec(builder);

        return new SpecImpl(spec);
    }

    protected EngineSpec buildSpec(Composition.Builder builder) {
        var all = buildSpec(builder.getAll(), EngineSpec::all);
        var ones = builder.getOnes().isEmpty() ? null : builder.getOnes().stream().map(one -> buildSpec(one, EngineSpec::one)).collect(Collectors.toSet());
        var none = buildSpec(builder.getNone(), EngineSpec::none);

        if (all == null && ones == null && none == null) {
            return EngineSpec.MATCH_ALL;
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

        return EngineSpec.combined(all, ones, none);
    }

    private EngineSpec buildSpec(Composition.Group group, Creator constructor) {
        var components = buildComponents(group);
        var specs = buildSpecs(group);

        return components != null || specs != null
                ? constructor.apply(Set.copyOf(components), specs)
                : null;
    }

    private Set<ComponentType<?, ?>> buildComponents(Composition.Group group) {
        var components = group.components();

        if (components.isEmpty()) {
            return null;
        }

        return Set.copyOf(components);
    }

    private Set<EngineSpec> buildSpecs(Composition.Group group) {
        var builders = group.builders();
        if (builders.isEmpty()) {
            return null;
        }

        return builders.stream()
                .map(this::buildSpec)
                .collect(Collectors.toSet());
    }

    protected class SpecImpl implements Spec {

        protected final EngineSpec spec;

        protected SpecImpl(EngineSpec spec) {
            this.spec = spec;
        }

        @Override
        public boolean isInterested(int entityId) {
            var archetype = entityManager.getArchetype(entityId);

            return spec.isInterested(archetype);
        }

    }

}
