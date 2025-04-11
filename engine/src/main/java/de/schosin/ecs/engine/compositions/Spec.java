package de.schosin.ecs.engine.compositions;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import de.schosin.ecs.engine.utils.collections.BitVector;

public sealed interface Spec {

    boolean isInterested(BitVector components);

    static Spec create(@Nullable BitVector all, @Nullable BitVector one, @Nullable BitVector none) {
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
record EmptyCompositionSpec() implements Spec {
    @Override
    public boolean isInterested(BitVector components) {
        return true;
    }
}

@NullMarked
record AllCompositionSpec(BitVector all) implements Spec {
    @Override
    public boolean isInterested(BitVector components) {
        return all.containsAll(components);
    }
}

@NullMarked
record AllOneCompositionSpec(BitVector all, BitVector one) implements Spec {
    @Override
    public boolean isInterested(BitVector components) {
        return all.containsAll(components) && one.containsSome(components);
    }
}

@NullMarked
record AllNoneCompositionSpec(BitVector all, BitVector none) implements Spec {
    @Override
    public boolean isInterested(BitVector components) {
        return all.containsAll(components) && none.containsNone(components);
    }
}

@NullMarked
record OneCompositionSpec(BitVector one) implements Spec {
    @Override
    public boolean isInterested(BitVector components) {
        return one.containsSome(components);
    }
}

@NullMarked
record NoneCompositionSpec(BitVector none) implements Spec {
    @Override
    public boolean isInterested(BitVector components) {
        return none.containsNone(components);
    }
}

@NullMarked
record OneNoneCompositionSpec(BitVector one, BitVector none) implements Spec {
    @Override
    public boolean isInterested(BitVector components) {
        return one.containsSome(components) && none.containsNone(components);
    }
}

@NullMarked
record DefaultCompositionSpec(BitVector all, BitVector one, BitVector none) implements Spec {
    @Override
    public boolean isInterested(BitVector components) {
        return all.containsAll(components) && one.containsSome(components) && none.containsNone(components);
    }
}
