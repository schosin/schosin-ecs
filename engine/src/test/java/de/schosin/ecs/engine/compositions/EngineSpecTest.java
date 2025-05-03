package de.schosin.ecs.engine.compositions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.engine.utils.collections.BitVector;

class EngineSpecTest {

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
            var spec = assertThat(EngineSpec.create(null, null, null)).isInstanceOf(EmptyCompositionSpec.class).actual();

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

            var spec = assertThat(EngineSpec.create(all, null, null)).isInstanceOf(AllCompositionSpec.class).actual();

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

            var spec = assertThat(EngineSpec.create(all, Set.of(one), null)).isInstanceOf(AllOneCompositionSpec.class).actual();

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

            var spec = assertThat(EngineSpec.create(all, null, none)).isInstanceOf(AllNoneCompositionSpec.class).actual();

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

            var spec = assertThat(EngineSpec.create(all, Set.of(one), none)).isInstanceOf(DefaultCompositionSpec.class).actual();

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

            var spec = assertThat(EngineSpec.create(null, Set.of(one), null)).isInstanceOf(OneCompositionSpec.class).actual();

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

            var spec = assertThat(EngineSpec.create(null, Set.of(one), none)).isInstanceOf(OneNoneCompositionSpec.class).actual();

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

            var spec = assertThat(EngineSpec.create(null, null, none)).isInstanceOf(NoneCompositionSpec.class).actual();

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

        @Test
        void testMultipleOnes() {
            var one = new BitVector();
            one.set(1);
            one.set(2);

            var otherOne = new BitVector();
            otherOne.set(3);
            otherOne.set(4);

            var spec = assertThat(EngineSpec.create(null, Set.of(one, otherOne), null)).isInstanceOf(OneCompositionSpec.class).actual();

            assertThat(spec.isInterested(mask())).isFalse();
            assertThat(spec.isInterested(mask(1))).isFalse();
            assertThat(spec.isInterested(mask(1, 2))).isFalse();
            assertThat(spec.isInterested(mask(1, 2, 3))).isTrue();
            assertThat(spec.isInterested(mask(1, 3))).isTrue();
            assertThat(spec.isInterested(mask(4))).isFalse();
            assertThat(spec.isInterested(mask(1, 2, 4))).isTrue();
            assertThat(spec.isInterested(mask(1, 2, 4, 5))).isTrue();
            assertThat(spec.isInterested(mask(1, 2, 3, 6))).isTrue();
        }

    }

    @Nested
    class MatchesTest {

        static final BitVector all = new BitVector();
        static final BitVector one = new BitVector();
        static final BitVector none = new BitVector();

        static final BitVector matchingAll = new BitVector();
        static final BitVector matchingOne = new BitVector();
        static final BitVector matchingNone = new BitVector();

        static final BitVector mismatchingAll = new BitVector();
        static final BitVector mismatchingOne = new BitVector();
        static final BitVector mismatchingNone = new BitVector();

        @BeforeAll
        static void setup() {
            all.set(1);
            one.set(2);
            none.set(3);

            matchingAll.set(1);
            matchingAll.set(2);

            matchingOne.set(2);
            matchingOne.set(3);

            matchingNone.set(3);
            matchingNone.set(1);

            mismatchingAll.set(2);
            mismatchingAll.set(3);

            mismatchingOne.set(1);
            mismatchingOne.set(3);

            mismatchingNone.set(1);
            mismatchingNone.set(2);
        }

        @Test
        void emptySpec() {
            var spec = EngineSpec.create(null, null, null);

            assertThat(spec.matches(EngineSpec.create(null, null, null))).isTrue();
            assertThat(spec.matches(EngineSpec.create(all, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void allSpec() {
            var spec = EngineSpec.create(matchingAll, null, null);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(EngineSpec.create(null, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, null))).isTrue();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void allSpec_mismatching() {
            var spec = EngineSpec.create(mismatchingAll, null, null);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(EngineSpec.create(null, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void allOneSpec() {
            var spec = EngineSpec.create(matchingAll, Set.of(matchingOne), null);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(EngineSpec.create(null, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, null))).isTrue();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), null))).isTrue();
            assertThat(spec.matches(EngineSpec.create(all, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), null))).isTrue();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void allOneSpec_mismatchingAll() {
            var spec = EngineSpec.create(mismatchingAll, Set.of(matchingOne), null);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(EngineSpec.create(null, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), null))).isTrue();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void allOneSpec_mismatchingOne() {
            var spec = EngineSpec.create(matchingAll, Set.of(mismatchingOne), null);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(EngineSpec.create(null, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, null))).isTrue();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void allNoneSpec() {
            var spec = EngineSpec.create(matchingAll, null, matchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(EngineSpec.create(null, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, null))).isTrue();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, none))).isTrue();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, null, none))).isTrue();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void allNoneSpec_mismatchingAll() {
            var spec = EngineSpec.create(mismatchingAll, null, matchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(EngineSpec.create(null, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, null, none))).isTrue();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void allNoneSpec_mismatchingNone() {
            var spec = EngineSpec.create(matchingAll, null, mismatchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(EngineSpec.create(null, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, null))).isTrue();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void oneSpec() {
            var spec = EngineSpec.create(null, Set.of(matchingOne), null);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(EngineSpec.create(null, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), null))).isTrue();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void oneSpec_mismatching() {
            var spec = EngineSpec.create(null, Set.of(mismatchingOne), null);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(EngineSpec.create(null, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void oneNoneSpec() {
            var spec = EngineSpec.create(null, Set.of(matchingOne), matchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(EngineSpec.create(null, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), null))).isTrue();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), none))).isTrue();
            assertThat(spec.matches(EngineSpec.create(null, null, none))).isTrue();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void oneNoneSpec_mismatchingOne() {
            var spec = EngineSpec.create(null, Set.of(mismatchingOne), matchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(EngineSpec.create(null, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, null, none))).isTrue();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void oneNoneSpec_mismatchingNone() {
            var spec = EngineSpec.create(null, Set.of(matchingOne), mismatchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(EngineSpec.create(null, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), null))).isTrue();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void noneSpec() {
            var spec = EngineSpec.create(null, null, matchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(EngineSpec.create(null, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, null, none))).isTrue();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void noneSpec_mismatching() {
            var spec = EngineSpec.create(null, null, mismatchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(EngineSpec.create(null, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void defaultSpec() {
            var spec = EngineSpec.create(matchingAll, Set.of(matchingOne), matchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(EngineSpec.create(null, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, null))).isTrue();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), null))).isTrue();
            assertThat(spec.matches(EngineSpec.create(all, null, none))).isTrue();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), null))).isTrue();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), none))).isTrue();
            assertThat(spec.matches(EngineSpec.create(null, null, none))).isTrue();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), none))).isTrue();
        }

        @Test
        void defaultSpec_mismatchingAll() {
            var spec = EngineSpec.create(mismatchingAll, Set.of(matchingOne), matchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(EngineSpec.create(null, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), null))).isTrue();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), none))).isTrue();
            assertThat(spec.matches(EngineSpec.create(null, null, none))).isTrue();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void defaultSpec_mismatchingOne() {
            var spec = EngineSpec.create(matchingAll, Set.of(mismatchingOne), matchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(EngineSpec.create(null, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, null))).isTrue();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, none))).isTrue();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, null, none))).isTrue();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void defaultSpec_mismatchingNone() {
            var spec = EngineSpec.create(matchingAll, Set.of(matchingOne), mismatchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(EngineSpec.create(null, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, null))).isTrue();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), null))).isTrue();
            assertThat(spec.matches(EngineSpec.create(all, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), null))).isTrue();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void testMultipleOnes() {
            var one = new BitVector();
            one.set(1);
            one.set(2);

            var otherOne = new BitVector();
            otherOne.set(3);
            otherOne.set(4);

            var overlappingOtherOne = new BitVector();
            overlappingOtherOne.set(4);
            overlappingOtherOne.set(5);

            var onlyThree = new BitVector();
            onlyThree.set(3);

            var spec = assertThat(EngineSpec.create(null, Set.of(one, otherOne), null)).isInstanceOf(OneCompositionSpec.class).actual();
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(EngineSpec.create(null, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, null, none))).isFalse();
            assertThat(spec.matches(EngineSpec.create(all, Set.of(one), none))).isFalse();

            assertThat(spec.matches(EngineSpec.create(null, Set.of(one, otherOne), null))).isTrue();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one, overlappingOtherOne), null))).isFalse();
            assertThat(spec.matches(EngineSpec.create(null, Set.of(one, onlyThree), null))).isTrue();
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
