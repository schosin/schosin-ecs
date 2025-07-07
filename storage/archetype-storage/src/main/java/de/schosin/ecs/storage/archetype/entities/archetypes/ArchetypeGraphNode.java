package de.schosin.ecs.storage.archetype.entities.archetypes;

import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.archetype.components.ComponentIndex;
import de.schosin.ecs.storage.archetype.entities.EntityIndex;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BitVector;
import de.schosin.ecs.utils.collections.ImmutableBag;

/**
 * Represents the graph between archetypes established by adding or removing component types.
 * The graph supports both resolving an archetype given a set of {@link RegularComponentType RegularComponentTypes},
 * as well as handling {@link PendingChanges}.
 * 
 * <p>
 * The node holds a lazy field of the corresponding archetype that gets initialized on first
 * access to {@link #getArchetype()}. This field is lazily set to avoid instantiating archetypes
 * that will never contain when traversing the graph.
 * </p>
 */
public class ArchetypeGraphNode {

    private final ComponentIndex componentIndex;
    private final EntityIndex entityIndex;

    private final BitVector componentIds;
    private final ImmutableBag<Component<?, ?>> components;
    private final Bag<RegularComponentType<?, ?>> componentTypes;

    private ArchetypeData archetype;

    private final Bag<ArchetypeGraphNode> add = new Bag<>(ArchetypeGraphNode.class, 64);
    private final Bag<ArchetypeGraphNode> remove = new Bag<>(ArchetypeGraphNode.class, 64);

    public ArchetypeGraphNode(ComponentIndex componentIndex, EntityIndex entityIndex, BitVector componentIds, ImmutableBag<Component<?, ?>> components) {
        this.componentIndex = componentIndex;
        this.entityIndex = entityIndex;
        this.componentIds = componentIds;
        this.components = components;

        this.componentTypes = new Bag<>(RegularComponentType.class, components.getSize());
        for (int i = 0, s = components.getSize(); i < s; i++) {
            this.componentTypes.add(components.get(i).type());
        }
    }

    /**
     * Returns the archetype node if {@code componentType} is added to the component types of this node.
     * Will return this node if the component type is already present.
     * 
     * <p>
     * To resolve the correct archetype node, start with the empty node and then add the components one-by-one
     * in a loop.
     * </p>
     * 
     * @param componentType added component type
     * @return archetype node for all component types of this node and the passed one
     */
    public ArchetypeGraphNode addComponentType(RegularComponentType<?, ?> componentType) {
        if (componentTypes.contains(componentType)) {
            return this;
        }

        var componentId = componentIndex.getId(componentType);

        var result = this.add.getSafe(componentId);
        if (result == null) {
            synchronized (this.add) {
                result = this.add.getSafe(componentId);
                if (result == null) {
                    result = entityIndex.addToArchetype(this, componentId, componentType);
                    this.add.set(componentId, result);
                }
            }
        }

        return result;
    }

    /**
     * Returns the archetype node if {@code componentType} is removed from the component types of this node.
     * Will return this node if the component is not present.
     * 
     * @param componentType removed component type
     * @return archetype node for all component types of this node minus the passed one
     */
    public ArchetypeGraphNode removeComponentType(RegularComponentType<?, ?> componentType) {
        if (!componentTypes.contains(componentType)) {
            return this;
        }

        var componentId = componentIndex.getId(componentType);

        var result = this.remove.getSafe(componentId);
        if (result == null) {
            synchronized (this.remove) {
                result = this.remove.getSafe(componentId);
                if (result == null) {
                    result = entityIndex.removeFromArchetype(this, componentId, componentType);
                    this.remove.set(componentId, result);
                }
            }
        }

        return result;
    }

    public ArchetypeData getArchetype() {
        if (this.archetype == null) {
            this.archetype = entityIndex.getArchetype(this);
        }

        return this.archetype;
    }

    public BitVector getComponentIds() {
        return this.componentIds;
    }

    public ImmutableBag<Component<?, ?>> getComponents() {
        return this.components;
    }

    public ImmutableBag<RegularComponentType<?, ?>> getComponentTypes() {
        return this.componentTypes;
    }

    @Override
    public String toString() {
        if (archetype != null) {
            return new StringBuilder().append("ArchetypeGraphNode(archetype = ").append(archetype)
                    .append(")")
                    .toString();
        }

        return new StringBuilder()
                .append("ArchetypeGraphNode(componentTypes = ").append(this.componentTypes)
                .append(")")
                .toString();
    }

}
