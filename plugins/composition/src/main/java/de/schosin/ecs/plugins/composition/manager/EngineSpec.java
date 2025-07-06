package de.schosin.ecs.plugins.composition.manager;

import static de.schosin.ecs.plugins.composition.manager.Helper.matchesComponents;
import static de.schosin.ecs.plugins.composition.manager.Helper.matchesSpec;

import java.util.Objects;
import java.util.Set;

import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.storage.api.entities.Archetype;

public sealed interface EngineSpec extends EntityManager.ComponentsPredicate {

    static EngineSpec MATCH_ALL = MatchAll.INSTANCE;

    static EngineSpec all(Set<ComponentType<?, ?>> types, Set<EngineSpec> specs) {
        return new AllSpec(types, specs);
    }

    static EngineSpec one(Set<ComponentType<?, ?>> types, Set<EngineSpec> specs) {
        return new OneSpec(types, specs);
    }

    static EngineSpec none(Set<ComponentType<?, ?>> types, Set<EngineSpec> specs) {
        return new NoneSpec(types, specs);
    }

    static EngineSpec combined(EngineSpec all, Set<EngineSpec> ones, EngineSpec none) {
        return new EngineSpecImpl(all, ones, none);
    }

    boolean matches(EngineSpec other);

    boolean equals(Object o);

    int hashCode();

}

record AllSpec(Set<ComponentType<?, ?>> types, Set<EngineSpec> specs) implements EngineSpec {

    AllSpec {
        if ((types == null || types.isEmpty()) && (specs == null || specs.isEmpty())) {
            throw new IllegalArgumentException("Atleast one argument must not be null or empty");
        }
    }

    @Override
    public boolean isInterested(Archetype archetype) {
        return (types == null || types.stream().allMatch(type -> archetype.getComponentTypes().stream().anyMatch(otherType -> type.matches(otherType))))
                && (specs == null || specs.stream().allMatch(spec -> spec.isInterested(archetype)));
    }

    @Override
    public boolean matches(EngineSpec other) {
        return switch (other) {
            case AllSpec(var otherTypes, var otherSpecs) -> matchesComponents(types, otherTypes) && matchesSpec(specs, otherSpecs);
            case OneSpec(var otherTypes, var otherSpecs) -> false;
            case NoneSpec(var otherTypes, var otherSpecs) -> false;
            case MatchAll m -> true;
            case EngineSpecImpl e -> false;
        };
    }

}

record OneSpec(Set<ComponentType<?, ?>> types, Set<EngineSpec> specs) implements EngineSpec {

    OneSpec {
        if ((types == null || types.isEmpty()) && (specs == null || specs.isEmpty())) {
            throw new IllegalArgumentException("Atleast one argument must not be null or empty");
        }
    }

    @Override
    public boolean isInterested(Archetype archetype) {
        return (types != null && types.stream().anyMatch(type -> archetype.getComponentTypes().stream().anyMatch(otherType -> type.matches(otherType))))
                || (specs != null && specs.stream().anyMatch(spec -> spec.isInterested(archetype)));
    }

    @Override
    @SuppressWarnings("null") // JDT bug 
    public boolean matches(EngineSpec other) {
        return switch (other) {
            case AllSpec(var otherTypes, var otherSpecs) -> false;
            case OneSpec(var otherTypes, var otherSpecs) -> (otherTypes != null && matchesComponents(types, otherTypes)) || (otherSpecs != null && matchesSpec(specs, otherSpecs));
            case NoneSpec(var otherTypes, var otherSpecs) -> false;
            case MatchAll m -> true;
            case EngineSpecImpl e -> false;
        };
    }
}

record NoneSpec(Set<ComponentType<?, ?>> types, Set<EngineSpec> specs) implements EngineSpec {

    NoneSpec {
        if ((types == null || types.isEmpty()) && (specs == null || specs.isEmpty())) {
            throw new IllegalArgumentException("Atleast one argument must not be null or empty");
        }
    }

    @Override
    public boolean isInterested(Archetype archetype) {
        return (types == null || types.stream().noneMatch(type -> archetype.getComponentTypes().stream().anyMatch(otherType -> type.matches(otherType))))
                && (specs == null || specs.stream().noneMatch(spec -> spec.isInterested(archetype)));
    }

    @Override
    public boolean matches(EngineSpec other) {
        return switch (other) {
            case AllSpec(var otherTypes, var otherSpecs) -> false;
            case OneSpec(var otherTypes, var otherSpecs) -> false;
            case NoneSpec(var otherTypes, var otherSpecs) -> matchesComponents(types, otherTypes) && matchesSpec(specs, otherSpecs);
            case MatchAll m -> true;
            case EngineSpecImpl e -> false;
        };
    }
}

enum MatchAll implements EngineSpec {

    INSTANCE;

    @Override
    public boolean isInterested(Archetype archetype) {
        return true;
    }

    @Override
    public boolean matches(EngineSpec other) {
        return other == MatchAll.INSTANCE;
    }

}

final class EngineSpecImpl implements EngineSpec {

    private final EngineSpec all;
    private final Set<? extends EngineSpec> ones;
    private final EngineSpec none;

    EngineSpecImpl(EngineSpec all, Set<? extends EngineSpec> ones, EngineSpec none) {
        if (all == null && (ones == null || ones.isEmpty()) && none == null) {
            throw new IllegalArgumentException("Atleast one argument must not be null or empty");
        }

        this.all = all;
        this.ones = ones;
        this.none = none;
    }

    @Override
    public boolean isInterested(Archetype archetype) {
        return (all == null || all.isInterested(archetype))
                && (ones == null || ones.stream().allMatch(one -> one.isInterested(archetype)))
                && (none == null || none.isInterested(archetype));
    }

    @Override
    public boolean matches(EngineSpec other) {
        return switch (other) {
            case AllSpec allSpec -> this.all != null && this.all.matches(allSpec);
            case OneSpec oneSpec -> this.ones != null && this.ones.stream().anyMatch(one -> one.matches(oneSpec));
            case NoneSpec noneSpec -> this.none != null && this.none.matches(noneSpec);
            case MatchAll m -> true;
            case EngineSpecImpl e -> (e.all == null || (all != null && all.matches(e.all)))
                    && (e.ones == null || (this.ones != null && e.ones.stream().allMatch(otherOne -> this.ones.stream().anyMatch(one -> one.matches(otherOne)))))
                    && (e.none == null || (none != null && none.matches(e.none)));
        };
    }

    @Override
    public int hashCode() {
        return Objects.hash(all, none, ones);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EngineSpecImpl other = (EngineSpecImpl) obj;
        return Objects.equals(this.all, other.all) && Objects.equals(this.none, other.none) && Objects.equals(this.ones, other.ones);
    }

}

class Helper {

    static boolean matchesComponents(Set<ComponentType<?, ?>> types, Set<ComponentType<?, ?>> otherTypes) {
        return otherTypes == null || (types != null && otherTypes.stream().allMatch(otherType -> types.stream().anyMatch(type -> switch (type) {
            case RegularComponentType<?, ?> regular -> otherType.matches(regular);
            default -> otherType.equals(type);
        })));
    }

    static boolean matchesSpec(Set<EngineSpec> specs, Set<EngineSpec> otherSpecs) {
        return otherSpecs == null || (specs != null && otherSpecs.stream().allMatch(otherSpec -> specs.stream().anyMatch(spec -> spec.matches(otherSpec))));
    }

}