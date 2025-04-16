package de.schosin.ecs.engine.compositions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

    @Nested
    class IsInterestedTest {

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

    }

    @Nested
    class MatchesTest {

        BitVector matchingAll;
        BitVector matchingOne;
        BitVector matchingNone;

        BitVector mismatchingAll;
        BitVector mismatchingOne;
        BitVector mismatchingNone;

        @BeforeEach
        void setup() {
            all.set(1);
            one.set(2);
            none.set(3);

            this.matchingAll = new BitVector();
            matchingAll.set(1);
            matchingAll.set(2);

            this.matchingOne = new BitVector();
            matchingOne.set(2);
            matchingOne.set(3);

            this.matchingNone = new BitVector();
            matchingNone.set(3);
            matchingNone.set(1);

            this.mismatchingAll = new BitVector();
            mismatchingAll.set(2);
            mismatchingAll.set(3);

            this.mismatchingOne = new BitVector();
            mismatchingOne.set(1);
            mismatchingOne.set(3);

            this.mismatchingNone = new BitVector();
            mismatchingNone.set(1);
            mismatchingNone.set(2);
        }

        @Test
        void emptySpec() {
            var spec = Spec.create(null, null, null);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(Spec.create(null, null, null))).isTrue();
            assertThat(spec.matches(Spec.create(all, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(all, one, null))).isFalse();
            assertThat(spec.matches(Spec.create(all, null, none))).isFalse();
            assertThat(spec.matches(Spec.create(null, one, null))).isFalse();
            assertThat(spec.matches(Spec.create(null, one, none))).isFalse();
            assertThat(spec.matches(Spec.create(null, null, none))).isFalse();
            assertThat(spec.matches(Spec.create(all, one, none))).isFalse();
        }

        @Test
        void allSpec() {
            var spec = Spec.create(all, null, null);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(Spec.create(null, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, null, null))).isTrue();
            assertThat(spec.matches(Spec.create(matchingAll, matchingOne, null))).isTrue();
            assertThat(spec.matches(Spec.create(matchingAll, null, matchingNone))).isTrue();
            assertThat(spec.matches(Spec.create(null, matchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(null, matchingOne, matchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(null, null, matchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, matchingOne, matchingNone))).isTrue();

            assertThat(spec.matches(Spec.create(null, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, mismatchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, null, mismatchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(null, mismatchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(null, mismatchingOne, mismatchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(null, null, mismatchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, mismatchingOne, mismatchingNone))).isFalse();
        }

        @Test
        void allOneSpec() {
            var spec = Spec.create(all, one, null);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(Spec.create(null, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, matchingOne, null))).isTrue();
            assertThat(spec.matches(Spec.create(matchingAll, null, matchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(null, matchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(null, matchingOne, matchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(null, null, matchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, matchingOne, matchingNone))).isTrue();

            assertThat(spec.matches(Spec.create(null, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, mismatchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, null, mismatchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(null, mismatchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(null, mismatchingOne, mismatchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(null, null, mismatchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, mismatchingOne, mismatchingNone))).isFalse();
        }

        @Test
        void allNoneSpec() {
            var spec = Spec.create(all, null, none);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(Spec.create(null, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, matchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, null, matchingNone))).isTrue();
            assertThat(spec.matches(Spec.create(null, matchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(null, matchingOne, matchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(null, null, matchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, matchingOne, matchingNone))).isTrue();

            assertThat(spec.matches(Spec.create(null, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, mismatchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, null, mismatchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(null, mismatchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(null, mismatchingOne, mismatchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(null, null, mismatchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, mismatchingOne, mismatchingNone))).isFalse();
        }

        @Test
        void oneSpec() {
            var spec = Spec.create(null, one, null);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(Spec.create(null, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, matchingOne, null))).isTrue();
            assertThat(spec.matches(Spec.create(matchingAll, null, matchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(null, matchingOne, null))).isTrue();
            assertThat(spec.matches(Spec.create(null, matchingOne, matchingNone))).isTrue();
            assertThat(spec.matches(Spec.create(null, null, matchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, matchingOne, matchingNone))).isTrue();

            assertThat(spec.matches(Spec.create(null, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, mismatchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, null, mismatchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(null, mismatchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(null, mismatchingOne, mismatchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(null, null, mismatchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, mismatchingOne, mismatchingNone))).isFalse();
        }

        @Test
        void oneNoneSpec() {
            var spec = Spec.create(null, one, none);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(Spec.create(null, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, matchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, null, matchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(null, matchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(null, matchingOne, matchingNone))).isTrue();
            assertThat(spec.matches(Spec.create(null, null, matchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, matchingOne, matchingNone))).isTrue();

            assertThat(spec.matches(Spec.create(null, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, mismatchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, null, mismatchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(null, mismatchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(null, mismatchingOne, mismatchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(null, null, mismatchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, mismatchingOne, mismatchingNone))).isFalse();
        }

        @Test
        void noneSpec() {
            var spec = Spec.create(null, null, none);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(Spec.create(null, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, matchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, null, matchingNone))).isTrue();
            assertThat(spec.matches(Spec.create(null, matchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(null, matchingOne, matchingNone))).isTrue();
            assertThat(spec.matches(Spec.create(null, null, matchingNone))).isTrue();
            assertThat(spec.matches(Spec.create(matchingAll, matchingOne, matchingNone))).isTrue();

            assertThat(spec.matches(Spec.create(null, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, mismatchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, null, mismatchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(null, mismatchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(null, mismatchingOne, mismatchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(null, null, mismatchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, mismatchingOne, mismatchingNone))).isFalse();
        }

        @Test
        void defaultSpec() {
            var spec = Spec.create(all, one, none);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(Spec.create(null, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, matchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, null, matchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(null, matchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(null, matchingOne, matchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(null, null, matchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(matchingAll, matchingOne, matchingNone))).isTrue();

            assertThat(spec.matches(Spec.create(null, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, null, null))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, mismatchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, null, mismatchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(null, mismatchingOne, null))).isFalse();
            assertThat(spec.matches(Spec.create(null, mismatchingOne, mismatchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(null, null, mismatchingNone))).isFalse();
            assertThat(spec.matches(Spec.create(mismatchingAll, mismatchingOne, mismatchingNone))).isFalse();
        }

    }

    private BitVector mask(int... set) {
        var mask = new BitVector();
        for (int index : set) {
            mask.set(index);
        }

        return mask;
    }

}
