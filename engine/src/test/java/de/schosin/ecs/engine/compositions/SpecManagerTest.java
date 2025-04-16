package de.schosin.ecs.engine.compositions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Composition;
import de.schosin.ecs.api.components.Composition.Builder;
import de.schosin.ecs.api.components.Spec;
import de.schosin.ecs.engine.AbstractWorldTest;

class SpecManagerTest {

    @Nested
    class IsInterestedTest extends AbstractIsInterestedTest<Spec> {

        @Override
        protected Spec create(Builder builder) {
            return world.createSpec(builder);
        }

        @Override
        protected boolean isInterested(Spec spec, int entityId) {
            return spec.isInterested(entityId);
        }

    }

    public abstract static class AbstractIsInterestedTest<T extends Spec> extends AbstractWorldTest {

        int none;
        int c1;
        int c12;
        int c13;
        int c2;
        int c23;
        int c3;
        int c123;

        protected abstract T create(Composition.Builder builder);

        protected abstract boolean isInterested(T spec, int entityId);

        @BeforeEach
        void setup() {
            none = world.createEntity();
            c1 = world.createEntity(new C1());
            c12 = world.createEntity(new C1(), new C2());
            c13 = world.createEntity(new C1(), new C3());
            c2 = world.createEntity(new C2());
            c23 = world.createEntity(new C2(), new C3());
            c3 = world.createEntity(new C3());
            c123 = world.createEntity(new C1(), new C2(), new C3());
        }

        @Test
        void emptySpec() {
            var spec = create(Spec.all());

            assertThat(isInterested(spec, none)).isTrue();
            assertThat(isInterested(spec, c1)).isTrue();
            assertThat(isInterested(spec, c12)).isTrue();
            assertThat(isInterested(spec, c13)).isTrue();
            assertThat(isInterested(spec, c2)).isTrue();
            assertThat(isInterested(spec, c23)).isTrue();
            assertThat(isInterested(spec, c3)).isTrue();
            assertThat(isInterested(spec, c123)).isTrue();
        }

        @Test
        void allSpec() {
            var spec = create(Spec.all(C1.class));

            assertThat(isInterested(spec, none)).isFalse();
            assertThat(isInterested(spec, c1)).isTrue();
            assertThat(isInterested(spec, c12)).isTrue();
            assertThat(isInterested(spec, c13)).isTrue();
            assertThat(isInterested(spec, c2)).isFalse();
            assertThat(isInterested(spec, c23)).isFalse();
            assertThat(isInterested(spec, c3)).isFalse();
            assertThat(isInterested(spec, c123)).isTrue();
        }

        @Test
        void allOneSpec() {
            var spec = create(Spec.all(C1.class).one(C2.class));

            assertThat(isInterested(spec, none)).isFalse();
            assertThat(isInterested(spec, c1)).isFalse();
            assertThat(isInterested(spec, c12)).isTrue();
            assertThat(isInterested(spec, c13)).isFalse();
            assertThat(isInterested(spec, c2)).isFalse();
            assertThat(isInterested(spec, c23)).isFalse();
            assertThat(isInterested(spec, c3)).isFalse();
            assertThat(isInterested(spec, c123)).isTrue();
        }

        @Test
        void allNoneSpec() {
            var spec = create(Spec.all(C1.class).none(C3.class));

            assertThat(isInterested(spec, none)).isFalse();
            assertThat(isInterested(spec, c1)).isTrue();
            assertThat(isInterested(spec, c12)).isTrue();
            assertThat(isInterested(spec, c13)).isFalse();
            assertThat(isInterested(spec, c2)).isFalse();
            assertThat(isInterested(spec, c23)).isFalse();
            assertThat(isInterested(spec, c3)).isFalse();
            assertThat(isInterested(spec, c123)).isFalse();
        }

        @Test
        void oneSpec() {
            var spec = create(Spec.one(C2.class));

            assertThat(isInterested(spec, none)).isFalse();
            assertThat(isInterested(spec, c1)).isFalse();
            assertThat(isInterested(spec, c12)).isTrue();
            assertThat(isInterested(spec, c13)).isFalse();
            assertThat(isInterested(spec, c2)).isTrue();
            assertThat(isInterested(spec, c23)).isTrue();
            assertThat(isInterested(spec, c3)).isFalse();
            assertThat(isInterested(spec, c123)).isTrue();
        }

        @Test
        void oneNoneSpec() {
            var spec = create(Spec.one(C2.class).none(C3.class));

            assertThat(isInterested(spec, none)).isFalse();
            assertThat(isInterested(spec, c1)).isFalse();
            assertThat(isInterested(spec, c12)).isTrue();
            assertThat(isInterested(spec, c13)).isFalse();
            assertThat(isInterested(spec, c2)).isTrue();
            assertThat(isInterested(spec, c23)).isFalse();
            assertThat(isInterested(spec, c3)).isFalse();
            assertThat(isInterested(spec, c123)).isFalse();
        }

        @Test
        void defaultSpec() {
            var spec = create(Spec.all(C1.class).one(C2.class).none(C3.class));

            assertThat(isInterested(spec, none)).isFalse();
            assertThat(isInterested(spec, c1)).isFalse();
            assertThat(isInterested(spec, c12)).isTrue();
            assertThat(isInterested(spec, c13)).isFalse();
            assertThat(isInterested(spec, c2)).isFalse();
            assertThat(isInterested(spec, c23)).isFalse();
            assertThat(isInterested(spec, c3)).isFalse();
            assertThat(isInterested(spec, c123)).isFalse();
        }

    }

    public record C1() {
    }

    public record C2() {
    }

    public record C3() {
    }

}
