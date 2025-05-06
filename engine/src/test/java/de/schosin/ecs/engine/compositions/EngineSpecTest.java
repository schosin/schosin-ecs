package de.schosin.ecs.engine.compositions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.engine.utils.collections.BitVector;

public class EngineSpecTest {

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
            var spec = create(null, null, null);

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

            var spec = create(all, null, null);

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

            var spec = create(all, Set.of(one), null);

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

            var spec = create(all, null, none);

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

            var spec = create(all, Set.of(one), none);

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

            var spec = create(null, Set.of(one), null);

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

            var spec = create(null, Set.of(one), none);

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

            var spec = create(null, null, none);

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
            var one = mask(1, 2);
            var otherOne = mask(3, 4);

            var spec = create(null, Set.of(one, otherOne), null);

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
            var spec = create(null, null, null);

            assertThat(spec.matches(create(null, null, null))).isTrue();
            assertThat(spec.matches(create(all, null, null))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(all, null, none))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(create(null, null, none))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void allSpec() {
            var spec = create(matchingAll, null, null);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(create(null, null, null))).isTrue();
            assertThat(spec.matches(create(all, null, null))).isTrue();
            assertThat(spec.matches(create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(all, null, none))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(create(null, null, none))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void allSpec_mismatching() {
            var spec = create(mismatchingAll, null, null);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(create(null, null, null))).isTrue();
            assertThat(spec.matches(create(all, null, null))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(all, null, none))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(create(null, null, none))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void allOneSpec() {
            var spec = create(matchingAll, Set.of(matchingOne), null);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(create(null, null, null))).isTrue();
            assertThat(spec.matches(create(all, null, null))).isTrue();
            assertThat(spec.matches(create(all, Set.of(one), null))).isTrue();
            assertThat(spec.matches(create(all, null, none))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), null))).isTrue();
            assertThat(spec.matches(create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(create(null, null, none))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void allOneSpec_mismatchingAll() {
            var spec = create(mismatchingAll, Set.of(matchingOne), null);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(create(null, null, null))).isTrue();
            assertThat(spec.matches(create(all, null, null))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(all, null, none))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), null))).isTrue();
            assertThat(spec.matches(create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(create(null, null, none))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void allOneSpec_mismatchingOne() {
            var spec = create(matchingAll, Set.of(mismatchingOne), null);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(create(null, null, null))).isTrue();
            assertThat(spec.matches(create(all, null, null))).isTrue();
            assertThat(spec.matches(create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(all, null, none))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(create(null, null, none))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void allNoneSpec() {
            var spec = create(matchingAll, null, matchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(create(null, null, null))).isTrue();
            assertThat(spec.matches(create(all, null, null))).isTrue();
            assertThat(spec.matches(create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(all, null, none))).isTrue();
            assertThat(spec.matches(create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(create(null, null, none))).isTrue();
            assertThat(spec.matches(create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void allNoneSpec_mismatchingAll() {
            var spec = create(mismatchingAll, null, matchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(create(null, null, null))).isTrue();
            assertThat(spec.matches(create(all, null, null))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(all, null, none))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(create(null, null, none))).isTrue();
            assertThat(spec.matches(create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void allNoneSpec_mismatchingNone() {
            var spec = create(matchingAll, null, mismatchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(create(null, null, null))).isTrue();
            assertThat(spec.matches(create(all, null, null))).isTrue();
            assertThat(spec.matches(create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(all, null, none))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(create(null, null, none))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void oneSpec() {
            var spec = create(null, Set.of(matchingOne), null);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(create(null, null, null))).isTrue();
            assertThat(spec.matches(create(all, null, null))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(all, null, none))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), null))).isTrue();
            assertThat(spec.matches(create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(create(null, null, none))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void oneSpec_mismatching() {
            var spec = create(null, Set.of(mismatchingOne), null);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(create(null, null, null))).isTrue();
            assertThat(spec.matches(create(all, null, null))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(all, null, none))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(create(null, null, none))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void oneNoneSpec() {
            var spec = create(null, Set.of(matchingOne), matchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(create(null, null, null))).isTrue();
            assertThat(spec.matches(create(all, null, null))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(all, null, none))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), null))).isTrue();
            assertThat(spec.matches(create(null, Set.of(one), none))).isTrue();
            assertThat(spec.matches(create(null, null, none))).isTrue();
            assertThat(spec.matches(create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void oneNoneSpec_mismatchingOne() {
            var spec = create(null, Set.of(mismatchingOne), matchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(create(null, null, null))).isTrue();
            assertThat(spec.matches(create(all, null, null))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(all, null, none))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(create(null, null, none))).isTrue();
            assertThat(spec.matches(create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void oneNoneSpec_mismatchingNone() {
            var spec = create(null, Set.of(matchingOne), mismatchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(create(null, null, null))).isTrue();
            assertThat(spec.matches(create(all, null, null))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(all, null, none))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), null))).isTrue();
            assertThat(spec.matches(create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(create(null, null, none))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void noneSpec() {
            var spec = create(null, null, matchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(create(null, null, null))).isTrue();
            assertThat(spec.matches(create(all, null, null))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(all, null, none))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(create(null, null, none))).isTrue();
            assertThat(spec.matches(create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void noneSpec_mismatching() {
            var spec = create(null, null, mismatchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(create(null, null, null))).isTrue();
            assertThat(spec.matches(create(all, null, null))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(all, null, none))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(create(null, null, none))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void defaultSpec() {
            var spec = create(matchingAll, Set.of(matchingOne), matchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(create(null, null, null))).isTrue();
            assertThat(spec.matches(create(all, null, null))).isTrue();
            assertThat(spec.matches(create(all, Set.of(one), null))).isTrue();
            assertThat(spec.matches(create(all, null, none))).isTrue();
            assertThat(spec.matches(create(null, Set.of(one), null))).isTrue();
            assertThat(spec.matches(create(null, Set.of(one), none))).isTrue();
            assertThat(spec.matches(create(null, null, none))).isTrue();
            assertThat(spec.matches(create(all, Set.of(one), none))).isTrue();
        }

        @Test
        void defaultSpec_mismatchingAll() {
            var spec = create(mismatchingAll, Set.of(matchingOne), matchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(create(null, null, null))).isTrue();
            assertThat(spec.matches(create(all, null, null))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(all, null, none))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), null))).isTrue();
            assertThat(spec.matches(create(null, Set.of(one), none))).isTrue();
            assertThat(spec.matches(create(null, null, none))).isTrue();
            assertThat(spec.matches(create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void defaultSpec_mismatchingOne() {
            var spec = create(matchingAll, Set.of(mismatchingOne), matchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(create(null, null, null))).isTrue();
            assertThat(spec.matches(create(all, null, null))).isTrue();
            assertThat(spec.matches(create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(all, null, none))).isTrue();
            assertThat(spec.matches(create(null, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(create(null, null, none))).isTrue();
            assertThat(spec.matches(create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void defaultSpec_mismatchingNone() {
            var spec = create(matchingAll, Set.of(matchingOne), mismatchingNone);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(create(null, null, null))).isTrue();
            assertThat(spec.matches(create(all, null, null))).isTrue();
            assertThat(spec.matches(create(all, Set.of(one), null))).isTrue();
            assertThat(spec.matches(create(all, null, none))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), null))).isTrue();
            assertThat(spec.matches(create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(create(null, null, none))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), none))).isFalse();
        }

        @Test
        void testMultipleOnes() {
            var one = mask(1, 2);
            var otherOne = mask(3, 4);
            var overlappingOtherOne = mask(4, 5);
            var onlyThree = mask(3);

            var spec = create(null, Set.of(one, otherOne), null);
            assertThatThrownBy(() -> spec.matches(null)).isInstanceOf(NullPointerException.class);

            assertThat(spec.matches(create(null, null, null))).isTrue();
            assertThat(spec.matches(create(all, null, null))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), null))).isFalse();
            assertThat(spec.matches(create(all, null, none))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one), null))).isTrue();
            assertThat(spec.matches(create(null, Set.of(one), none))).isFalse();
            assertThat(spec.matches(create(null, null, none))).isFalse();
            assertThat(spec.matches(create(all, Set.of(one), none))).isFalse();

            assertThat(spec.matches(create(null, Set.of(one, otherOne), null))).isTrue();
            assertThat(spec.matches(create(null, Set.of(one, overlappingOtherOne), null))).isFalse();
            assertThat(spec.matches(create(null, Set.of(one, onlyThree), null))).isTrue();
        }

    }

    @Nested
    class NestedSpecsTest {

        @Nested
        class NestedAllSpecTest {

            @Test
            void testTwoOneSpecs() {
                var vector1 = mask(1);
                var vector12 = mask(1, 2);
                var vector13 = mask(1, 3);
                var vector123 = mask(1, 2, 3);
                var vector2 = mask(2);
                var vector23 = mask(2, 3);
                var vector3 = mask(3);

                var one12 = new OneSpec(vector12, null);
                var one23 = new OneSpec(vector23, null);

                var spec = new AllSpec(null, Set.of(one12, one23));

                // Verify
                assertThat(spec.isInterested(vector1)).isFalse();
                assertThat(spec.isInterested(vector12)).isTrue();
                assertThat(spec.isInterested(vector13)).isTrue();
                assertThat(spec.isInterested(vector123)).isTrue();
                assertThat(spec.isInterested(vector2)).isTrue();
                assertThat(spec.isInterested(vector23)).isTrue();
                assertThat(spec.isInterested(vector3)).isFalse();
            }

            @Test
            void testTwoNoneSpecs() {
                var vector1 = mask(1);
                var vector12 = mask(1, 2);
                var vector13 = mask(1, 3);
                var vector123 = mask(1, 2, 3);
                var vector2 = mask(2);
                var vector23 = mask(2, 3);
                var vector3 = mask(3);

                var not1 = new NoneSpec(vector1, null);
                var not2 = new NoneSpec(vector2, null);

                var spec = new AllSpec(null, Set.of(not1, not2));

                // Verify
                assertThat(spec.isInterested(vector1)).isFalse();
                assertThat(spec.isInterested(vector12)).isFalse();
                assertThat(spec.isInterested(vector13)).isFalse();
                assertThat(spec.isInterested(vector123)).isFalse();
                assertThat(spec.isInterested(vector2)).isFalse();
                assertThat(spec.isInterested(vector23)).isFalse();
                assertThat(spec.isInterested(vector3)).isTrue();
            }

        }

        @Nested
        class NestedOneSpecTest {

            @Test
            void testTwoAllSpecs() {
                var vector1 = mask(1);
                var vector12 = mask(1, 2);
                var vector13 = mask(1, 3);
                var vector123 = mask(1, 2, 3);
                var vector2 = mask(2);
                var vector23 = mask(2, 3);
                var vector3 = mask(3);

                var one12 = new AllSpec(vector12, null);
                var one23 = new AllSpec(vector23, null);

                var spec = new OneSpec(null, Set.of(one12, one23));

                // Verify
                assertThat(spec.isInterested(vector1)).isFalse();
                assertThat(spec.isInterested(vector12)).isTrue();
                assertThat(spec.isInterested(vector13)).isFalse();
                assertThat(spec.isInterested(vector123)).isTrue();
                assertThat(spec.isInterested(vector2)).isFalse();
                assertThat(spec.isInterested(vector23)).isTrue();
                assertThat(spec.isInterested(vector3)).isFalse();
            }

            @Test
            void testTwoNoneSpecs() {
                var vector1 = mask(1);
                var vector12 = mask(1, 2);
                var vector13 = mask(1, 3);
                var vector123 = mask(1, 2, 3);
                var vector2 = mask(2);
                var vector23 = mask(2, 3);
                var vector3 = mask(3);

                var not1 = new NoneSpec(vector1, null);
                var not2 = new NoneSpec(vector2, null);

                var spec = new OneSpec(null, Set.of(not1, not2));

                // Verify
                assertThat(spec.isInterested(vector1)).isTrue();
                assertThat(spec.isInterested(vector12)).isFalse();
                assertThat(spec.isInterested(vector13)).isTrue();
                assertThat(spec.isInterested(vector123)).isFalse();
                assertThat(spec.isInterested(vector2)).isTrue();
                assertThat(spec.isInterested(vector23)).isTrue();
                assertThat(spec.isInterested(vector3)).isTrue();
            }

        }

        @Nested
        class NestedNoneSpecTest {

            @Test
            void testTwoAllSpecs() {
                var vector1 = mask(1);
                var vector12 = mask(1, 2);
                var vector13 = mask(1, 3);
                var vector123 = mask(1, 2, 3);
                var vector2 = mask(2);
                var vector23 = mask(2, 3);
                var vector3 = mask(3);

                var one12 = new AllSpec(vector12, null);
                var one23 = new AllSpec(vector23, null);

                var spec = new NoneSpec(null, Set.of(one12, one23));

                // Verify
                assertThat(spec.isInterested(vector1)).isTrue();
                assertThat(spec.isInterested(vector12)).isFalse();
                assertThat(spec.isInterested(vector13)).isTrue();
                assertThat(spec.isInterested(vector123)).isFalse();
                assertThat(spec.isInterested(vector2)).isTrue();
                assertThat(spec.isInterested(vector23)).isFalse();
                assertThat(spec.isInterested(vector3)).isTrue();
            }

            @Test
            void testTwoOneSpecs() {
                var vector1 = mask(1);
                var vector12 = mask(1, 2);
                var vector13 = mask(1, 3);
                var vector123 = mask(1, 2, 3);
                var vector2 = mask(2);
                var vector23 = mask(2, 3);
                var vector3 = mask(3);

                var not1 = new OneSpec(vector12, null);
                var not2 = new OneSpec(vector23, null);

                var spec = new NoneSpec(null, Set.of(not1, not2));

                // Verify
                assertThat(spec.isInterested(vector1)).isFalse();
                assertThat(spec.isInterested(vector12)).isFalse();
                assertThat(spec.isInterested(vector13)).isFalse();
                assertThat(spec.isInterested(vector123)).isFalse();
                assertThat(spec.isInterested(vector2)).isFalse();
                assertThat(spec.isInterested(vector23)).isFalse();
                assertThat(spec.isInterested(vector3)).isFalse();
            }

            @Test
            void testTwoLessStrictOneSpecs() {
                var vector1 = mask(1);
                var vector12 = mask(1, 2);
                var vector13 = mask(1, 3);
                var vector123 = mask(1, 2, 3);
                var vector2 = mask(2);
                var vector23 = mask(2, 3);
                var vector3 = mask(3);

                var not1 = new OneSpec(vector1, null);
                var not2 = new OneSpec(vector2, null);

                var spec = new NoneSpec(null, Set.of(not1, not2));

                // Verify
                assertThat(spec.isInterested(vector1)).isFalse();
                assertThat(spec.isInterested(vector12)).isFalse();
                assertThat(spec.isInterested(vector13)).isFalse();
                assertThat(spec.isInterested(vector123)).isFalse();
                assertThat(spec.isInterested(vector2)).isFalse();
                assertThat(spec.isInterested(vector23)).isFalse();
                assertThat(spec.isInterested(vector3)).isTrue();
            }

        }

    }

    public static EngineSpec create(BitVector all, Set<BitVector> ones, BitVector none) {
        if (all == null && ones == null && none == null) {
            return MatchAll.INSTANCE;
        }

        if (all == null && ones == null) {
            return new NoneSpec(none, null);
        }

        if (ones == null && none == null) {
            return new AllSpec(all, null);
        }

        if (all == null && none == null && ones != null && ones.size() == 1) {
            return new OneSpec(ones.iterator().next(), null);
        }

        var oneSpecs = ones != null ? ones.stream().map(one -> new OneSpec(one, null)).collect(Collectors.toSet()) : null;
        return new EngineSpecImpl(new AllSpec(all, null), oneSpecs, new NoneSpec(none, null));
    }

    private BitVector mask(int... set) {
        var mask = new BitVector();
        for (int index : set) {
            mask.set(index);
        }

        return mask;
    }

}
