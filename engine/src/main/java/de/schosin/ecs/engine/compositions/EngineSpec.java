package de.schosin.ecs.engine.compositions;

import java.util.Set;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import de.schosin.ecs.engine.utils.collections.BitVector;

public sealed interface EngineSpec {

    boolean isInterested(BitVector components);

    boolean matches(EngineSpec other);

    public static EngineSpec create(@Nullable BitVector all, @Nullable Set<BitVector> ones, @Nullable BitVector none) {
        if (all != null) {
            if (ones != null) {
                return none != null ? new DefaultCompositionSpec(all, ones, none) : new AllOneCompositionSpec(all, ones);
            }

            return none != null ? new AllNoneCompositionSpec(all, none) : new AllCompositionSpec(all);
        }

        if (ones != null) {
            return none != null ? new OneNoneCompositionSpec(ones, none) : new OneCompositionSpec(ones);
        }

        return none != null ? new NoneCompositionSpec(none) : new EmptyCompositionSpec();
    }

}

@NullMarked
record EmptyCompositionSpec() implements EngineSpec {
    @Override
    public boolean isInterested(BitVector components) {
        return true;
    }

    @Override
    public boolean matches(EngineSpec other) {
        return other instanceof EmptyCompositionSpec;
    }
}

@NullMarked
record AllCompositionSpec(BitVector all) implements EngineSpec {
    @Override
    public boolean isInterested(BitVector components) {
        return components.containsAll(all);
    }

    @Override
    public boolean matches(EngineSpec other) {
        return switch (other) {
            case AllCompositionSpec(var all) -> this.all.containsAll(all);
            default -> false;
        };
    }
}

@NullMarked
record AllOneCompositionSpec(BitVector all, Set<BitVector> ones) implements EngineSpec {
    @Override
    public boolean isInterested(BitVector components) {
        return components.containsAll(all) && ones.stream().allMatch(one -> components.containsSome(one));
    }

    @Override
    public boolean matches(EngineSpec other) {
        return switch (other) {
            case AllCompositionSpec(var all) -> this.all.containsAll(all);
            case OneCompositionSpec(var ones) -> this.ones.stream().allMatch(thisOne -> ones.stream().anyMatch(otherOne -> thisOne.containsAll(otherOne)));
            case AllOneCompositionSpec(var all, var ones) -> this.all.containsAll(all) && this.ones.stream().allMatch(thisOne -> ones.stream().anyMatch(otherOne -> thisOne.containsAll(otherOne)));
            default -> false;
        };
    }
}

@NullMarked
record AllNoneCompositionSpec(BitVector all, BitVector none) implements EngineSpec {
    @Override
    public boolean isInterested(BitVector components) {
        return components.containsAll(all) && components.containsNone(none);
    }

    @Override
    public boolean matches(EngineSpec other) {
        return switch (other) {
            case AllCompositionSpec(var all) -> this.all.containsAll(all);
            case NoneCompositionSpec(var none) -> this.none.containsAll(none);
            case AllNoneCompositionSpec(var all, var none) -> this.all.containsAll(all) && this.none.containsAll(none);
            default -> false;
        };
    }
}

@NullMarked
record OneCompositionSpec(Set<BitVector> ones) implements EngineSpec {
    @Override
    public boolean isInterested(BitVector components) {
        return ones.stream().allMatch(one -> components.containsSome(one));
    }

    @Override
    public boolean matches(EngineSpec other) {
        return switch (other) {
            case OneCompositionSpec(var ones) -> this.ones.stream().allMatch(thisOne -> ones.stream().anyMatch(otherOne -> thisOne.containsAll(otherOne)));
            default -> false;
        };
    }
}

@NullMarked
record NoneCompositionSpec(BitVector none) implements EngineSpec {
    @Override
    public boolean isInterested(BitVector components) {
        return components.containsNone(none);
    }

    @Override
    public boolean matches(EngineSpec other) {
        return switch (other) {
            case NoneCompositionSpec(var none) -> this.none.containsAll(none);
            default -> false;
        };
    }
}

@NullMarked
record OneNoneCompositionSpec(Set<BitVector> ones, BitVector none) implements EngineSpec {
    @Override
    public boolean isInterested(BitVector components) {
        return ones.stream().allMatch(one -> components.containsSome(one)) && components.containsNone(none);
    }

    @Override
    public boolean matches(EngineSpec other) {
        return switch (other) {
            case OneCompositionSpec(var ones) -> this.ones.stream().allMatch(thisOne -> ones.stream().anyMatch(otherOne -> thisOne.containsAll(otherOne)));
            case NoneCompositionSpec(var none) -> this.none.containsAll(none);
            case OneNoneCompositionSpec(var ones, var none) -> this.none.containsAll(none) && this.ones.stream().allMatch(thisOne -> ones.stream().anyMatch(otherOne -> thisOne.containsAll(otherOne)));
            default -> false;
        };
    }
}

@NullMarked
record DefaultCompositionSpec(BitVector all, Set<BitVector> ones, BitVector none) implements EngineSpec {
    @Override
    public boolean isInterested(BitVector components) {
        return components.containsAll(all) && ones.stream().allMatch(one -> components.containsSome(one)) && components.containsNone(none);
    }

    @Override
    public boolean matches(EngineSpec other) {
        return switch (other) {
            case EmptyCompositionSpec() -> false;
            case AllCompositionSpec(var all) -> this.all.containsAll(all);
            case AllOneCompositionSpec(var all, var ones) -> this.all.containsAll(all) && this.ones.stream().allMatch(thisOne -> ones.stream().anyMatch(otherOne -> thisOne.containsAll(otherOne)));
            case AllNoneCompositionSpec(var all, var none) -> this.all.containsAll(all) && this.none.containsAll(none);
            case OneCompositionSpec(var ones) -> this.ones.stream().allMatch(thisOne -> ones.stream().anyMatch(otherOne -> thisOne.containsAll(otherOne)));
            case NoneCompositionSpec(var none) -> this.none.containsAll(none);
            case OneNoneCompositionSpec(var ones, var none) -> this.none.containsAll(none) && this.ones.stream().allMatch(thisOne -> ones.stream().anyMatch(otherOne -> thisOne.containsAll(otherOne)));
            case DefaultCompositionSpec(var all, var ones, var none) -> this.all.containsAll(all) && this.none.containsAll(none)
                    && this.ones.stream().allMatch(thisOne -> ones.stream().anyMatch(otherOne -> thisOne.containsAll(otherOne)));
        };
    }
}
