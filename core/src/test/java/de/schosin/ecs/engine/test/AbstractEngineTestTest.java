package de.schosin.ecs.engine.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.sun.jdi.ClassType;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.mappers.ComponentMapper.PooledComponentMapper;
import de.schosin.ecs.engine.AbstractWorldTest;

class AbstractEngineTestTest {

    @Nested
    class ComponentTest extends AbstractWorldTest {

        @Test
        void testGetComponent() {
            var c1 = new C1();
            var entityId = world.createEntity(c1);

            assertThat(getComponent(entityId, C1.class)).isSameAs(c1);
        }

        @Test
        void testGetComponent_WhenNotPresent_ReturnsNull() {
            var entityId = world.createEntity();

            assertThat(getComponent(entityId, C1.class)).isNull();
        }

        @Test
        void testHasComponent() {
            var entityId = world.createEntity(new C1());

            assertThatCode(() -> verifyHasComponents(entityId, C1.class)).doesNotThrowAnyException();
        }

        @Test
        void testHasComponent_WhenNotPresent_Throws() {
            var entityId = world.createEntity();

            assertThatThrownBy(() -> verifyHasComponents(entityId, C1.class))
                    .isInstanceOf(AssertionError.class)
                    .message().containsSubsequence("entity has", ClassType.class.getSimpleName(), C1.class.getSimpleName());
        }

        @Test
        void testHasComponents() {
            var entityId = world.createEntity(new C1(), new C2());

            assertThatCode(() -> verifyHasComponents(entityId, C1.class)).doesNotThrowAnyException();
            assertThatCode(() -> verifyHasComponents(entityId, C2.class)).doesNotThrowAnyException();
            assertThatCode(() -> verifyHasComponents(entityId, C1.class, C2.class)).doesNotThrowAnyException();
        }

        @Test
        void testHasComponents_WhenNotPresent_Throws() {
            var entityId = world.createEntity();

            assertThatThrownBy(() -> verifyHasComponents(entityId, C1.class))
                    .isInstanceOf(AssertionError.class)
                    .message().containsSubsequence("entity has ", ClassType.class.getSimpleName(), C1.class.getSimpleName());

            assertThatThrownBy(() -> verifyHasComponents(entityId, C2.class))
                    .isInstanceOf(AssertionError.class)
                    .message().containsSubsequence("entity has ", ClassType.class.getSimpleName(), C2.class.getSimpleName());

            assertThatThrownBy(() -> verifyHasComponents(entityId, C1.class, C2.class))
                    .isInstanceOf(AssertionError.class)
                    .message().containsSubsequence("entity has ", ClassType.class.getSimpleName(), C1.class.getSimpleName());

        }

        @Test
        void testDoesNotHaveComponent() {
            var entityId = world.createEntity();

            assertThatCode(() -> verifyDoesNotHaveComponents(entityId, C1.class)).doesNotThrowAnyException();
        }

        @Test
        void testDoesNotHaveComponent_WhenPresent_Throws() {
            var entityId = world.createEntity(new C1());

            assertThatThrownBy(() -> verifyDoesNotHaveComponents(entityId, C1.class))
                    .isInstanceOf(AssertionError.class)
                    .message().containsSubsequence("entity does not have ", ClassType.class.getSimpleName(), C1.class.getSimpleName());
        }

        @Test
        void testDoesNotHaveComponents() {
            var entityId = world.createEntity();

            assertThatCode(() -> verifyDoesNotHaveComponents(entityId, C1.class)).doesNotThrowAnyException();
            assertThatCode(() -> verifyDoesNotHaveComponents(entityId, C2.class)).doesNotThrowAnyException();
            assertThatCode(() -> verifyDoesNotHaveComponents(entityId, C1.class, C2.class)).doesNotThrowAnyException();
        }

        @Test
        void testDoesNotHaveComponents_WhenPresent_Throws() {
            var entityId = world.createEntity(new C1(), new C2());

            assertThatThrownBy(() -> verifyDoesNotHaveComponents(entityId, C1.class))
                    .isInstanceOf(AssertionError.class)
                    .message().containsSubsequence("entity does not have ", ClassType.class.getSimpleName(), C1.class.getSimpleName());

            assertThatThrownBy(() -> verifyDoesNotHaveComponents(entityId, C2.class))
                    .isInstanceOf(AssertionError.class)
                    .message().containsSubsequence("entity does not have ", ClassType.class.getSimpleName(), C2.class.getSimpleName());

            assertThatThrownBy(() -> verifyDoesNotHaveComponents(entityId, C1.class, C2.class))
                    .isInstanceOf(AssertionError.class)
                    .message().containsSubsequence("entity does not have ", ClassType.class.getSimpleName(), C1.class.getSimpleName());
        }

    }

    @Nested
    class ArchetypeTest extends AbstractWorldTest {

        PooledComponentMapper<C1> mapper1;
        PooledComponentMapper<C2> mapper2;

        @BeforeEach
        void setupMappers() {
            this.mapper1 = world.getPooledComponents(C1.class);
            this.mapper2 = world.getPooledComponents(C2.class);
        }

        @Test
        void testArchetypeHasComponents() {
            var entityId = world.createEntity(new C1(), new C2());

            assertThatCode(() -> verifyArchetypeHasComponents(entityId, C1.class)).doesNotThrowAnyException();
            assertThatCode(() -> verifyArchetypeHasComponents(entityId, C2.class)).doesNotThrowAnyException();
            assertThatCode(() -> verifyArchetypeHasComponents(entityId, C1.class, C2.class)).doesNotThrowAnyException();
        }

        @Test
        void testArchetypeHasComponents_WhenUpdateNotProcessed() {
            var entityId = world.createEntity();

            mapper1.add(entityId);
            mapper2.add(entityId);

            assertThatThrownBy(() -> verifyArchetypeHasComponents(entityId, C1.class))
                    .isInstanceOf(AssertionError.class)
                    .message().containsSubsequence("archetype has ", ClassType.class.getSimpleName(), C1.class.getSimpleName());

            assertThatThrownBy(() -> verifyArchetypeHasComponents(entityId, C2.class))
                    .isInstanceOf(AssertionError.class)
                    .message().containsSubsequence("archetype has ", ClassType.class.getSimpleName(), C2.class.getSimpleName());

            assertThatThrownBy(() -> verifyArchetypeHasComponents(entityId, C1.class, C2.class))
                    .isInstanceOf(AssertionError.class)
                    .message().containsSubsequence("archetype has ", ClassType.class.getSimpleName(), C1.class.getSimpleName());
        }

        @Test
        void testArchetypeHasComponents_WhenNotPresent_Throws() {
            var entityId = world.createEntity();

            assertThatThrownBy(() -> verifyArchetypeHasComponents(entityId, C1.class))
                    .isInstanceOf(AssertionError.class)
                    .message().containsSubsequence("archetype has ", ClassType.class.getSimpleName(), C1.class.getSimpleName());

            assertThatThrownBy(() -> verifyArchetypeHasComponents(entityId, C2.class))
                    .isInstanceOf(AssertionError.class)
                    .message().containsSubsequence("archetype has ", ClassType.class.getSimpleName(), C2.class.getSimpleName());

            assertThatThrownBy(() -> verifyArchetypeHasComponents(entityId, C1.class, C2.class))
                    .isInstanceOf(AssertionError.class)
                    .message().containsSubsequence("archetype has ", ClassType.class.getSimpleName(), C1.class.getSimpleName());
        }

        @Test
        void testArchetypeDoesNotHaveComponents() {
            var entityId = world.createEntity();

            assertThatCode(() -> verifyArchetypeDoesNotHaveComponents(entityId, C1.class)).doesNotThrowAnyException();
            assertThatCode(() -> verifyArchetypeDoesNotHaveComponents(entityId, C2.class)).doesNotThrowAnyException();
            assertThatCode(() -> verifyArchetypeDoesNotHaveComponents(entityId, C1.class, C2.class)).doesNotThrowAnyException();
        }

        @Test
        void testArchetypeDoesNotHaveComponents_WhenUpdateNotProcessed() {
            var entityId = world.createEntity();

            mapper1.add(entityId);
            mapper2.add(entityId);

            assertThatCode(() -> verifyArchetypeDoesNotHaveComponents(entityId, C1.class)).doesNotThrowAnyException();
            assertThatCode(() -> verifyArchetypeDoesNotHaveComponents(entityId, C2.class)).doesNotThrowAnyException();
            assertThatCode(() -> verifyArchetypeDoesNotHaveComponents(entityId, C1.class, C2.class)).doesNotThrowAnyException();
        }

        @Test
        void testArchetypeDoesNotHaveComponents_WhenPresent_Throws() {
            var entityId = world.createEntity(new C1(), new C2());

            assertThatThrownBy(() -> verifyArchetypeDoesNotHaveComponents(entityId, C1.class))
                    .isInstanceOf(AssertionError.class)
                    .message().containsSubsequence("archetype does not have ", ClassType.class.getSimpleName(), C1.class.getSimpleName());

            assertThatThrownBy(() -> verifyArchetypeDoesNotHaveComponents(entityId, C2.class))
                    .isInstanceOf(AssertionError.class)
                    .message().containsSubsequence("archetype does not have ", ClassType.class.getSimpleName(), C2.class.getSimpleName());

            assertThatThrownBy(() -> verifyArchetypeDoesNotHaveComponents(entityId, C1.class, C2.class))
                    .isInstanceOf(AssertionError.class)
                    .message().containsSubsequence("archetype does not have ", ClassType.class.getSimpleName(), C1.class.getSimpleName());
        }

    }

    @Nested
    class VerifyTest extends AbstractWorldTest {

        @Nested
        class VerifyMethodTest extends AbstractVerifyTest {

            @Override
            protected void verify(Consumer<Verify> consumer) {
                var verify = createVerify();
                consumer.accept(verify);

                verify.verify();
            }

        }

        @Nested
        class AutoCloseTest extends AbstractVerifyTest {

            @Override
            protected void verify(Consumer<Verify> consumer) {
                try (var verify = createVerify()) {
                    consumer.accept(verify);
                } catch (Exception ex) {
                    fail("AutoClose threw %s: %s".formatted(ex.getClass().getSimpleName(), ex.getMessage()), ex);
                }
            }

        }

        @Nested
        class LambdaTest extends AbstractVerifyTest {

            @Override
            protected void verify(Consumer<Verify> consumer) {
                VerifyTest.this.verify(consumer);
            }

        }

        abstract class AbstractVerifyTest {

            protected abstract void verify(Consumer<Verify> consumer);

            @Test
            void testNothingHappening() throws Exception {
                verify(verify -> {
                    assertThatCode(verify::verify).doesNotThrowAnyException();
                });
            }

            @Nested
            class InsertedTest {

                @Test
                void testEntityInserted_WhenNoVerifications_Passes() {
                    verify(verify -> {
                        world.createEntity();
                    });
                }

                @Test
                void testEntityInserted_WhenNoMoreInserted_EmptyEntityCreated_Throws() {
                    assertThatThrownBy(() -> verify(verify -> {
                        verify.expectNoMoreInserted();

                        world.createEntity();
                    })).isInstanceOf(AssertionError.class)
                            .hasMessageContainingAll("Expected no more inserted", "<any components>");
                }

                @Test
                void testEntityInserted_WhenNoMoreInserted_EntityCreated_Throws() {
                    assertThatThrownBy(() -> verify(verify -> {
                        verify.expectNoMoreInserted();

                        world.createEntity(new C1(), new C2());
                    })).isInstanceOf(AssertionError.class)
                            .hasMessageContainingAll("Expected no more inserted", "C1", "C2");
                }

                @Test
                void testEntityInserted_WhenNoEntitiesInserted_Throws() {
                    assertThatThrownBy(() -> verify(verify -> verify.expectInserted(C1.class)))
                            .isInstanceOf(AssertionError.class)
                            .hasMessageContainingAll("Expected entity to be inserted", "C1");
                }

                @Test
                void testEntityInserted_WhenEmptyEntityCreated_Throws() {
                    assertThatThrownBy(() -> {
                        verify(verify -> verify.expectInserted(C1.class));

                        world.createEntity();
                    }).isInstanceOf(AssertionError.class)
                            .hasMessageContainingAll("Expected entity to be inserted", "C1");
                }

                @Test
                void testEntityInserted_WhenMismatchingEntityCreated_Throws() {
                    assertThatThrownBy(() -> {
                        verify(verify -> verify.expectInserted(C1.class));

                        world.createEntity(new C2());
                    }).isInstanceOf(AssertionError.class)
                            .hasMessageContainingAll("Expected entity to be inserted", "C1");
                }

                @Test
                void testEntityInserted() {
                    verify(verify -> {
                        verify.expectInserted(C1.class);

                        world.createEntity(new C1());
                    });
                }

                @Test
                void testEntityInserted_WhenMultipleExpected() {
                    verify(verify -> {
                        verify.expectInserted(C1.class);
                        verify.expectInserted(C2.class);

                        world.createEntity(new C1());
                        world.createEntity(new C2());
                    });
                }

                @Test
                void testEntityInserted_WhenMultipleExpected_OrderDoesNotMatter() {
                    verify(verify -> {
                        verify.expectInserted(C1.class);
                        verify.expectInserted(C2.class);

                        world.createEntity(new C2());
                        world.createEntity(new C1());
                    });
                }

                @Test
                void testEntityInserted_WhenMoreCreatedThanExpected_Passes() {
                    verify(verify -> {
                        verify.expectInserted(C1.class);

                        world.createEntity(new C1());
                        world.createEntity(new C1());
                    });
                }

                @Test
                void testEntityInserted_WhenNoMoreInserted_ReportsUnexpectedOnly() {
                    assertThatThrownBy(() -> verify(verify -> {
                        verify.expectInserted(C1.class);
                        verify.expectNoMoreInserted();

                        world.createEntity(new C1());
                        world.createEntity(new C2());
                    })).isInstanceOf(AssertionError.class)
                            .hasMessageContainingAll("Expected no more inserted", "C2");
                }

                @Test
                void testEntityInserted_WhenNoMoreInserted_ReportsUnexpectedOnly_OrderDoesNotMatter() {
                    assertThatThrownBy(() -> verify(verify -> {
                        verify.expectInserted(C1.class);
                        verify.expectNoMoreInserted();

                        world.createEntity(new C2());
                        world.createEntity(new C1());
                    })).isInstanceOf(AssertionError.class)
                            .hasMessageContainingAll("Expected no more inserted", "C2");
                }

                @Test
                void testEntityInserted_WhenNoMoreInserted_ReportsUnexpectedOnly_SameCompositionDoesNotMatter() {
                    assertThatThrownBy(() -> verify(verify -> {
                        verify.expectInserted(C1.class);
                        verify.expectNoMoreInserted();

                        world.createEntity(new C1());
                        world.createEntity(new C1());
                    })).isInstanceOf(AssertionError.class)
                            .hasMessageContainingAll("Expected no more inserted", "C1");
                }

            }

            @Nested
            class UpdatedTest {

                PooledComponentMapper<C1> mapper1;
                PooledComponentMapper<C2> mapper2;

                @BeforeEach
                void setupMappers() {
                    this.mapper1 = world.getPooledComponents(C1.class);
                    this.mapper2 = world.getPooledComponents(C2.class);
                }

                @Test
                void testEntityUpdated_WhenNoMoreUpdated_Throws() {
                    var entityId = world.createEntity();

                    assertThatThrownBy(() -> verify(verify -> {
                        verify.expectNoMoreUpdated();

                        mapper1.add(entityId);
                        world.process();
                    })).isInstanceOf(AssertionError.class)
                            .hasMessageContaining("Expected no more updated, but got entity %d".formatted(entityId));
                }

                @Test
                void testEntityUpdated_WhenNoMoreUpdated_WhenSomeExpected_Throws() {
                    var entityId = world.createEntity();
                    var entityId2 = world.createEntity();

                    assertThatThrownBy(() -> verify(verify -> {
                        verify.expectUpdated(entityId, C1.class);
                        verify.expectNoMoreUpdated();

                        mapper1.add(entityId);
                        mapper1.add(entityId2);
                        world.process();
                    })).isInstanceOf(AssertionError.class)
                            .hasMessageContaining("Expected no more updated, but got entity %d".formatted(entityId2))
                            .hasMessageNotContaining("Expected no more updated, but got entity %d".formatted(entityId));
                }

                @Test
                void testEntityUpdated_WhenNoComponentsExpected_Throws() {
                    var entityId = world.createEntity();

                    assertThatThrownBy(() -> verify(verify -> {
                        verify.expectUpdated(entityId);

                        mapper1.add(entityId);
                        world.process();
                    })).isInstanceOf(AssertionError.class)
                            .hasMessageContainingAll("Expected entity %d to have 0 components, but some were unexpected.".formatted(entityId), "C1");
                }

                @Test
                void testEntityUpdated_WhenMultipleComponentsAdded_Passes() {
                    var entityId = world.createEntity();

                    verify(verify -> {
                        verify.expectUpdated(entityId, C1.class, C2.class);

                        mapper1.add(entityId);
                        mapper2.add(entityId);
                        world.process();
                    });
                }

                @Test
                void testEntityUpdated_WhenMoreComponentsThanExpectedAdded_Throws() {
                    var entityId = world.createEntity();

                    assertThatThrownBy(() -> verify(verify -> {
                        verify.expectUpdated(entityId, C1.class);

                        mapper1.add(entityId);
                        mapper2.add(entityId);
                        world.process();
                    })).isInstanceOf(AssertionError.class)
                            .hasMessageNotContaining("C1")
                            .hasMessageContainingAll("Expected entity %d to have 1 components, but some were unexpected".formatted(entityId), "C2");
                }

                @Test
                void testEntityUpdated_WhenLessComponentsAdded_Throws() {
                    var entityId = world.createEntity();

                    assertThatThrownBy(() -> verify(verify -> {
                        verify.expectUpdated(entityId, C1.class, C2.class);

                        mapper1.add(entityId);
                        world.process();
                    })).isInstanceOf(AssertionError.class)
                            .hasMessageNotContaining("C1")
                            .hasMessageContainingAll("Expected entity %d to have 2 components, but some were missing".formatted(entityId), "C2");
                }

                @Test
                void testEntityUpdated_WhenNotUpdated_Throws() {
                    var entityId = world.createEntity();

                    assertThatThrownBy(() -> verify(verify -> {
                        verify.expectUpdated(entityId, C1.class, C2.class);
                    })).isInstanceOf(AssertionError.class)
                            .hasMessageContainingAll("Expected entity %d to be updated".formatted(entityId), "C1", "C2");
                }

                @Test
                void testEntityUpdated_MoreEntitiesUpdated_Passes() {
                    var entityId = world.createEntity();
                    var entityId2 = world.createEntity();

                    verify(verify -> {
                        verify.expectUpdated(entityId, C1.class);

                        mapper1.add(entityId);
                        mapper1.add(entityId2);
                        world.process();
                    });
                }

                @Test
                void testEntityUpdated_MoreEntitiesUpdated_Passes_OrderDoesNotMatter() {
                    var entityId = world.createEntity();
                    var entityId2 = world.createEntity();

                    verify(verify -> {
                        verify.expectUpdated(entityId2, C1.class);

                        mapper1.add(entityId);
                        mapper1.add(entityId2);
                        world.process();
                    });
                }

                @Test
                void testEntityUpdated() {
                    var entityId = world.createEntity();

                    verify(verify -> {
                        verify.expectUpdated(entityId, C1.class);

                        mapper1.add(entityId);
                        world.process();
                    });
                }

            }

            @Nested
            class RemovedTest {

                @Test
                void testEntityRemoved_WhenWorldNotProcessed_Throws() {
                    var entityId = world.createEntity();

                    assertThatThrownBy(() -> verify(verify -> {
                        verify.expectRemoved(entityId);

                        world.deleteEntity(entityId);
                    })).isInstanceOf(AssertionError.class)
                            .hasMessageContainingAll("Expected entity %d to be removed".formatted(entityId));
                }

                @Test
                void testEntityRemoved_WhenNoMoreRemoved_Throws() {
                    var entityId = world.createEntity();

                    assertThatThrownBy(() -> verify(verify -> {
                        verify.expectNoMoreRemoved();

                        world.deleteEntity(entityId);
                        world.process();
                    })).isInstanceOf(AssertionError.class)
                            .hasMessageContainingAll("Expected no more removed, but got entity %d".formatted(entityId));
                }

                @Test
                void testEntityRemoved_WhenNoMoreRemoved_WhenSomeExpected_Throws() {
                    var entityId = world.createEntity();
                    var entityId2 = world.createEntity();

                    assertThatThrownBy(() -> verify(verify -> {
                        verify.expectRemoved(entityId);
                        verify.expectNoMoreRemoved();

                        world.deleteEntity(entityId);
                        world.deleteEntity(entityId2);
                        world.process();
                    })).isInstanceOf(AssertionError.class)
                            .hasMessageNotContaining("Expected no more removed, but got entity %d".formatted(entityId))
                            .hasMessageContainingAll("Expected no more removed, but got entity %d".formatted(entityId2));
                }

                @Test
                void testEntityRemoved_WhenMoreRemoved_Passes() {
                    var entityId = world.createEntity();
                    var entityId2 = world.createEntity();

                    verify(verify -> {
                        verify.expectRemoved(entityId);

                        world.deleteEntity(entityId);
                        world.deleteEntity(entityId2);
                        world.process();
                    });
                }

                @Test
                void testEntityRemoved_WhenMoreRemoved_Passes_OrderDoesNotMatter() {
                    var entityId = world.createEntity();
                    var entityId2 = world.createEntity();

                    verify(verify -> {
                        verify.expectRemoved(entityId2);

                        world.deleteEntity(entityId);
                        world.deleteEntity(entityId2);
                        world.process();
                    });
                }

                @Test
                void testEntityRemoved() {
                    var entityId = world.createEntity();

                    verify(verify -> {
                        verify.expectRemoved(entityId);

                        world.deleteEntity(entityId);
                        world.process();
                    });
                }

            }

        }

    }

    public record C1() implements Pooled {
    }

    public record C2() implements Pooled {
    }

}
