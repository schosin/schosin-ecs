package de.schosin.ecs.engine.compositions;

import static de.schosin.ecs.engine.compositions.Helper.matchesComponents;
import static de.schosin.ecs.engine.compositions.Helper.matchesSpec;

import java.util.Objects;
import java.util.Set;

import de.schosin.ecs.engine.utils.collections.BitVector;

public sealed interface EngineSpec {

    boolean isInterested(BitVector components);

    boolean matches(EngineSpec other);

    boolean equals(Object o);

    int hashCode();

}

record AllSpec(BitVector all, Set<EngineSpec> specs) implements EngineSpec {

    @Override
    public boolean isInterested(BitVector components) {
        return (all == null || components.containsAll(all))
                && (specs == null || specs.stream().allMatch(spec -> spec.isInterested(components)));
    }

    @Override
    public boolean matches(EngineSpec other) {
        return switch (other) {
            case AllSpec(BitVector otherAll, Set<EngineSpec> otherSpecs) -> matchesComponents(all, otherAll) && matchesSpec(specs, otherSpecs);
            case OneSpec(BitVector otherOne, Set<EngineSpec> otherSpecs) -> false;
            case NoneSpec(BitVector otherNone, Set<EngineSpec> otherSpecs) -> false;
            case MatchAll m -> true;
            case EngineSpecImpl e -> false;
        };
    }

}

record OneSpec(BitVector one, Set<EngineSpec> specs) implements EngineSpec {

    @Override
    public boolean isInterested(BitVector components) {
        return (one != null && components.containsSome(one)) || (specs != null && specs.stream().anyMatch(spec -> spec.isInterested(components)));
    }

    @Override
    @SuppressWarnings("null") // JDT bug 
    public boolean matches(EngineSpec other) {
        return switch (other) {
            case AllSpec(BitVector otherAll, Set<EngineSpec> otherSpecs) -> false;
            case OneSpec(BitVector otherOne, Set<EngineSpec> otherSpecs) -> (otherOne != null && matchesComponents(one, otherOne)) || (otherSpecs != null && matchesSpec(specs, otherSpecs));
            case NoneSpec(BitVector otherNone, Set<EngineSpec> otherSpecs) -> false;
            case MatchAll m -> true;
            case EngineSpecImpl e -> false;
        };
    }
}

record NoneSpec(BitVector none, Set<EngineSpec> specs) implements EngineSpec {

    @Override
    public boolean isInterested(BitVector components) {
        return (none == null || components.containsNone(none))
                && (specs == null || specs.stream().noneMatch(spec -> spec.isInterested(components)));
    }

    @Override
    public boolean matches(EngineSpec other) {
        return switch (other) {
            case AllSpec(BitVector otherAll, Set<EngineSpec> otherSpecs) -> false;
            case OneSpec(BitVector otherOne, Set<EngineSpec> otherSpecs) -> false;
            case NoneSpec(BitVector otherNone, Set<EngineSpec> otherSpecs) -> matchesComponents(none, otherNone) && matchesSpec(specs, otherSpecs);
            case MatchAll m -> true;
            case EngineSpecImpl e -> false;
        };
    }
}

enum MatchAll implements EngineSpec {

    INSTANCE;

    @Override
    public boolean isInterested(BitVector components) {
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
        this.all = all;
        this.ones = ones;
        this.none = none;
    }

    @Override
    public boolean isInterested(BitVector components) {
        return (all == null || all.isInterested(components))
                && (ones == null || ones.stream().allMatch(one -> one.isInterested(components)))
                && (none == null || none.isInterested(components));
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

    static boolean matchesComponents(BitVector components, BitVector otherComponents) {
        return otherComponents == null || (components != null && components.containsAll(otherComponents));
    }

    static boolean matchesSpec(Set<EngineSpec> specs, Set<EngineSpec> otherSpecs) {
        return otherSpecs == null || (specs != null && otherSpecs.stream().allMatch(otherSpec -> specs.stream().anyMatch(spec -> spec.matches(otherSpec))));
    }

}