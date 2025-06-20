package de.schosin.ecs.plugins.archetype;

import de.schosin.ecs.api.Plugin;
import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.plugins.archetype.ArchetypeSet.ArchetypeSetType;
import de.schosin.ecs.plugins.archetype.BaseArchetype.ArchetypeConsumer;

@Plugin(ArchetypeManager.class)
public interface ArchetypePlugin extends ArchetypeCreator {

    default <T extends ComponentSet<?>, C extends ArchetypeConsumer> ArchetypeSet<C> createArchetype(ArchetypeSetType<T, C> type) {
        return null;
    }

}
