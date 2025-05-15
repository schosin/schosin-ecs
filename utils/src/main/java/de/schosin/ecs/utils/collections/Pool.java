package de.schosin.ecs.utils.collections;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.NullMarked;

import de.schosin.ecs.api.Pooled;

@NullMarked
public sealed interface Pool<T> {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static <T> Pool<T> bounded(int limit, Class<? super T> clazz, Supplier<T> constructor) {
        if (Pooled.class.isAssignableFrom(clazz)) {
            return new BoundedPoolImpl<>(limit, clazz, constructor, instance -> ((Pooled) instance).reset());
        }

        return new BoundedPoolImpl(limit, clazz, constructor, null);
    }

    static <T> Pool<T> bounded(int limit, Class<? super T> clazz, Supplier<T> constructor, Consumer<T> reset) {
        return new BoundedPoolImpl<>(limit, clazz, constructor, reset);
    }

    static <T> Pool<T> unbounded(Class<? super T> clazz, Supplier<T> constructor) {
        if (Pooled.class.isAssignableFrom(clazz)) {
            return new PoolImpl<>(clazz, constructor, instance -> ((Pooled) instance).reset());
        }

        return new PoolImpl<>(clazz, constructor, null);
    }

    static <T> Pool<T> unbounded(Class<? super T> clazz, Supplier<T> constructor, Consumer<T> reset) {
        return new PoolImpl<>(clazz, constructor, reset);
    }

    <R> R withInstance(Function<T, R> func);

    void withInstanceNoResult(Consumer<T> consumer);

    T getInstance();

    void free(T instance);

}

final class BoundedPoolImpl<T> extends PoolImpl<T> {

    private final int limit;

    protected BoundedPoolImpl(int limit, Class<? super T> clazz, Supplier<T> constructor, Consumer<T> reset) {
        super(clazz, constructor, reset);

        this.limit = limit;
    }

    @Override
    public void free(T instance) {
        if (data.getSize() < limit) {
            super.free(instance);
        }
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("BoundedPoolImpl(size = ").append(data.getSize())
                .append(", limit = ").append(limit)
                .toString();
    }

}

sealed class PoolImpl<T> implements Pool<T> {

    protected final Bag<? super T> data;
    private final Supplier<T> constructor;
    private final Consumer<T> reset;

    protected PoolImpl(Class<? super T> clazz, Supplier<T> constructor, Consumer<T> reset) {
        this.data = new Bag<>(clazz, 1024);
        this.constructor = constructor;
        this.reset = reset;
    }

    public <R> R withInstance(Function<T, R> func) {
        var instance = getInstance();
        try {
            return func.apply(instance);
        } finally {
            free(instance);
        }
    }

    public void withInstanceNoResult(Consumer<T> consumer) {
        var instance = getInstance();
        try {
            consumer.accept(instance);
        } finally {
            free(instance);
        }
    }

    @SuppressWarnings("unchecked")
    public T getInstance() {
        if (!data.isEmpty()) {
            synchronized (data) {
                if (!data.isEmpty()) {
                    return (T) data.removeLast();
                }
            }
        }

        return constructor.get();
    }

    public void free(T instance) {
        if (reset != null) {
            reset.accept(instance);
        }

        synchronized (data) {
            this.data.add(instance);
        }
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("PoolImpl(size = ").append(data.getSize())
                .toString();
    }

}