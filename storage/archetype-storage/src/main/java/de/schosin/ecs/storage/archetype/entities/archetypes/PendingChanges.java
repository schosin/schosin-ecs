package de.schosin.ecs.storage.archetype.entities.archetypes;

import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relations;
import de.schosin.ecs.api.components.Relations.ComponentRelations;
import de.schosin.ecs.api.components.Relations.EntityRelations;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.archetype.components.ComponentIndex;
import de.schosin.ecs.storage.archetype.utils.results.ComponentRelationResultImpl;
import de.schosin.ecs.storage.archetype.utils.results.EntityRelationResultImpl;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.ImmutableIntBag;
import de.schosin.ecs.utils.collections.IntBag;

public class PendingChanges {

    private final ComponentIndex componentIndex;

    private final ArchetypeGraphNode archetypeNode;
    private ArchetypeGraphNode pendingArchetypeNode;

    private final IntBag addedIds = new IntBag(4);
    private final Bag<RegularComponentType<?, ?>> addedTypes = new Bag<>(RegularComponentType.class, 4);
    private final Bag<Object> added = new Bag<>(Object.class, 4);

    private final Bag<RegularComponentType<?, ?>> removedTypes = new Bag<>(RegularComponentType.class, 4);

    public PendingChanges(ComponentIndex componentIndex, ArchetypeGraphNode archetypeNode) {
        this.componentIndex = componentIndex;

        this.archetypeNode = archetypeNode;
        this.pendingArchetypeNode = archetypeNode;
    }

    public ArchetypeGraphNode getPendingArchetypeNode() {
        if (this.pendingArchetypeNode == archetypeNode) {
            return null;
        }

        return this.pendingArchetypeNode;
    }

    public boolean containsComponent(RegularComponentType<?, ?> type) {
        return addedTypes.contains(type);
    }

    @SuppressWarnings("unchecked")
    public <R> R getComponent(int componentId) {
        var index = addedIds.indexOf(componentId);
        if (index == -1) {
            return null;
        }

        return (R) added.get(index);
    }

    public boolean isNoAdded() {
        return this.addedTypes.isEmpty();
    }

    public boolean isEmpty() {
        return this.addedTypes.isEmpty() && this.removedTypes.isEmpty();
    }

    public ArchetypeGraphNode getArchetypeNode() {
        return this.archetypeNode;
    }

    public ImmutableIntBag getAddedIds() {
        return this.addedIds;
    }

    public ImmutableBag<RegularComponentType<?, ?>> getAddedTypes() {
        return this.addedTypes;
    }

    public ImmutableBag<Object> getAdded() {
        return this.added;
    }

    public ImmutableBag<RegularComponentType<?, ?>> getRemovedTypes() {
        return this.removedTypes;
    }

    /**
     * Add the component as a pending change. If a pending change is not needed, it will return false.
     * 
     * @param type type of component
     * @param component component instance
     * @return false if the component must be added immediately
     */
    public boolean add(RegularComponentType<?, ?> type, Object component) {
        // Validate
        if (type == null || component == null || !type.isInstance(component)) {
            reset(); // invalidate state
            throw new StorageEngineException("Expected component to match type '%s' but got: %s".formatted(type, component));
        }

        var componentId = componentIndex.getId(type);

        // Undo remove
        if (removedTypes.remove(type)) {
            pendingArchetypeNode = pendingArchetypeNode.addComponentType(type, componentId);
        }

        // Don't add if type already part of current archetype (no archetype change)
        if (archetypeNode.getComponentTypes().contains(type)) {
            return false;
        }

        // Update pending archetype node
        pendingArchetypeNode = pendingArchetypeNode.addComponentType(type, componentId);

        // Add component
        switch (type) {
            case ClassType<?> classType -> addClassComponent(classType, componentId, component);
            case ComponentRelationType<?, ?> relationType -> {
                if (!(component instanceof ComponentRelations<?, ?> relations)) {
                    addComponentRelation(relationType, componentId, (ComponentRelation<?, ?>) component);
                    return true;
                }

                for (int i = 0, s = relations.size(); i < s; i++) {
                    addComponentRelation(relationType, componentId, relations.get(i));
                }

                Relations.free(relations);
            }
            case ExclusiveComponentRelationType<?, ?> relationType -> addExclusiveComponentRelation(relationType, componentId, component);
            case EntityRelationType<?> relationType -> {
                if (!(component instanceof EntityRelations<?> relations)) {
                    addEntityRelation(relationType, componentId, (EntityRelation<?>) component);
                    return true;
                }

                for (int i = 0, s = relations.size(); i < s; i++) {
                    addEntityRelation(relationType, componentId, relations.get(i));
                }

                Relations.free(relations);
            }
            case ExclusiveEntityRelationType<?> relationType -> addExclusiveEntityRelation(relationType, componentId, (EntityRelation<?>) component);
        }

        return true;
    }

    private void addClassComponent(ClassType<?> type, int componentId, Object component) {
        var index = addedIds.indexOf(componentId);
        if (index > -1) {
            added.set(index, component);
            return;
        }

        // Add component
        addedIds.add(componentId);
        addedTypes.add(type);
        added.add(component);
    }

    private void addComponentRelation(ComponentRelationType<?, ?> relationType, int componentId, ComponentRelation<?, ?> relation) {
        var index = addedIds.indexOf(componentId);
        if (index > -1) {
            var result = (ComponentRelationResultImpl) added.get(index);
            result.add(relation);

            return;
        }

        var result = ComponentRelationResultImpl.getInstance();
        result.add(relation);

        addedIds.add(componentId);
        addedTypes.add(relationType);
        added.add(result);
    }

    private void addExclusiveComponentRelation(ExclusiveComponentRelationType<?, ?> relationType, int componentId, Object component) {
        // Remove matching relations from archetype
        var componentTypes = archetypeNode.getComponentTypes();

        for (int i = 0, s = componentTypes.getSize(); i < s; i++) {
            if (componentTypes.get(i) instanceof ExclusiveComponentRelationType<?, ?> other && other.relationship().equals(relationType.relationship())) {
                remove(other);
            }
        }

        // Remove matching relations in addedTypes
        for (int i = addedTypes.getSize() - 1; i >= 0; i--) {
            if (addedTypes.get(i) instanceof ExclusiveComponentRelationType<?, ?> other && other.relationship().equals(relationType.relationship())) {
                addedIds.removeIndex(i);
                addedTypes.remove(i);
                added.remove(i);
            }
        }

        // Add relation component
        addedIds.add(componentId);
        addedTypes.add(relationType);
        added.add(component);
    }

    private void addEntityRelation(EntityRelationType<?> relationType, int componentId, EntityRelation<?> relation) {

        var index = addedIds.indexOf(componentId);
        if (index > -1) {
            var result = (EntityRelationResultImpl) added.get(index);
            result.add(relation);

            return;
        }

        var result = EntityRelationResultImpl.getInstance();
        result.add(relation);

        addedIds.add(componentId);
        addedTypes.add(relationType);
        added.add(result);
    }

    private void addExclusiveEntityRelation(ExclusiveEntityRelationType<?> relationType, int componentId, EntityRelation<?> component) {
        // Remove matching relations in addedTypes
        for (int i = addedTypes.getSize() - 1; i >= 0; i--) {
            if (addedTypes.get(i) instanceof ExclusiveEntityRelationType<?> other && other.relationship().equals(relationType.relationship())) {
                addedIds.removeIndex(i);
                addedTypes.remove(i);
                added.remove(i);
            }
        }

        // Add relation component
        addedIds.add(componentId);
        addedTypes.add(relationType);
        added.add(component);
    }

    public void remove(RegularComponentType<?, ?> type) {
        var archetypeType = archetypeNode.getComponentTypes().contains(type);

        // Skip if no-op (not part of archetype or added types)
        if (!addedTypes.contains(type) && !archetypeType) {
            return;
        }

        // Add remove
        if (archetypeType && !removedTypes.contains(type)) {
            removedTypes.add(type);
            pendingArchetypeNode = pendingArchetypeNode.removeComponentType(type);
        }

        // Undo adds
        for (int i = addedTypes.getSize() - 1; i >= 0; i--) {
            if (addedTypes.get(i).equals(type)) {
                addedIds.removeIndex(i);
                addedTypes.remove(i);
                added.remove(i);

                pendingArchetypeNode = pendingArchetypeNode.removeComponentType(type);

                break;
            }
        }
    }

    public void reset() {
        addedIds.clear();
        addedTypes.clear();
        added.clear();

        removedTypes.clear();

        this.pendingArchetypeNode = archetypeNode;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("PendingChanges(archetypeNode = ").append(this.archetypeNode)
                .append(", addedTypes = ").append(this.addedTypes)
                .append(", removedTypes = ").append(this.removedTypes)
                .append(")")
                .toString();
    }

}
