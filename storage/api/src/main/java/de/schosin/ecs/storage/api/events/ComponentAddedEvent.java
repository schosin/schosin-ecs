package de.schosin.ecs.storage.api.events;

import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.events.ComponentAddedEvent.RegularComponentAddedEvent.ClassComponentAddedEvent;
import de.schosin.ecs.storage.api.events.ComponentAddedEvent.RegularComponentAddedEvent.ComponentRelationAddedEvent;
import de.schosin.ecs.storage.api.events.ComponentAddedEvent.RegularComponentAddedEvent.EntityRelationAddedEvent;
import de.schosin.ecs.storage.api.events.ComponentAddedEvent.RegularComponentAddedEvent.ExclusiveComponentRelationAddedEvent;
import de.schosin.ecs.storage.api.events.ComponentAddedEvent.RegularComponentAddedEvent.ExclusiveEntityRelationAddedEvent;
import de.schosin.ecs.utils.collections.Pool;

public sealed interface ComponentAddedEvent extends StorageEvent {

    static ComponentAddedEvent get(int componentId, RegularComponentType<?, ?> type, Component<?, ?> component) {
        return switch (type) {
            case ClassType<?> classType -> ClassComponentAddedEventImpl.get(componentId, classType, component);
            case ComponentRelationType<?, ?> relationType -> ComponentRelationAddedEventImpl.get(componentId, relationType, component);
            case ExclusiveComponentRelationType<?, ?> exclusiveRelationType -> ExclusiveComponentRelationAddedEventImpl.get(componentId, exclusiveRelationType, component);
            case EntityRelationType<?> relationType -> EntityRelationAddedEventImpl.get(componentId, relationType, component);
            case ExclusiveEntityRelationType<?> exclusiveRelationType -> ExclusiveEntityRelationAddedEventImpl.get(componentId, exclusiveRelationType, component);
        };
    }

    int componentId();

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

        sealed interface EntityRelationAddedEvent extends RegularComponentAddedEvent {
            EntityRelationType<?> type();
        }

        sealed interface ExclusiveEntityRelationAddedEvent extends RegularComponentAddedEvent {
            ExclusiveEntityRelationType<?> type();
        }

    }

}

abstract sealed class AbstractComponentAddedEvent<T extends RegularComponentType<?, ?>> implements ComponentAddedEvent {

    protected int componentId = -1;
    protected T type;
    protected Component<?, ?> component;

    @Override
    public int componentId() {
        return componentId;
    }

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
        this.componentId = -1;
        this.type = null;
        this.component = null;
    }

}

final class ClassComponentAddedEventImpl extends AbstractComponentAddedEvent<ClassType<?>> implements ClassComponentAddedEvent {

    private static final Pool<ClassComponentAddedEventImpl> POOL = Pool.unbounded(ClassComponentAddedEventImpl.class, ClassComponentAddedEventImpl::new);

    static ClassComponentAddedEvent get(int componentId, ClassType<?> type, Component<?, ?> component) {
        var instance = POOL.getInstance();
        instance.componentId = componentId;
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

    static ComponentRelationAddedEvent get(int componentId, ComponentRelationType<?, ?> type, Component<?, ?> component) {
        var instance = POOL.getInstance();
        instance.componentId = componentId;
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

    static ExclusiveComponentRelationAddedEvent get(int componentId, ExclusiveComponentRelationType<?, ?> type, Component<?, ?> component) {
        var instance = POOL.getInstance();
        instance.componentId = componentId;
        instance.type = type;
        instance.component = component;

        return instance;
    }

    @Override
    public void free() {
        POOL.free(this);
    }

}

final class EntityRelationAddedEventImpl extends AbstractComponentAddedEvent<EntityRelationType<?>> implements EntityRelationAddedEvent {

    private static final Pool<EntityRelationAddedEventImpl> POOL = Pool.unbounded(EntityRelationAddedEventImpl.class, EntityRelationAddedEventImpl::new);

    static EntityRelationAddedEvent get(int componentId, EntityRelationType<?> type, Component<?, ?> component) {
        var instance = POOL.getInstance();
        instance.componentId = componentId;
        instance.type = type;
        instance.component = component;

        return instance;
    }

    @Override
    public void free() {
        POOL.free(this);
    }

}

final class ExclusiveEntityRelationAddedEventImpl extends AbstractComponentAddedEvent<ExclusiveEntityRelationType<?>> implements ExclusiveEntityRelationAddedEvent {

    private static final Pool<ExclusiveEntityRelationAddedEventImpl> POOL = Pool.unbounded(ExclusiveEntityRelationAddedEventImpl.class, ExclusiveEntityRelationAddedEventImpl::new);

    static ExclusiveEntityRelationAddedEvent get(int componentId, ExclusiveEntityRelationType<?> type, Component<?, ?> component) {
        var instance = POOL.getInstance();
        instance.componentId = componentId;
        instance.type = type;
        instance.component = component;

        return instance;
    }

    @Override
    public void free() {
        POOL.free(this);
    }

}
