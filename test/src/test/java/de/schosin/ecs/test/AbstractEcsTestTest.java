package de.schosin.ecs.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.World;

class AbstractEcsTestTest {

    public interface CustomWorld extends World {
        default int getAnswer() {
            return 42;
        }
    }

    @Nested
    class WorldTypeParameterTest extends AbstractEcsTest<CustomWorld> {

        @Nested
        class ExtendsDirectly extends AbstractEcsTest<CustomWorld> {

            @Test
            void testCustomWorld() {
                assertThat(world).isNotNull();
                assertThat(world.getAnswer()).isEqualTo(42);
            }

        }

        @Nested
        class RawType {

            @SuppressWarnings("rawtypes")
            class RawTypeImpl extends AbstractEcsTest {
            }

            @Test
            void testRawType() {
                var impl = new RawTypeImpl();

                assertThatThrownBy(() -> impl.createWorld())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Could not detect type of world");
            }

            @Test
            void testRawType_WhenCreateWorldOverriden_Passes() {
                var impl = new RawTypeImpl() {
                    @Override
                    protected World createWorld() {
                        return World.builder(CustomWorld.class).build();
                    }
                };

                assertThat(impl.createWorld()).isInstanceOf(CustomWorld.class);
            }

        }

        @Nested
        class ExtendsIndirectly extends AbstractExtendsIndirectly {

            @Test
            void testCustomWorld() {
                assertThat(world).isNotNull();
                assertThat(world.getAnswer()).isEqualTo(42);
            }

        }

        @Nested
        class ExtendsIndirectlyGeneric extends AbstractExtendsIndirectlyGeneric<String> {

            @Test
            void testCustomWorld() {
                assertThat(world).isNotNull();
                assertThat(world.getAnswer()).isEqualTo(42);
            }

        }

        abstract class AbstractExtendsIndirectly extends AbstractEcsTest<CustomWorld> {
        }

        @SuppressWarnings("unused")
        abstract class AbstractExtendsIndirectlyGeneric<T> extends AbstractEcsTest<CustomWorld> {
        }

    }

}
