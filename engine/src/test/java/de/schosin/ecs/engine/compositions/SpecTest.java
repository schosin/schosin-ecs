package de.schosin.ecs.engine.compositions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.engine.utils.collections.BitVector;

class SpecTest {

    BitVector all;
    BitVector one;
    BitVector none;

    @BeforeEach
    void setup() {
        this.all = new BitVector();
        this.one = new BitVector();
        this.none = new BitVector();
    }

    @Test
    void testEmpty() {
        var spec = assertThat(Spec.create(null, null, null)).isInstanceOf(EmptyCompositionSpec.class).actual();

        assertThat(spec.isInterested(mask())).isTrue();
        assertThat(spec.isInterested(mask(1))).isTrue();
        assertThat(spec.isInterested(mask(1, 2))).isTrue();
        assertThat(spec.isInterested(mask(1, 2, 3))).isTrue();
        assertThat(spec.isInterested(mask(1, 3))).isTrue();
    }

    @Test
    void testAll() {
        all.set(1);
        all.set(2);

        var spec = assertThat(Spec.create(all, null, null)).isInstanceOf(AllCompositionSpec.class).actual();

        assertThat(spec.isInterested(mask())).isFalse();
        assertThat(spec.isInterested(mask(1))).isFalse();
        assertThat(spec.isInterested(mask(1, 2))).isTrue();
        assertThat(spec.isInterested(mask(1, 2, 3))).isTrue();
        assertThat(spec.isInterested(mask(1, 3))).isFalse();
    }

    @Test
    void testAllOne() {
        all.set(1);
        all.set(2);

        one.set(3);
        one.set(4);

        var spec = assertThat(Spec.create(all, one, null)).isInstanceOf(AllOneCompositionSpec.class).actual();

        assertThat(spec.isInterested(mask())).isFalse();
        assertThat(spec.isInterested(mask(1))).isFalse();
        assertThat(spec.isInterested(mask(1, 2))).isFalse();
        assertThat(spec.isInterested(mask(1, 2, 3))).isTrue();
        assertThat(spec.isInterested(mask(1, 3))).isFalse();
        assertThat(spec.isInterested(mask(4))).isFalse();
        assertThat(spec.isInterested(mask(1, 2, 4))).isTrue();
    }

    @Test
    void testAllNone() {
        all.set(1);
        all.set(2);

        none.set(5);
        none.set(6);

        var spec = assertThat(Spec.create(all, null, none)).isInstanceOf(AllNoneCompositionSpec.class).actual();

        assertThat(spec.isInterested(mask())).isFalse();
        assertThat(spec.isInterested(mask(1))).isFalse();
        assertThat(spec.isInterested(mask(1, 2))).isTrue();
        assertThat(spec.isInterested(mask(1, 2, 3))).isTrue();
        assertThat(spec.isInterested(mask(1, 3))).isFalse();
        assertThat(spec.isInterested(mask(4))).isFalse();
        assertThat(spec.isInterested(mask(1, 2, 4))).isTrue();
        assertThat(spec.isInterested(mask(1, 2, 4, 5))).isFalse();
        assertThat(spec.isInterested(mask(1, 2, 3, 6))).isFalse();
    }

    @Test
    void testDefault() {
        all.set(1);
        all.set(2);

        one.set(3);
        one.set(4);

        none.set(5);
        none.set(6);

        var spec = assertThat(Spec.create(all, one, none)).isInstanceOf(DefaultCompositionSpec.class).actual();

        assertThat(spec.isInterested(mask())).isFalse();
        assertThat(spec.isInterested(mask(1))).isFalse();
        assertThat(spec.isInterested(mask(1, 2))).isFalse();
        assertThat(spec.isInterested(mask(1, 2, 3))).isTrue();
        assertThat(spec.isInterested(mask(1, 3))).isFalse();
        assertThat(spec.isInterested(mask(4))).isFalse();
        assertThat(spec.isInterested(mask(1, 2, 4))).isTrue();
        assertThat(spec.isInterested(mask(1, 2, 4, 5))).isFalse();
        assertThat(spec.isInterested(mask(1, 2, 3, 6))).isFalse();
    }

    @Test
    void testOne() {
        one.set(3);
        one.set(4);

        var spec = assertThat(Spec.create(null, one, null)).isInstanceOf(OneCompositionSpec.class).actual();

        assertThat(spec.isInterested(mask())).isFalse();
        assertThat(spec.isInterested(mask(1))).isFalse();
        assertThat(spec.isInterested(mask(1, 2))).isFalse();
        assertThat(spec.isInterested(mask(1, 2, 3))).isTrue();
        assertThat(spec.isInterested(mask(1, 3))).isTrue();
        assertThat(spec.isInterested(mask(4))).isTrue();
        assertThat(spec.isInterested(mask(1, 2, 4))).isTrue();
        assertThat(spec.isInterested(mask(1, 2, 4, 5))).isTrue();
        assertThat(spec.isInterested(mask(1, 2, 3, 6))).isTrue();
    }

    @Test
    void testOneNone() {
        one.set(3);
        one.set(4);

        none.set(5);
        none.set(6);

        var spec = assertThat(Spec.create(null, one, none)).isInstanceOf(OneNoneCompositionSpec.class).actual();

        assertThat(spec.isInterested(mask())).isFalse();
        assertThat(spec.isInterested(mask(1))).isFalse();
        assertThat(spec.isInterested(mask(1, 2))).isFalse();
        assertThat(spec.isInterested(mask(1, 2, 3))).isTrue();
        assertThat(spec.isInterested(mask(1, 3))).isTrue();
        assertThat(spec.isInterested(mask(4))).isTrue();
        assertThat(spec.isInterested(mask(1, 2, 4))).isTrue();
        assertThat(spec.isInterested(mask(1, 2, 4, 5))).isFalse();
        assertThat(spec.isInterested(mask(1, 2, 3, 6))).isFalse();
    }

    @Test
    void testNone() {
        none.set(5);
        none.set(6);

        var spec = assertThat(Spec.create(null, null, none)).isInstanceOf(NoneCompositionSpec.class).actual();

        assertThat(spec.isInterested(mask())).isTrue();
        assertThat(spec.isInterested(mask(1))).isTrue();
        assertThat(spec.isInterested(mask(1, 2))).isTrue();
        assertThat(spec.isInterested(mask(1, 2, 3))).isTrue();
        assertThat(spec.isInterested(mask(1, 3))).isTrue();
        assertThat(spec.isInterested(mask(4))).isTrue();
        assertThat(spec.isInterested(mask(1, 2, 4))).isTrue();
        assertThat(spec.isInterested(mask(1, 2, 4, 5))).isFalse();
        assertThat(spec.isInterested(mask(1, 2, 3, 6))).isFalse();
    }

    private BitVector mask(int... set) {
        var mask = new BitVector();
        for (int index : set) {
            mask.set(index);
        }

        return mask;
    }

}
