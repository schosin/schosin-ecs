package de.schosin.ecs.api.components.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.Relation.EntityRelationship;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Relation.Relationship;
import de.schosin.ecs.api.components.Relation.Target;
import de.schosin.ecs.api.components.types.AbstractComponentTypeTest.MatchesTestCase;
import de.schosin.ecs.api.data.DataProcessor;

public abstract class AbstractComponentTypeTest<T extends Enum<T> & MatchesTestCase> {

    protected static final ClassType<EnumComponent> FETCH = ComponentType.component(EnumComponent.class);

    protected static final Class<?> SYNTHETIC_CLASS = ((Runnable) () -> {
    }).getClass();

    private static final Set<Class<? extends ComponentType<?, ?>>> EXPECTED_COMPONENT_TYPES = resolveComponentTypeClasses();

    protected final Class<T> matchesTestCases;

    protected AbstractComponentTypeTest(Class<T> matchesTestCases) {
        this.matchesTestCases = matchesTestCases;
    }

    interface MatchesTestCase {

        String name();

        ComponentType<?, ?> type();

        ComponentType<?, ?> otherType();

        boolean matches();

    }

    @Test
    void verifyMatchesTestCases() {
        var expected = new HashSet<>(EXPECTED_COMPONENT_TYPES);

        for (var testCase : matchesTestCases.getEnumConstants()) {
            expected.remove(testCase.otherType().getClass());
        }

        assertThat(expected).as("must cover all component types").isEmpty();
    }

    @TestFactory
    Stream<DynamicTest> testMatches() {
        return Arrays.stream(matchesTestCases.getEnumConstants())
                .map(this::testMatches);
    }

    DynamicTest testMatches(MatchesTestCase testCase) {
        var type = testCase.type();
        var otherType = testCase.otherType();
        var matches = testCase.matches();

        var description = "%s %s %s".formatted(type, matches ? "matches" : "does not match", otherType);
        return DynamicTest.dynamicTest(testCase.name(), () -> assertThat(type.matches(otherType)).as(description).isEqualTo(matches));
    }

    abstract class CommonComponentTest {

        protected abstract ComponentType<?, ?> type(Class<?> clazz);

        @ParameterizedTest
        @ValueSource(classes = { GenericComponent.class, ComponentInterface.class, AbstractComponent.class, Object.class, int[].class, Integer[].class, Object[].class })
        void testInvalidType(Class<?> clazz) {
            assertThatThrownBy(() -> type(clazz))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(clazz.getName(), "cannot be used as a component");
        }

        @ParameterizedTest
        @MethodSource("de.schosin.ecs.api.components.types.AbstractComponentTypeTest#unsupportedTypes")
        void testUnsupportedClassType(Class<?> clazz) {
            assertThatThrownBy(() -> type(clazz))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(clazz.getName(), "cannot be used as a component");
        }

        @Test
        void testInvalidSyntheticClass() {
            assertThatThrownBy(() -> type(SYNTHETIC_CLASS))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(SYNTHETIC_CLASS.getName(), "must not be synthetic");
        }

        @Test
        void testComponentSet() {
            assertThatThrownBy(() -> type(MyComponentSet.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(MyComponentSet.class.getName(), "cannot be used as a component", "must not be component sets");
        }

    }

    static Stream<Arguments> unsupportedTypes() {
        return ComponentTypeHelper.UNSUPPORTED_TYPES.stream()
                .map(clazz -> Arguments.of(Named.of(clazz.getSimpleName(), clazz)));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Set<Class<? extends ComponentType<?, ?>>> resolveComponentTypeClasses() {
        return Set.copyOf(resolveComponentTypeClasses((Class) ComponentType.class, new HashSet<>()));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Set<Class<? extends ComponentType<?, ?>>> resolveComponentTypeClasses(Class<? extends ComponentType<?, ?>> clazz, Set<Class<? extends ComponentType<?, ?>>> result) {
        if (Modifier.isFinal(clazz.getModifiers())) {
            result.add(clazz);
            return result;
        }

        if (CustomComponentType.class.equals(clazz)) {
            return result;
        }

        var subclasses = clazz.getPermittedSubclasses();
        if (subclasses == null || subclasses.length == 0) {
            throw new IllegalStateException("Class '%s' is neither final, nor has permitted subclasses. ComponentType hierarchy broken.".formatted(clazz.getName()));
        }

        for (var subclass : subclasses) {
            resolveComponentTypeClasses((Class) subclass, result);
        }

        return result;
    }

    enum EnumComponent {
        A, B
    }

    record Component(String value) {
    }

    record GenericComponent<T>(T value) {
    }

    interface ComponentInterface {
    }

    @SuppressWarnings("unused")
    interface GenericComponentInterface<T> {
    }

    abstract class AbstractComponent {
    }

    class NonFinalComponent {
    }

    final class FinalComponent implements ComponentInterface {
    }

    enum RelationshipComponent implements Relationship {
    }

    enum ExclusiveComponent implements Exclusive {
    }

    enum EntityRelationshipComponent implements EntityRelationship {
    }

    enum ExclusiveEntityRelationship implements EntityRelationship, Exclusive {
    }

    enum TargetComponent implements Target {
    }

    interface RelationshipWildcard extends Relationship {
    }

    interface TargetWildcard extends Target {
    }

    interface ExclusiveWildcard extends Exclusive {
    }

    interface EntityRelationshipWildcard extends EntityRelationship {
    }

    interface ExclusiveEntityRelationshipWildcard extends EntityRelationship, Exclusive {
    }

    interface MyComponentSet extends ComponentSet<MyComponentSet.Processor> {
        interface Processor extends DataProcessor<MyComponentSet> {
        }

        Component component();
    }

    interface OtherComponentSet extends ComponentSet<OtherComponentSet.Processor> {
        interface Processor extends DataProcessor<OtherComponentSet> {
        }

        Component component();
    }

}
