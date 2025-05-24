package de.schosin.ecs.api.components.types;

import static de.schosin.ecs.api.components.types.ComponentType.componentSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.types.AbstractComponentTypeTest.MyComponentSet;
import de.schosin.ecs.api.components.types.AbstractComponentTypeTest.MyComponentSetClass;

class ComponentSetTypeTest {

    @Test
    void testToString() {
        assertThat(componentSet(MyComponentSet.class))
                .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                .containsSubsequence("ComponentSetType", MyComponentSet.class.getSimpleName());
    }

    @Test
    void testComponentSet() {
        assertThatCode(() -> new ComponentSetType<>(MyComponentSet.class)).doesNotThrowAnyException();
    }

    @Test
    void testComponentSetImplementation_DoesThrow() {
        assertThatCode(() -> new ComponentSetType<>(MyComponentSetClass.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll(MyComponentSetClass.class.getName(), "cannot be used as a component set", "must be interfaces");
    }

}
