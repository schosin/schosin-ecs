package de.schosin.ecs.engine.components;

import java.util.Iterator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Result.ComponentResult;
import de.schosin.ecs.api.components.mappers.Components.ComponentMapper;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BagIterator;

public class ComponentResultImpl<T> implements ComponentResult<T>, Pooled {

    private final Class<T> clazz;
    private final Bag<ComponentMapper<? extends T>> mappers;
    private final Bag<T> components;

    private final ThreadLocal<BagIterator<T>> iterator = ThreadLocal.withInitial(BagIterator::new);

    private int entityId = -1;
    private int size = -1;

    public ComponentResultImpl(Class<T> clazz, Bag<ComponentMapper<? extends T>> mappers) {
        this.clazz = clazz;
        this.mappers = mappers;
        this.components = new Bag<>(clazz, 4);
    }

    public ComponentResultImpl<T> init(int entityId) {
        this.entityId = entityId;

        return this;
    }

    @NonNull
    @Override
    public T get(int i) {
        if (i >= size()) {
            throw new ArrayIndexOutOfBoundsException(i);
        }

        return this.components.get(i);
    }

    @Nullable
    @Override
    public <R extends T> R get(Class<R> clazz) {
        var s = size(); // initializes components, must be done before getData

        var data = components.getData();
        for (int i = 0; i < s; i++) {
            var component = data[i];
            if (clazz == component.getClass()) {
                return clazz.cast(component);
            }
        }

        return null;
    }

    @Override
    public int size() {
        if (size == -1) {
            var data = mappers.getData();
            for (int i = 0, s = mappers.getSize(); i < s; i++) {
                var component = data[i].get(entityId);
                if (component != null) {
                    components.add(clazz.cast(component));
                }
            }

            this.size = components.getSize();
        }

        return size;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public Iterator<T> iterator() {
        size();
        return iterator.get().init(this.components);
    }

    @Override
    public void reset() {
        this.entityId = -1;
        this.size = -1;

        this.components.clear();
    }

}
