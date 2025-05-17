package de.schosin.ecs.engine.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.ComponentType.RegularComponentRelationType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.engine.AbstractWorldTest;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.ComponentRelationComponent;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.junit5.SealedSubclassesSource;
import de.schosin.ecs.utils.junit5.SealedSubclassesSource.Mode;

class ComponentMaskManagerTest extends AbstractWorldTest {

    enum TestCases {

        regularComponent(component(RegularComponent.class), RegularComponent::new),
        pooledComponent(component(PooledComponent.class), PooledComponent::new),
        componentRelation(relation(RelationshipComponent.class, TargetComponent.class), getRelation(new RelationshipComponent(), new TargetComponent(1))),
        exclusiveComponentRelation(exclusiveRelation(ExclusiveRelationshipComponent.class, TargetComponent.class), getRelation(new ExclusiveRelationshipComponent(), new TargetComponent(1)));

        private final RegularComponentType<?, ?> type;
        private final Function<Component<?, ?>, Object> instance;

        private TestCases(RegularComponentType<?, ?> type, Supplier<Object> instance) {
            this(type, components -> instance.get());

        }

        private TestCases(RegularComponentType<?, ?> type, Function<Component<?, ?>, Object> instance) {
            this.type = type;
            this.instance = instance;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private static Function<Component<?, ?>, Object> getRelation(Object relationship, Object target) {
            return component -> ((ComponentRelationComponent) component).getInstance(relationship, target);
        }

    }

    @ParameterizedTest
    @SealedSubclassesSource(value = Component.class, mode = Mode.NON_SEALED)
    void verifyTestCases(Class<? extends Component<?, ?>> clazz) {
        for (var test : TestCases.values()) {
            var component = componentManager.getComponent(test.type);
            if (clazz.isAssignableFrom(component.getClass())) {
                return;
            }
        }

        fail("Component type '%s' not tested", clazz.getName());
    }

    @Nested
    class GetComponentMaskByIdTest {

        @ParameterizedTest
        @EnumSource(TestCases.class)
        void testGetById(TestCases test) {
            var component = componentManager.getComponent(test.type);

            var entityId = world.createEntity(test.instance.apply(component));
            var componentMask = entityManager.getComponentMask(entityId);

            assertThat(componentMaskManager.getComponentMask(componentMask.getId())).isSameAs(componentMask);
        }

    }

    @Nested
    class GetComponentMaskByComponentTypeTest {

        @ParameterizedTest
        @EnumSource(TestCases.class)
        void testSingleComponentType(TestCases test) {
            var component = componentManager.getComponent(test.type);
            var componentMask = componentMaskManager.getComponentMask(test.type);

            assertThat(componentMask.getComponents()).singleElement().isSameAs(component);
            assertThat(componentMask.getMask().get(component.id())).isTrue();
        }

        @Test
        void testMultipleTypes() {
            var types = Arrays.stream(TestCases.values()).map(test -> test.type).toArray(RegularComponentType<?, ?>[]::new);

            var componentMask = componentMaskManager.getComponentMask(types);
            assertThat(componentMask.getComponents()).hasSameSizeAs(types);

            for (var type : types) {
                var component = componentManager.getComponent(type);

                assertThat(componentMask.getComponents()).contains(component);
                assertThat(componentMask.getMask().get(component.id())).isTrue();
            }
        }

        @ParameterizedTest
        @EnumSource(TestCases.class)
        void testDuplicateComponentInstaces(TestCases test) {
            assumeThat(test.type).isNotInstanceOf(RegularComponentRelationType.class);

            assertThatThrownBy(() -> componentMaskManager.getComponentMask(test.type, component(OtherComponent.class), test.type))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Detected duplicate component types");
        }

    }

    @Nested
    class GetComponentMaskByComponentInstancesTest {

        @ParameterizedTest
        @EnumSource(TestCases.class)
        void testSingleComponentInstance(TestCases test) {
            var component = componentManager.getComponent(test.type);
            var instance = test.instance.apply(component);

            var componentMask = componentMaskManager.getComponentMask(instance);

            assertThat(componentMask.getComponents()).singleElement().isSameAs(component);
            assertThat(componentMask.getMask().get(component.id())).isTrue();
        }

        @Test
        void testMultipleComponentInstances() {
            var testCases = TestCases.values();

            var components = new Component<?, ?>[testCases.length];
            var instances = new Object[testCases.length];

            for (int i = 0, s = testCases.length; i < s; i++) {
                var test = testCases[i];

                var component = components[i] = componentManager.getComponent(test.type);
                instances[i] = test.instance.apply(component);
            }

            var componentMask = componentMaskManager.getComponentMask(instances);
            assertThat(componentMask.getComponents()).containsExactlyInAnyOrder(components);

            for (var component : components) {
                assertThat(componentMask.getMask().get(component.id())).isTrue();
            }
        }

        @Test
        void testDuplicateComponentInstaces() {
            assertThatThrownBy(() -> componentMaskManager.getComponentMask(new RegularComponent(), new OtherComponent(), new RegularComponent()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Detected duplicate component types");
        }

        @Test
        void testMultipleRelationsOfSameType() {
            var component = componentManager.getComponent(relation(RelationshipComponent.class, TargetComponent.class));

            var relationship = new RelationshipComponent();
            var relation1 = component.getInstance(relationship, new TargetComponent(1));
            var relation2 = component.getInstance(relationship, new TargetComponent(2));

            var componentMask = componentMaskManager.getComponentMask(relation1, relation2);

            assertThat(componentMask.getComponents()).singleElement().isSameAs(component);
            assertThat(componentMask.getMask().get(component.id())).isTrue();
        }

    }

    @Nested
    class GetComponentMaskByComponentDataTest {

        @ParameterizedTest
        @EnumSource(TestCases.class)
        void testSingleComponentData(TestCases test) {
            var component = componentManager.getComponent(test.type);
            var componentMask = componentMaskManager.getComponentMask(component);

            assertThat(componentMask.getComponents()).singleElement().isSameAs(component);
            assertThat(componentMask.getMask().get(component.id())).isTrue();
        }

        @Test
        void testMultipleComponentData() {
            var components = Arrays.stream(TestCases.values()).map(test -> componentManager.getComponent(test.type)).toArray(Component<?, ?>[]::new);

            var componentMask = componentMaskManager.getComponentMask(components);
            assertThat(componentMask.getComponents()).containsExactlyInAnyOrder(components);

            for (var component : components) {
                assertThat(componentMask.getMask().get(component.id())).isTrue();
            }
        }

        @ParameterizedTest
        @EnumSource(TestCases.class)
        void testDuplicateComponentInstaces(TestCases test) {
            assumeThat(test.type).isNotInstanceOf(RegularComponentRelationType.class);

            var component = componentManager.getComponent(test.type);
            var otherComponent = componentManager.getComponent(component(OtherComponent.class));

            assertThatThrownBy(() -> componentMaskManager.getComponentMask(component, otherComponent, component))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Detected duplicate component types");
        }

    }

    @Nested
    class AddComponentTest {

        @ParameterizedTest
        @EnumSource(TestCases.class)
        void testEmptyEntity(TestCases test) {
            var component = componentManager.getComponent(test.type);

            var entityId = world.createEntity();
            var componentMask = entityManager.getComponentMask(entityId);

            var newComponentMask = componentMaskManager.addComponent(componentMask, component);
            assertThat(newComponentMask).isNotSameAs(componentMask);
            assertThat(newComponentMask.getComponents()).containsOnly(component);
            assertThat(newComponentMask.getMask().get(component.id())).isTrue();
        }

        @ParameterizedTest
        @EnumSource(TestCases.class)
        void testEntityWithOtherComponent(TestCases test) {
            var component = componentManager.getComponent(test.type);

            var entityId = world.createEntity(new OtherComponent());
            var componentMask = entityManager.getComponentMask(entityId);

            var newComponentMask = componentMaskManager.addComponent(componentMask, component);
            assertThat(newComponentMask).isNotSameAs(componentMask);
            assertThat(newComponentMask.getComponents()).hasSize(2).contains(component);
            assertThat(newComponentMask.getMask().get(component.id())).isTrue();
        }

        @ParameterizedTest
        @EnumSource(TestCases.class)
        void testEntityWithComponent_DoesNotAlterComponentMask(TestCases test) {
            var component = componentManager.getComponent(test.type);

            var entityId = world.createEntity(test.instance.apply(component));
            var componentMask = entityManager.getComponentMask(entityId);

            var newComponentMask = componentMaskManager.addComponent(componentMask, component);
            assertThat(newComponentMask).isSameAs(componentMask);
        }

    }

    @Nested
    class RemoveComponentTest {

        @ParameterizedTest
        @EnumSource(TestCases.class)
        void testEmptyEntity(TestCases test) {
            var component = componentManager.getComponent(test.type);

            var entityId = world.createEntity();
            var componentMask = entityManager.getComponentMask(entityId);

            var newComponentMask = componentMaskManager.removeComponent(componentMask, component);
            assertThat(newComponentMask).isSameAs(componentMask);
        }

        @ParameterizedTest
        @EnumSource(TestCases.class)
        void testEntityWithOtherComponent(TestCases test) {
            var component = componentManager.getComponent(test.type);

            var entityId = world.createEntity(new OtherComponent());
            var componentMask = entityManager.getComponentMask(entityId);

            var newComponentMask = componentMaskManager.removeComponent(componentMask, component);
            assertThat(newComponentMask).isSameAs(componentMask);
        }

        @ParameterizedTest
        @EnumSource(TestCases.class)
        void testEntityWithComponent(TestCases test) {
            var component = componentManager.getComponent(test.type);

            var entityId = world.createEntity(test.instance.apply(component));
            var componentMask = entityManager.getComponentMask(entityId);

            var newComponentMask = componentMaskManager.removeComponent(componentMask, component);
            assertThat(newComponentMask).isNotSameAs(componentMask);
            assertThat(newComponentMask.getComponents()).isEmpty();
            assertThat(newComponentMask.getMask().get(component.id())).isFalse();
        }

        @ParameterizedTest
        @EnumSource(TestCases.class)
        void testEntityWithComponentAndOtherComponent(TestCases test) {
            var component = componentManager.getComponent(test.type);

            var entityId = world.createEntity(new OtherComponent(), test.instance.apply(component));
            var componentMask = entityManager.getComponentMask(entityId);

            var newComponentMask = componentMaskManager.removeComponent(componentMask, component);
            assertThat(newComponentMask).isNotSameAs(componentMask);
            assertThat(newComponentMask.getComponents()).hasSize(1).doesNotContain(component);
            assertThat(newComponentMask.getMask().get(component.id())).isFalse();
        }

    }

    @Nested
    class GetComponentMasksTest {

        @Test
        void testMatchAll() {
            var components = new HashSet<Component<?, ?>>();

            for (var test : TestCases.values()) {
                var component = componentManager.getComponent(test.type);
                components.add(component);

                world.createEntity(test.instance.apply(component));
            }

            var bag = new Bag<>(ComponentMask.class);

            // Call
            componentMaskManager.getComponentMasks(mask -> true, bag);

            // Verify
            assertThat(iterable(bag))
                    .hasSameSizeAs(components)
                    .as("has single component").allSatisfy(mask -> assertThat(mask.getComponents()).singleElement().isIn(components));
        }

        @Test
        void testPredicate() {
            var components = new HashSet<Component<?, ?>>();

            for (var test : TestCases.values()) {
                var component = componentManager.getComponent(test.type);
                components.add(component);

                world.createEntity(test.instance.apply(component));
            }

            var bag = new Bag<>(ComponentMask.class);

            // Call
            componentMaskManager.getComponentMasks(mask -> mask.getComponents()[0].display().contains("Pooled"), bag);

            var expected = componentManager.getComponent(component(PooledComponent.class));

            // Verify
            assertThat(iterable(bag))
                    .singleElement()
                    .satisfies(mask -> assertThat(mask.getComponents()).singleElement().isSameAs(expected));
        }

    }

    record RegularComponent() {
    }

    record PooledComponent() implements Pooled {
    }

    static record RelationshipComponent() {
    }

    static record ExclusiveRelationshipComponent() implements Exclusive {
    }

    static record TargetComponent(int value) {
    }

    private record OtherComponent() {
    }

}
