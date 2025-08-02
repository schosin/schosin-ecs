package de.schosin.ecs.storage.archetype.entities.archetypes;

import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relations;
import de.schosin.ecs.api.components.Relations.ComponentRelations;
import de.schosin.ecs.api.components.Relations.EntityRelations;
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
    public <R> R getComponent(RegularComponentType<?, R> type) {
        var index = addedTypes.indexOf(type);
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

        // Undo remove
        if (removedTypes.remove(type)) {
            pendingArchetypeNode = pendingArchetypeNode.removeComponentType(type);
        }

        // Don't add if type already part of current archetype (no archetype change)
        if (archetypeNode.getComponentTypes().contains(type)) {
            return false;
        }

        // Update pending archetype node
        pendingArchetypeNode = pendingArchetypeNode.addComponentType(type);

        // Add component
        switch (type) {
            case ComponentRelationType<?, ?> relationType -> {
                if (!(component instanceof ComponentRelations<?, ?> relations)) {
                    addComponentRelation(relationType, (ComponentRelation<?, ?>) component);
                    return true;
                }

                for (int i = 0, s = relations.size(); i < s; i++) {
                    addComponentRelation(relationType, relations.get(i));
                }

                Relations.free(relations);
            }
            case ExclusiveComponentRelationType<?, ?> relationType -> addExclusiveComponentRelation(relationType, component);
            case EntityRelationType<?> relationType -> {
                if (!(component instanceof EntityRelations<?> relations)) {
                    addEntityRelation(relationType, (EntityRelation<?>) component);
                    return true;
                }

                for (int i = 0, s = relations.size(); i < s; i++) {
                    addEntityRelation(relationType, relations.get(i));
                }

                Relations.free(relations);
            }
            case ExclusiveEntityRelationType<?> relationType -> addExclusiveEntityRelation(relationType, (EntityRelation<?>) component);
            default -> addRegularComponent(type, component);
        }

        return true;
    }

    private void addComponentRelation(ComponentRelationType<?, ?> relationType, ComponentRelation<?, ?> relation) {
        // Resolve result, creating if not present yet
        ComponentRelationResultImpl result = null;

        for (int i = 0, s = addedTypes.getSize(); i < s; i++) {
            if (addedTypes.get(i).equals(relationType)) {
                result = (ComponentRelationResultImpl) added.get(i);
            }
        }

        if (result == null) {
            result = ComponentRelationResultImpl.getInstance();

            addedIds.add(componentIndex.getId(relationType));
            addedTypes.add(relationType);
            added.add(result);
        }

        // Add relation
        result.add(relation);
    }

    private void addExclusiveComponentRelation(ExclusiveComponentRelationType<?, ?> relationType, Object component) {
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
        addedIds.add(componentIndex.getId(relationType));
        addedTypes.add(relationType);
        added.add(component);
    }

    private void addEntityRelation(EntityRelationType<?> relationType, EntityRelation<?> relation) {
        // Resolve result, creating if not present yet
        EntityRelationResultImpl result = null;

        for (int i = 0, s = addedTypes.getSize(); i < s; i++) {
            if (addedTypes.get(i).equals(relationType)) {
                result = (EntityRelationResultImpl) added.get(i);
            }
        }

        if (result == null) {
            result = EntityRelationResultImpl.getInstance();

            addedIds.add(componentIndex.getId(relationType));
            addedTypes.add(relationType);
            added.add(result);
        }

        // Add relation
        result.add(relation);
    }

    private void addExclusiveEntityRelation(ExclusiveEntityRelationType<?> relationType, EntityRelation<?> component) {
        // Remove matching relations in addedTypes
        for (int i = addedTypes.getSize() - 1; i >= 0; i--) {
            if (addedTypes.get(i) instanceof ExclusiveEntityRelationType<?> other && other.relationship().equals(relationType.relationship())) {
                addedIds.removeIndex(i);
                addedTypes.remove(i);
                added.remove(i);
            }
        }

        // Add relation component
        addedIds.add(componentIndex.getId(relationType));
        addedTypes.add(relationType);
        added.add(component);
    }

    private void addRegularComponent(RegularComponentType<?, ?> type, Object component) {
        // Replace already added (same component added multiple times)
        for (int i = 0, s = addedTypes.getSize(); i < s; i++) {
            var addedType = addedTypes.get(i);
            if (!addedType.equals(type)) {
                continue;
            }

            // Don't replace non-exclusive component relation if different target
            if (component instanceof ComponentRelation<?, ?> relation && !((ComponentRelation<?, ?>) added.get(i)).target().equals(relation.target())) {
                continue;
            }

            // Don't replace non-exclusive entity relation if different target
            if (component instanceof EntityRelation<?> relation && ((EntityRelation<?>) added.get(i)).target() != relation.target()) {
                continue;
            }

            // Replace added component with same type
            added.set(i, component);
            return;
        }

        // Add component
        addedIds.add(componentIndex.getId(type));
        addedTypes.add(type);
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
