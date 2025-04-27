package de.schosin.ecs.engine.compositions;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import de.schosin.ecs.engine.utils.collections.BitVector;

public sealed interface EngineSpec {

    boolean isInterested(BitVector components);

    boolean matches(EngineSpec other);

    public static EngineSpec create(@Nullable BitVector all, @Nullable BitVector one, @Nullable BitVector none) {
        if (all != null) {
            if (one != null) {
                return none != null ? new DefaultCompositionSpec(all, one, none) : new AllOneCompositionSpec(all, one);
            }

            return none != null ? new AllNoneCompositionSpec(all, none) : new AllCompositionSpec(all);
        }

        if (one != null) {
            return none != null ? new OneNoneCompositionSpec(one, none) : new OneCompositionSpec(one);
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
record AllOneCompositionSpec(BitVector all, BitVector one) implements EngineSpec {
    @Override
    public boolean isInterested(BitVector components) {
        return components.containsAll(all) && components.containsSome(one);
    }

    @Override
    public boolean matches(EngineSpec other) {
        return switch (other) {
            case AllCompositionSpec(var all) -> this.all.containsAll(all);
            case OneCompositionSpec(var one) -> this.one.containsAll(one);
            case AllOneCompositionSpec(var all, var one) -> this.all.containsAll(all) && this.one.containsAll(one);
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
record OneCompositionSpec(BitVector one) implements EngineSpec {
    @Override
    public boolean isInterested(BitVector components) {
        return components.containsSome(one);
    }

    @Override
    public boolean matches(EngineSpec other) {
        return switch (other) {
            case OneCompositionSpec(var one) -> this.one.containsAll(one);
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
record OneNoneCompositionSpec(BitVector one, BitVector none) implements EngineSpec {
    @Override
    public boolean isInterested(BitVector components) {
        return components.containsSome(one) && components.containsNone(none);
    }

    @Override
    public boolean matches(EngineSpec other) {
        return switch (other) {
            case OneCompositionSpec(var one) -> this.one.containsAll(one);
            case NoneCompositionSpec(var none) -> this.none.containsAll(none);
            case OneNoneCompositionSpec(var one, var none) -> this.one.containsAll(one) && this.none.containsAll(none);
            default -> false;
        };
    }
}

@NullMarked
record DefaultCompositionSpec(BitVector all, BitVector one, BitVector none) implements EngineSpec {
    @Override
    public boolean isInterested(BitVector components) {
        return components.containsAll(all) && components.containsSome(one) && components.containsNone(none);
    }

    @Override
    public boolean matches(EngineSpec other) {
        return switch (other) {
            case EmptyCompositionSpec() -> false;
            case AllCompositionSpec(var all) -> this.all.containsAll(all);
            case AllOneCompositionSpec(var all, var one) -> this.all.containsAll(all) && this.one.containsAll(one);
            case AllNoneCompositionSpec(var all, var none) -> this.all.containsAll(all) && this.none.containsAll(none);
            case OneCompositionSpec(var one) -> this.one.containsAll(one);
            case NoneCompositionSpec(var none) -> this.none.containsAll(none);
            case OneNoneCompositionSpec(var one, var none) -> this.one.containsAll(one) && this.none.containsAll(none);
            case DefaultCompositionSpec(var all, var one, var none) -> this.all.containsAll(all) && this.one.containsAll(one) && this.none.containsAll(none);
        };
    }
}
