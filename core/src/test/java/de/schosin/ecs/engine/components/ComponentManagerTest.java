package de.schosin.ecs.engine.components;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

import de.schosin.ecs.engine.AbstractWorldTest;
import de.schosin.ecs.engine.utils.ClassUtils;
import de.schosin.ecs.engine.utils.ClassUtils.ClassType;

class ComponentManagerTest extends AbstractWorldTest {

    enum TypeTest {

        valid_class(ValidClass.class),
        valid_record(ValidRecord.class),
        valid_enum(ValidEnum.class),

        invalid_generic_class(InvalidGenericClass.class),
        invalid_abstract(InvalidAbstractClass.class),
        invalid_interface(InvalidInterface.class),
        invalid_annotation(InvalidAnnotation.class),
        invalid_synthetic(((Runnable) ComponentManagerTest::invalidSynthethic).getClass()),

        invalid_primitive_array(int[].class),
        invalid_string_array(String[].class),

        invalid_boolean(boolean.class),
        invalid_byte(byte.class),
        invalid_char(char.class),
        invalid_short(short.class),
        invalid_int(int.class),
        invalid_long(long.class),
        invalid_float(float.class),
        invalid_double(double.class),
        invalid_boolean_wrapper(Boolean.class),
        invalid_byte_wrapper(Byte.class),
        invalid_character_wrapper(Character.class),
        invalid_short_wrapper(Short.class),
        invalid_integer_wrapper(Integer.class),
        invalid_long_wrapper(Long.class),
        invalid_float_wrapper(Float.class),
        invalid_double_wrapper(Double.class),
        invalid_String(String.class);

        private final Class<?> component;

        private TypeTest(Class<?> component) {
            this.component = component;
        }

    }

    @ParameterizedTest
    @EnumSource(ClassType.class)
    void verityTypeTest(ClassType type) {
        for (var test : TypeTest.values()) {
            var testType = ClassUtils.detectType(test.component);
            if (testType == type) {
                return;
            }
        }

        fail("ClassType '%s' not tested.", type);
    }

    @ParameterizedTest
    @EnumSource(value = TypeTest.class, names = "valid_.+", mode = Mode.MATCH_ALL)
    void testValid(TypeTest test) {
        assertThatCode(() -> componentManager.getData(test.component)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = TypeTest.class, names = "valid_.+", mode = Mode.MATCH_NONE)
    void testInvalid(TypeTest test) {
        assertThatThrownBy(() -> componentManager.getData(test.component))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll(test.component.getSimpleName(), "Invalid component", "Allowed types");
    }

    @Test
    void testExtendedComponent_ParentFirst() {
        componentManager.getData(ValidClass.class);

        assertThatThrownBy(() -> componentManager.getData(ExtendedClass.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContainingAll("Extending ", "not supported", ValidClass.class.getSimpleName(), ExtendedClass.class.getSimpleName());
    }

    @Test
    void testExtendedComponent_ParentSecond() {
        componentManager.getData(ExtendedClass.class);

        assertThatThrownBy(() -> componentManager.getData(ValidClass.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContainingAll("Extending ", "not supported", ValidClass.class.getSimpleName(), ExtendedClass.class.getSimpleName());
    }

    private static void invalidSynthethic() {
    }

    public static class ValidClass {
    }

    public static class ExtendedClass extends ValidClass {
    }

    @SuppressWarnings("unused")
    public static class InvalidGenericClass<T> {
    }

    public static abstract class InvalidAbstractClass {
    }

    public record ValidRecord() {
    }

    public enum ValidEnum {
        INSTANCE
    }

    public interface InvalidInterface {
    }

    public @interface InvalidAnnotation {
    }

}
