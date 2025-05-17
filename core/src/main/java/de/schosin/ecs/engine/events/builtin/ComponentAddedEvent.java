package de.schosin.ecs.engine.events.builtin;

import de.schosin.ecs.api.components.ComponentType.ClassType;
import de.schosin.ecs.api.components.ComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.ComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.engine.events.builtin.ComponentAddedEvent.RegularComponentAddedEvent.ClassComponentAddedEvent;
import de.schosin.ecs.engine.events.builtin.ComponentAddedEvent.RegularComponentAddedEvent.ComponentRelationAddedEvent;
import de.schosin.ecs.engine.events.builtin.ComponentAddedEvent.RegularComponentAddedEvent.ExclusiveComponentRelationAddedEvent;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.utils.collections.Pool;

public sealed interface ComponentAddedEvent extends Event {

    static ComponentAddedEvent get(RegularComponentType<?, ?> type, Component<?, ?> component) {
        return switch (type) {
            case ClassType<?> classType -> ClassComponentAddedEventImpl.get(classType, component);
            case ComponentRelationType<?, ?> relationType -> ComponentRelationAddedEventImpl.get(relationType, component);
            case ExclusiveComponentRelationType<?, ?> exclusiveRelationType -> ExclusiveComponentRelationAddedEventImpl.get(exclusiveRelationType, component);
        };
    }

    RegularComponentType<?, ?> type();

    Component<?, ?> component();

    sealed interface RegularComponentAddedEvent extends ComponentAddedEvent {

        sealed interface ClassComponentAddedEvent extends RegularComponentAddedEvent {
            ClassType<?> type();
        }

        sealed interface ComponentRelationAddedEvent extends RegularComponentAddedEvent {
            ComponentRelationType<?, ?> type();
        }

        sealed interface ExclusiveComponentRelationAddedEvent extends RegularComponentAddedEvent {
            ExclusiveComponentRelationType<?, ?> type();
        }

    }

}

abstract sealed class AbstractComponentAddedEvent<T extends RegularComponentType<?, ?>> implements ComponentAddedEvent {

    protected T type;
    protected Component<?, ?> component;

    @Override
    public T type() {
        return type;
    }

    @Override
    public Component<?, ?> component() {
        return component;
    }

    @Override
    public void reset() {
        this.type = null;
        this.component = null;
    }

}

final class ClassComponentAddedEventImpl extends AbstractComponentAddedEvent<ClassType<?>> implements ClassComponentAddedEvent {

    private static final Pool<ClassComponentAddedEventImpl> POOL = Pool.unbounded(ClassComponentAddedEventImpl.class, ClassComponentAddedEventImpl::new);

    static ClassComponentAddedEvent get(ClassType<?> type, Component<?, ?> component) {
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

final class ComponentRelationAddedEventImpl extends AbstractComponentAddedEvent<ComponentRelationType<?, ?>> implements ComponentRelationAddedEvent {

    private static final Pool<ComponentRelationAddedEventImpl> POOL = Pool.unbounded(ComponentRelationAddedEventImpl.class, ComponentRelationAddedEventImpl::new);

    static ComponentRelationAddedEvent get(ComponentRelationType<?, ?> type, Component<?, ?> component) {
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

final class ExclusiveComponentRelationAddedEventImpl extends AbstractComponentAddedEvent<ExclusiveComponentRelationType<?, ?>> implements ExclusiveComponentRelationAddedEvent {

    private static final Pool<ExclusiveComponentRelationAddedEventImpl> POOL = Pool.unbounded(ExclusiveComponentRelationAddedEventImpl.class, ExclusiveComponentRelationAddedEventImpl::new);

    static ExclusiveComponentRelationAddedEvent get(ExclusiveComponentRelationType<?, ?> type, Component<?, ?> component) {
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
