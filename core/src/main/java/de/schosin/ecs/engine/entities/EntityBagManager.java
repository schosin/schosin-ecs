package de.schosin.ecs.engine.entities;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.entities.ImmutableEntityBag;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.events.ArchetypeAddedEvent;

public class EntityBagManager {

    private final StorageEngine storageEngine;
    private final EntityManager entityManager;
    private final ComponentMapperManager componentMapperManager;

    private final Map<Set<RegularComponentType<?, ?>>, ArchetypeEntityBag> archetypeBags = new ConcurrentHashMap<>();

    public EntityBagManager(StorageEngine storageEngine, EntityManager entityManager, ComponentMapperManager componentMapperManager, EventManager eventManager) {
        this.storageEngine = storageEngine;
        this.entityManager = entityManager;
        this.componentMapperManager = componentMapperManager;

        eventManager.registerEventHandler(ArchetypeAddedEvent.class, this::handleArchetypeAdded);
    }

    private void handleArchetypeAdded(ArchetypeAddedEvent event) {
        var archetype = event.archetype();

        for (var bag : archetypeBags.values()) {
            bag.handleArchetype(archetype);
        }
    }

    public ImmutableEntityBag getAllEntities() {
        return getEntities(Set.of());
    }

    public ImmutableEntityBag getEntities(Class<?>... componentTypes) {
        return getEntities(Arrays.stream(componentTypes).map(ComponentType::component).collect(Collectors.toSet()));
    }

    public ImmutableEntityBag getEntities(RegularComponentType<?, ?>... componentTypes) {
        return getEntities(Set.of(componentTypes));
    }

    private ImmutableEntityBag getEntities(Set<RegularComponentType<?, ?>> componentTypes) {
        return archetypeBags.computeIfAbsent(componentTypes, this::createArchetypeEntityBag);
    }

    private ArchetypeEntityBag createArchetypeEntityBag(Set<RegularComponentType<?, ?>> componentTypes) {
        var bag = new ArchetypeEntityBag(entityManager, componentMapperManager, componentTypes);

        for (var archetype : storageEngine.getArchetypes()) {
            bag.handleArchetype(archetype);
        }

        return bag;
    }

}
