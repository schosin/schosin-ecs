package de.schosin.ecs.plugins.wildcards;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.ComponentSetConfig;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.plugins.wildcards.result.WildcardComponentRelations;
import de.schosin.ecs.plugins.wildcards.result.WildcardEntityRelations;
import de.schosin.ecs.plugins.wildcards.result.WildcardEntityRelationsData;
import de.schosin.ecs.plugins.wildcards.result.WildcardResult;
import de.schosin.ecs.test.AbstractEcsTest;

@SuppressWarnings("unused")
class WildcardManagerTest extends AbstractEcsTest<WildcardWorld> {

    @Nested
    class ComponentSetDetectionTest {

        @Nested
        class WildcardResultTest {

            @Test
            void testWildcardResult() {
                var component1 = new Component1(1);
                var component2 = new Component2(2);
                var entityId = world.createEntity(component1, component2);

                var mapper = world.getComponents(WildcardResultSet.TYPE);

                var result = mapper.get(entityId);
                assertThat(result).isNotNull();

                // Verify
                var components = result.components();
                assertThat(components).containsExactlyInAnyOrder(component1, component2);
                assertThat(components.toString()).contains(component1.toString(), component2.toString());
            }

            @Test
            void testReclaim() {
                var component1 = new Component1(1);
                var component2 = new Component2(2);
                var entityId = world.createEntity(component1, component2);

                var mapper = world.getComponents(WildcardResultSet.TYPE);

                var result = mapper.get(entityId);
                assertThat(result).isNotNull();

                var components = result.components();
                assertThat(components).containsExactlyInAnyOrder(component1, component2);
                assertThat(components.toString()).contains(component1.toString(), component2.toString());

                // Call
                world.process();

                // Verify
                assertThat(components.toString()).contains("invalidated");
            }

            @ComponentSetConfig("WildcardResultSet")
            private void processEntity(int entityId, WildcardResult<Object> components) {
            }

        }

        @Nested
        class WildcardComponentRelationsTest {

            @Test
            void testWildcardResult() {
                var relation1 = Relation.create(new Relationship1(1), new Target1(10));
                var relation2 = Relation.create(new Relationship2(2), new Target1(20));
                var relation3 = Relation.create(new Relationship3(3), new Target1(30));

                var entityId = world.createEntity(relation1, relation2, relation3);

                var mapper = world.getComponents(WildcardComponentRelationsSet.TYPE);

                var result = mapper.get(entityId);
                assertThat(result).isNotNull();

                // Verify
                var components = result.components();
                assertThat(components).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation1, relation2, relation3);
                assertThat(components.toString()).contains(relation1.toString(), relation2.toString(), relation3.toString());

                var interfaceComponents = result.interfaceComponents();
                assertThat(interfaceComponents).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation1, relation2);
                assertThat(interfaceComponents.toString()).contains(relation1.toString(), relation2.toString());
            }

            @Test
            void testReclaim() {
                var relation1 = Relation.create(new Relationship1(1), new Target1(10));
                var relation2 = Relation.create(new Relationship2(2), new Target1(20));
                var relation3 = Relation.create(new Relationship3(3), new Target1(30));

                var entityId = world.createEntity(relation1, relation2, relation3);

                var mapper = world.getComponents(WildcardComponentRelationsSet.TYPE);

                var result = mapper.get(entityId);
                assertThat(result).isNotNull();

                var components = result.components();
                assertThat(components).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation1, relation2, relation3);
                assertThat(components.toString()).contains(relation1.toString(), relation2.toString(), relation3.toString());

                var interfaceComponents = result.interfaceComponents();
                assertThat(interfaceComponents).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation1, relation2);
                assertThat(interfaceComponents.toString()).contains(relation1.toString(), relation2.toString());

                // Call
                world.process();

                // Verify
                assertThat(components.toString()).contains("invalidated");
                assertThat(interfaceComponents.toString()).contains("invalidated");
            }

            @ComponentSetConfig("WildcardComponentRelationsSet")
            private void processEntity(int entityId, WildcardComponentRelations<Object, Object> components, WildcardComponentRelations<Relationship12, Object> interfaceComponents) {
            }

        }

        @Nested
        class WildcardEntityRelationsTest {

            @Test
            void testWildcardResult() {
                var target = world.createEntity();

                var relation1 = Relation.create(new Relationship1(1), target);
                var relation2 = Relation.create(new Relationship2(2), target);
                var relation3 = Relation.create(new Relationship3(3), target);

                var entityId = world.createEntity(relation1, relation2, relation3);

                var mapper = world.getComponents(WildcardEntityRelationsSet.TYPE);

                var result = mapper.get(entityId);
                assertThat(result).isNotNull();

                // Verify
                var components = result.components();
                assertThat(components).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation1, relation2, relation3);
                assertThat(components.toString()).contains(relation1.toString(), relation2.toString(), relation3.toString());

                var interfaceComponents = result.interfaceComponents();
                assertThat(interfaceComponents).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation1, relation2);
                assertThat(interfaceComponents.toString()).contains(relation1.toString(), relation2.toString());
            }

            @Test
            void testReclaim() {
                var target = world.createEntity();

                var relation1 = Relation.create(new Relationship1(1), target);
                var relation2 = Relation.create(new Relationship2(2), target);
                var relation3 = Relation.create(new Relationship3(3), target);

                var entityId = world.createEntity(relation1, relation2, relation3);

                var mapper = world.getComponents(WildcardEntityRelationsSet.TYPE);

                var result = mapper.get(entityId);
                assertThat(result).isNotNull();

                var components = result.components();
                assertThat(components).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation1, relation2, relation3);
                assertThat(components.toString()).contains(relation1.toString(), relation2.toString(), relation3.toString());

                var interfaceComponents = result.interfaceComponents();
                assertThat(interfaceComponents).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation1, relation2);
                assertThat(interfaceComponents.toString()).contains(relation1.toString(), relation2.toString());

                // Call
                world.process();

                // Verify
                assertThat(components.toString()).contains("invalidated");
                assertThat(interfaceComponents.toString()).contains("invalidated");
            }

            @ComponentSetConfig("WildcardEntityRelationsSet")
            private void processEntity(int entityId, WildcardEntityRelations<Object> components, WildcardEntityRelations<Relationship12> interfaceComponents) {
            }

        }

        @Nested
        class WildcardEntityRelationsDataTest {

            @Test
            void testWildcardResult() {
                var component1 = new Component1(421);
                var target1 = world.createEntity(component1);
                var component2 = new Component1(422);
                var target2 = world.createEntity(component2);
                var component3 = new Component1(423);
                var target3 = world.createEntity(component3);

                var relation1 = Relation.create(new Relationship1(1), target1);
                var relation2 = Relation.create(new Relationship2(2), target2);
                var relation3 = Relation.create(new Relationship3(3), target3);

                var entityId = world.createEntity(relation1, relation2, relation3);

                var mapper = world.getComponents(WildcardEntityRelationsDataSet.TYPE);

                var result = mapper.get(entityId);
                assertThat(result).isNotNull();

                // Verify
                var components = result.components();
                assertThat(components).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation1, relation2, relation3);
                assertThat(components.toString())
                        .containsSubsequence(new Relationship1(1).toString(), Integer.toString(target1), component1.toString())
                        .containsSubsequence(new Relationship2(2).toString(), Integer.toString(target2), component2.toString())
                        .containsSubsequence(new Relationship3(3).toString(), Integer.toString(target3), component3.toString());

                var interfaceComponents = result.interfaceComponents();
                assertThat(interfaceComponents).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation1, relation2);
                assertThat(interfaceComponents.toString())
                        .containsSubsequence(new Relationship1(1).toString(), Integer.toString(target1), component1.toString())
                        .containsSubsequence(new Relationship2(2).toString(), Integer.toString(target2), component2.toString());
            }

            @Test
            void testReclaim() {
                var component1 = new Component1(421);
                var target1 = world.createEntity(component1);
                var component2 = new Component1(422);
                var target2 = world.createEntity(component2);
                var component3 = new Component1(423);
                var target3 = world.createEntity(component3);

                var relation1 = Relation.create(new Relationship1(1), target1);
                var relation2 = Relation.create(new Relationship2(2), target2);
                var relation3 = Relation.create(new Relationship3(3), target3);

                var entityId = world.createEntity(relation1, relation2, relation3);

                var mapper = world.getComponents(WildcardEntityRelationsDataSet.TYPE);

                var result = mapper.get(entityId);
                assertThat(result).isNotNull();

                var components = result.components();
                assertThat(components).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation1, relation2, relation3);
                assertThat(components.toString())
                        .containsSubsequence(new Relationship1(1).toString(), Integer.toString(target1), component1.toString())
                        .containsSubsequence(new Relationship2(2).toString(), Integer.toString(target2), component2.toString())
                        .containsSubsequence(new Relationship3(3).toString(), Integer.toString(target3), component3.toString());

                var interfaceComponents = result.interfaceComponents();
                assertThat(interfaceComponents).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation1, relation2);
                assertThat(interfaceComponents.toString())
                        .containsSubsequence(new Relationship1(1).toString(), Integer.toString(target1), component1.toString())
                        .containsSubsequence(new Relationship2(2).toString(), Integer.toString(target2), component2.toString());

                // Call
                world.process();

                // Verify
                assertThat(components.toString()).contains("invalidated");
                assertThat(interfaceComponents.toString()).contains("invalidated");
            }

            @ComponentSetConfig("WildcardEntityRelationsDataSet")
            private void processEntity(int entityId, WildcardEntityRelationsData<Object, Component1> components, WildcardEntityRelationsData<Relationship12, Component1> interfaceComponents) {
            }

        }

    }

    public record Component1(int value) {
    }

    public record Component2(int value) {
    }

    interface Relationship12 {
    }

    record Relationship1(int value) implements Relationship12 {
    }

    record Relationship2(int value) implements Relationship12 {
    }

    record Relationship3(int value) implements Exclusive {
    }

    record Target1(int value) {
    }

}
