package de.schosin.ecs.plugins.archetype;

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.types.ComponentSetType;
import de.schosin.ecs.plugins.archetype.BaseArchetype.ArchetypeConsumer;

public interface ArchetypeSet<C extends ArchetypeConsumer> extends BaseArchetype<C> {

    record ArchetypeSetType<T extends ComponentSet<?>, C extends ArchetypeConsumer>(ComponentSetType<T, ?> componentType, Class<C> consumerClazz) {
    }

}
