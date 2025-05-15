package de.schosin.ecs.engine.events.builtin;

import de.schosin.ecs.api.components.ComponentType.ClassType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.engine.events.builtin.ComponentAddedEvent.RegularComponentAddedEvent.ClassComponentAddedEvent;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.utils.collections.Pool;

public sealed interface ComponentAddedEvent extends Event {

    RegularComponentType<?> type();

    Component<?> component();

    sealed interface RegularComponentAddedEvent extends ComponentAddedEvent {

        sealed interface ClassComponentAddedEvent extends RegularComponentAddedEvent {

            static <T> ClassComponentAddedEvent get(ClassType<T> type, Component<T> component) {
                return ClassComponentAddedEventImpl.get(type, component);
            }

        }

    }

}

abstract sealed class AbstractComponentAddedEvent implements ComponentAddedEvent {

    protected RegularComponentType<?> type;
    protected Component<?> component;

    @Override
    public RegularComponentType<?> type() {
        return type;
    }

    @Override
    public Component<?> component() {
        return component;
    }

    @Override
    public void reset() {
        this.type = null;
        this.component = null;
    }

}

final class ClassComponentAddedEventImpl extends AbstractComponentAddedEvent implements ClassComponentAddedEvent {

    private static final Pool<ClassComponentAddedEventImpl> POOL = Pool.unbounded(ClassComponentAddedEventImpl.class, ClassComponentAddedEventImpl::new);

    static <T> ClassComponentAddedEvent get(ClassType<T> type, Component<T> component) {
        var instance = POOL.getInstance();
        instance.type = type;
        instance.component = component;

        return instance;
    }

    @Override
    public void free() {
        POOL.free(this);
    }

}