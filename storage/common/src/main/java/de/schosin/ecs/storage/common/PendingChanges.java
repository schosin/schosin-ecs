package de.schosin.ecs.storage.common;

import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.storage.common.results.ComponentRelationResultImpl;
import de.schosin.ecs.storage.common.results.EntityRelationResultImpl;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;

public class PendingChanges {

    private ComponentMask componentMask;

    private final Bag<RegularComponentType<?, ?>> addedTypes = new Bag<>(RegularComponentType.class, 4);
    private final Bag<Object> added = new Bag<>(Object.class, 4);

    private final Bag<RegularComponentType<?, ?>> removedTypes = new Bag<>(RegularComponentType.class, 4);

    public PendingChanges(ComponentMask componentMask) {
        this.componentMask = componentMask;
    }

    public void setComponentMask(ComponentMask componentMask) {
        this.componentMask = componentMask;
    }

    @SuppressWarnings("unchecked")
    public <R> R getComponent(RegularComponentType<?, R> type) {
        for (int i = 0, s = addedTypes.getSize(); i < s; i++) {
            if (addedTypes.get(i).equals(type)) {
                return (R) added.get(i);
            }
        }

        return null;
    }

    public boolean isEmpty() {
        return this.addedTypes.isEmpty() && this.removedTypes.isEmpty();
    }

    public ComponentMask getComponentMask() {
        return this.componentMask;
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
            throw new StorageEngineException("Expected component of type '%s', but got: %s".formatted(type, component));
        }

        // Undo remove
        removedTypes.remove(type);

        // Don't add if type already part of current component mask (no archetype change)
        if (componentMask.getComponentTypes().contains(type)) {
            return false;
        }

        // Add component
        switch (type) {
            case ComponentRelationType<?, ?> relationType -> addComponentRelation(relationType, (ComponentRelation<?, ?>) component);
            case ExclusiveComponentRelationType<?, ?> relationType -> addExclusiveComponentRelation(relationType, component);
            case EntityRelationType<?> relationType -> addEntityRelation(relationType, (EntityRelation<?>) component);
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

            addedTypes.add(relationType);
            added.add(result);
        }

        // Add relation
        result.add(relation);
    }

    private void addExclusiveComponentRelation(ExclusiveComponentRelationType<?, ?> relationType, Object component) {
        // Remove matching relations from component mask
        var componentTypes = componentMask.getComponentTypes();

        for (int i = 0, s = componentTypes.getSize(); i < s; i++) {
            if (componentTypes.get(i) instanceof ExclusiveComponentRelationType<?, ?> other && other.relationship().equals(relationType.relationship())) {
                remove(other);
            }
        }

        // Remove matching relations in addedTypes
        for (int i = addedTypes.getSize() - 1; i >= 0; i--) {
            if (addedTypes.get(i) instanceof ExclusiveComponentRelationType<?, ?> other && other.relationship().equals(relationType.relationship())) {
                addedTypes.remove(i);
                added.remove(i);
            }
        }

        // Add relation component
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
                addedTypes.remove(i);
                added.remove(i);
            }
        }

        // Add relation component
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
        addedTypes.add(type);
        added.add(component);
    }

    public void remove(RegularComponentType<?, ?> type) {
        var componentMaskType = componentMask.getComponentTypes().contains(type);

        // Skip if no-op (not part of component mask or added types)
        if (!addedTypes.contains(type) && !componentMaskType) {
            return;
        }

        // Add remove
        if (componentMaskType && !removedTypes.contains(type)) {
            removedTypes.add(type);
        }

        // Undo adds
        for (int i = addedTypes.getSize() - 1; i >= 0; i--) {
            if (addedTypes.get(i).equals(type)) {
                addedTypes.remove(i);
                added.remove(i);
            }
        }
    }

    public void reset() {
        for (int i = 0, s = added.getSize(); i < s; i++) {
            var component = added.get(i);

            if (component instanceof ComponentRelationResultImpl result) {
                result.free();
                continue;
            }

            if (component instanceof EntityRelationResultImpl result) {
                result.free();
            }
        }

        addedTypes.clear();
        added.clear();

        removedTypes.clear();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PendingChanges(componentMask = ").append(this.componentMask.getId()).append(", addedTypes = ").append(this.addedTypes).append(", removedTypes = ").append(this.removedTypes)
                .append(")");
        return builder.toString();
    }

}
