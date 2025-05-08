package de.schosin.ecs.engine.compositions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
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
        void testAll_ClassesAndSpecs() {
            var nested = new OneSpec(mask(3, 4), null);
            var spec = new AllSpec(mask(1, 2), Set.of(nested));

            assertThat(spec.isInterested(mask())).isFalse();
            assertThat(spec.isInterested(mask(1))).isFalse();
            assertThat(spec.isInterested(mask(1, 2))).isFalse();
            assertThat(spec.isInterested(mask(1, 2, 3))).isTrue();
            assertThat(spec.isInterested(mask(1, 3))).isFalse();
            assertThat(spec.isInterested(mask(4))).isFalse();
            assertThat(spec.isInterested(mask(1, 2, 4))).isTrue();
            assertThat(spec.isInterested(mask(1, 2, 4, 5))).isTrue();
            assertThat(spec.isInterested(mask(1, 2, 3, 6))).isTrue();
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
        void testAllOne_ClassesAndSpecs() {
            var nestedAll = new OneSpec(mask(3, 4), null);
            var all = new AllSpec(mask(1, 2), Set.of(nestedAll));

            var nestedOne = new AllSpec(mask(7, 8), null);
            var one = new OneSpec(mask(5, 6), Set.of(nestedOne));

            var spec = new EngineSpecImpl(all, Set.of(one), null);

            assertThat(spec.isInterested(mask())).isFalse();

            var allMatches = List.of(mask(1, 2, 3), mask(1, 2, 4), mask(1, 2, 3, 4));
            var allMismatches = List.of(mask(3), mask(4), mask(1, 3), mask(1, 4), mask(2, 3), mask(2, 4), mask(1, 2));

            var oneMatches = List.of(mask(5), mask(6), mask(7, 8));
            var oneMismatches = List.of(mask(7), mask(8));

            allMatches.forEach(mask -> assertThat(spec.isInterested(mask)).as("allMatch(%s)", mask).isFalse());
            allMismatches.forEach(mask -> assertThat(spec.isInterested(mask)).as("allMismatch(%s)", mask).isFalse());

            oneMatches.forEach(mask -> assertThat(spec.isInterested(mask)).as("oneMatch(%s)", mask).isFalse());
            oneMismatches.forEach(mask -> assertThat(spec.isInterested(mask)).as("oneMismatch(%s)", mask).isFalse());

            allMatches.forEach(a -> oneMatches.forEach(o -> assertThat(spec.isInterested(merge(a, o))).as("allMatch(%s) | oneMatch(%s): ", a, o).isTrue()));
            allMatches.forEach(a -> oneMismatches.forEach(o -> assertThat(spec.isInterested(merge(a, o))).as("allMatch(%s) | oneMismatch(%s): ", a, o).isFalse()));

            allMismatches.forEach(a -> oneMatches.forEach(o -> assertThat(spec.isInterested(merge(a, o))).as("allMismatch(%s) | oneMatch(%s): ", a, o).isFalse()));
            allMismatches.forEach(a -> oneMismatches.forEach(o -> assertThat(spec.isInterested(merge(a, o))).as("allMismatch(%s) | oneMismatch(%s): ", a, o).isFalse()));
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
        void testAllNone_ClassesAndSpecs() {
            var nestedAll = new OneSpec(mask(3, 4), null);
            var all = new AllSpec(mask(1, 2), Set.of(nestedAll));

            var nestedNone = new AllSpec(mask(7, 8), null);
            var none = new NoneSpec(mask(5, 6), Set.of(nestedNone));

            var spec = new EngineSpecImpl(all, null, none);

            assertThat(spec.isInterested(mask())).isFalse();

            var allMatches = List.of(mask(1, 2, 3), mask(1, 2, 4), mask(1, 2, 3, 4));
            var allMismatches = List.of(mask(3), mask(4), mask(1, 3), mask(1, 4), mask(2, 3), mask(2, 4), mask(1, 2));

            var noneMatches = List.of(mask(7), mask(8));
            var noneMismatches = List.of(mask(5, 7, 8), mask(6, 7, 8), mask(5, 6, 7, 8));

            allMatches.forEach(mask -> assertThat(spec.isInterested(mask)).as("allMatch(%s)", mask).isTrue());
            allMismatches.forEach(mask -> assertThat(spec.isInterested(mask)).as("allMismatch(%s)", mask).isFalse());

            noneMatches.forEach(mask -> assertThat(spec.isInterested(mask)).as("noneMatch(%s)", mask).isFalse());
            noneMismatches.forEach(mask -> assertThat(spec.isInterested(mask)).as("noneMismatch(%s)", mask).isFalse());

            allMatches.forEach(a -> noneMatches.forEach(n -> assertThat(spec.isInterested(merge(a, n))).as("allMatch(%s) | noneMatch(%s): ", a, n).isTrue()));
            allMatches.forEach(a -> noneMismatches.forEach(n -> assertThat(spec.isInterested(merge(a, n))).as("allMatch(%s) | noneMismatch(%s): ", a, n).isFalse()));

            allMismatches.forEach(a -> noneMatches.forEach(n -> assertThat(spec.isInterested(merge(a, n))).as("allMismatch(%s) | noneMatch(%s): ", a, n).isFalse()));
            allMismatches.forEach(a -> noneMismatches.forEach(n -> assertThat(spec.isInterested(merge(a, n))).as("allMismatch(%s) | noneMismatch(%s): ", a, n).isFalse()));
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
        void testOne_ClassesAndSpecs() {
            var nested = new AllSpec(mask(3, 4), null);
            var spec = new OneSpec(mask(1, 2), Set.of(nested));

            assertThat(spec.isInterested(mask())).isFalse();
            assertThat(spec.isInterested(mask(1))).isTrue();
            assertThat(spec.isInterested(mask(1, 2))).isTrue();
            assertThat(spec.isInterested(mask(1, 2, 3))).isTrue();
            assertThat(spec.isInterested(mask(1, 3))).isTrue();
            assertThat(spec.isInterested(mask(4))).isFalse();
            assertThat(spec.isInterested(mask(1, 2, 4))).isTrue();
            assertThat(spec.isInterested(mask(1, 2, 4, 5))).isTrue();
            assertThat(spec.isInterested(mask(1, 2, 3, 6))).isTrue();
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
        void testOneNone_ClassesAndSpecs() {
            var nestedOne = new AllSpec(mask(3, 4), null);
            var one = new OneSpec(mask(1, 2), Set.of(nestedOne));

            var nestedNone = new AllSpec(mask(7, 8), null);
            var none = new NoneSpec(mask(5, 6), Set.of(nestedNone));

            var spec = new EngineSpecImpl(null, Set.of(one), none);

            assertThat(spec.isInterested(mask())).isFalse();

            var oneMatches = List.of(mask(1), mask(2), mask(3, 4));
            var oneMismatches = List.of(mask(3), mask(4));

            var noneMatches = List.of(mask(7), mask(8));
            var noneMismatches = List.of(mask(5, 7, 8), mask(6, 7, 8), mask(5, 6, 7, 8));

            oneMatches.forEach(mask -> assertThat(spec.isInterested(mask)).as("oneMatch(%s)", mask).isTrue());
            oneMismatches.forEach(mask -> assertThat(spec.isInterested(mask)).as("oneMismatch(%s)", mask).isFalse());

            noneMatches.forEach(mask -> assertThat(spec.isInterested(mask)).as("noneMatch(%s)", mask).isFalse());
            noneMismatches.forEach(mask -> assertThat(spec.isInterested(mask)).as("noneMismatch(%s)", mask).isFalse());

            oneMatches.forEach(o -> noneMatches.forEach(n -> assertThat(spec.isInterested(merge(o, n))).as("oneMatch(%s) | noneMatch(%s): ", o, n).isTrue()));
            oneMatches.forEach(o -> noneMismatches.forEach(n -> assertThat(spec.isInterested(merge(o, n))).as("oneMatch(%s) | noneMismatch(%s): ", o, n).isFalse()));

            oneMismatches.forEach(o -> noneMatches.forEach(n -> assertThat(spec.isInterested(merge(o, n))).as("oneMismatch(%s) | noneMatch(%s): ", o, n).isFalse()));
            oneMismatches.forEach(o -> noneMismatches.forEach(n -> assertThat(spec.isInterested(merge(o, n))).as("oneMismatch(%s) | noneMismatch(%s): ", o, n).isFalse()));
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
        void testNone_ClassesAndSpecs() {
            var nested = new OneSpec(mask(3, 4), null);
            var spec = new NoneSpec(mask(1, 2), Set.of(nested));

            assertThat(spec.isInterested(mask())).isTrue();
            assertThat(spec.isInterested(mask(1))).isFalse();
            assertThat(spec.isInterested(mask(1, 2))).isFalse();
            assertThat(spec.isInterested(mask(1, 2, 3))).isFalse();
            assertThat(spec.isInterested(mask(1, 3))).isFalse();
            assertThat(spec.isInterested(mask(3))).isFalse();
            assertThat(spec.isInterested(mask(4))).isFalse();
            assertThat(spec.isInterested(mask(1, 2, 4))).isFalse();
            assertThat(spec.isInterested(mask(1, 2, 4, 5))).isFalse();
            assertThat(spec.isInterested(mask(1, 2, 3, 6))).isFalse();
            assertThat(spec.isInterested(mask(3, 4, 5))).isFalse();
            assertThat(spec.isInterested(mask(3, 5))).isFalse();
            assertThat(spec.isInterested(mask(4, 5))).isFalse();
            assertThat(spec.isInterested(mask(5))).isTrue();
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
        void testDefault_ClassesAndSpecs() {
            var nestedAll = new OneSpec(mask(3, 4), null);
            var all = new AllSpec(mask(1, 2), Set.of(nestedAll));

            var nestedOne = new AllSpec(mask(7, 8), null);
            var one = new OneSpec(mask(5, 6), Set.of(nestedOne));

            var nestedNone = new AllSpec(mask(11, 12), null);
            var none = new NoneSpec(mask(9, 10), Set.of(nestedNone));

            var spec = new EngineSpecImpl(all, Set.of(one), none);

            assertThat(spec.isInterested(mask())).isFalse();

            var allMatches = List.of(mask(1, 2, 3), mask(1, 2, 4), mask(1, 2, 3, 4));
            var allMismatches = List.of(mask(1), mask(2), mask(1, 3), mask(1, 4));

            var oneMatches = List.of(mask(5), mask(6), mask(7, 8));
            var oneMismatches = List.of(mask(7), mask(8));

            var noneMatches = List.of(mask(11), mask(12));
            var noneMismatches = List.of(mask(9, 11, 12), mask(10, 11, 12), mask(9, 10, 11, 12));

            allMatches.forEach(a -> oneMatches
                    .forEach(o -> noneMatches.forEach(n -> assertThat(spec.isInterested(merge(merge(a, o), n))).as("allMatch(%s) | oneMatch(%s) | noneMatch(%s): ", a, o, n).isTrue())));
            allMismatches.forEach(a -> oneMatches
                    .forEach(o -> noneMatches.forEach(n -> assertThat(spec.isInterested(merge(merge(a, o), n))).as("allMismatch(%s) | oneMatch(%s) | noneMatch(%s): ", a, o, n).isFalse())));
            allMatches.forEach(a -> oneMismatches
                    .forEach(o -> noneMatches.forEach(n -> assertThat(spec.isInterested(merge(merge(a, o), n))).as("allMatch(%s) | oneMismatch(%s) | noneMatch(%s): ", a, o, n).isFalse())));
            allMatches.forEach(a -> oneMatches
                    .forEach(o -> noneMismatches.forEach(n -> assertThat(spec.isInterested(merge(merge(a, o), n))).as("allMatch(%s) | oneMatch(%s) | noneMismatch(%s): ", a, o, n).isFalse())));
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

        var allSpec = all != null ? new AllSpec(all, null) : null;
        var oneSpecs = ones != null ? ones.stream().map(one -> new OneSpec(one, null)).collect(Collectors.toSet()) : null;
        var noneSpec = none != null ? new NoneSpec(none, null) : null;

        return new EngineSpecImpl(allSpec, oneSpecs, noneSpec);
    }

    private BitVector mask(int... set) {
        var mask = new BitVector();
        for (int index : set) {
            mask.set(index);
        }

        return mask;
    }

    private BitVector merge(BitVector first, BitVector second) {
        var mask = new BitVector(first);
        second.iterate(mask::set);

        return mask;
    }

}
