package de.schosin.ecs.utils.junit5;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.AnnotationBasedArgumentsProvider;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.platform.commons.util.Preconditions;

public class SealedSubclassesArgumentsProvider extends AnnotationBasedArgumentsProvider<SealedSubclassesSource> {

    @Override
    protected Stream<? extends Arguments> provideArguments(ExtensionContext context, SealedSubclassesSource annotation) {
        return getSubclasses(context, annotation)
                .map(SealedSubclassesArgumentsProvider::toArgument);
    }

    private Stream<Class<?>> getSubclasses(ExtensionContext context, SealedSubclassesSource annotation) {
        var sealedClass = determineSealedClass(context, annotation);

        return getSubclasses(annotation, sealedClass);

    }

    private Stream<Class<?>> getSubclasses(SealedSubclassesSource annotation, Class<?> sealedClass) {
        Preconditions.condition(sealedClass.isSealed(), () -> String.format("Class '%s' is not sealed.", sealedClass));

        return switch (annotation.mode()) {
            case DIRECT -> provideDirectSubclasses(sealedClass);
            case FINAL -> provideFinalSubclasses(sealedClass);
            case NON_SEALED -> provideNonSealedSubclasses(sealedClass);
            case LEAF -> provideLeafSubclasses(sealedClass);
        };
    }

    private Stream<Class<?>> provideDirectSubclasses(Class<?> sealedClass) {
        return Arrays.stream(sealedClass.getPermittedSubclasses());
    }

    private Stream<Class<?>> provideFinalSubclasses(Class<?> sealedClass) {
        return walkTree(sealedClass, SealedSubclassesArgumentsProvider::isFinal);
    }

    private Stream<Class<?>> provideNonSealedSubclasses(Class<?> sealedClass) {
        return walkTree(sealedClass, SealedSubclassesArgumentsProvider::isNonSealed);
    }

    private Stream<Class<?>> provideLeafSubclasses(Class<?> sealedClass) {
        return walkTree(sealedClass, SealedSubclassesArgumentsProvider::isLeaf);
    }

    private Stream<Class<?>> walkTree(Class<?> clazz, Predicate<Class<?>> predicate) {
        if (clazz.isSealed()) {
            return Arrays.stream(clazz.getPermittedSubclasses())
                    .flatMap(subclass -> walkTree(subclass, predicate));
        }

        return predicate.test(clazz)
                ? Stream.of(clazz)
                : Stream.empty();
    }

    private static Arguments toArgument(Class<?> subclass) {
        return Arguments.arguments(Named.of(subclass.getSimpleName(), subclass));
    }

    private static boolean isFinal(Class<?> subclass) {
        return Modifier.isFinal(subclass.getModifiers());
    }

    private static boolean isNonSealed(Class<?> subclass) {
        // subclass known to be a permitted subclass.
        // subclasses must be either sealed, non-sealed or final,
        // so it is non-sealed if it is not sealed or final 
        return !subclass.isSealed() && !isFinal(subclass);
    }

    private static boolean isLeaf(Class<?> subclass) {
        return isFinal(subclass) || isNonSealed(subclass);
    }

    private Class<?> determineSealedClass(ExtensionContext context, SealedSubclassesSource annotation) {
        Class<?> sealedClass = annotation.value();

        if (Object.class.equals(sealedClass)) {
            Method method = context.getRequiredTestMethod();
            Type[] parameterTypes = method.getGenericParameterTypes();
            Preconditions.condition(parameterTypes.length > 0,
                    () -> "Test method must declare at least one parameter: " + method.toGenericString());

            Type firstParamType = parameterTypes[0];
            sealedClass = determineSealedClass(firstParamType, method.toGenericString());
        }

        return sealedClass;
    }

    private Class<?> determineSealedClass(Type type, String ctx) {
        Preconditions.condition(type instanceof ParameterizedType p && Class.class.equals(p.getRawType()),
                () -> "Parameter must reference an Class type (alternatively, use the annotation's 'value' attribute to specify the type explicitly): "
                        + ctx);

        return (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
    }

}
