package de.schosin.ecs.storage.api.components;

import java.util.function.Supplier;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;

public sealed interface ComponentProvider {

    RegularComponentType<?, ?> type();

    record PooledComponent(ClassType<? extends Pooled> type) implements ComponentProvider {
    }

    record EnumComponent(ClassType<? extends Enum<?>> type, Enum<?> component) implements ComponentProvider {
    }

    record SuppliedComponent(RegularComponentType<?, ?> type, Supplier<?> supplier) implements ComponentProvider {
    }

}
