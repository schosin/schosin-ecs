package de.schosin.ecs.plugins.composition.manager.components.defaultcomponent;

import de.schosin.ecs.api.components.mappers.CustomComponentMapper;

public sealed interface DefaultComponents<T> extends CustomComponentMapper<T, T> permits DefaultComponentsImpl {
}
