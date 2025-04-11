package de.schosin.ecs.engine.components;

import de.schosin.ecs.engine.utils.collections.Bag;
import de.schosin.ecs.engine.utils.collections.BitVector;
import de.schosin.ecs.engine.utils.collections.IntBag;

/**
 * Describes a component mask for the components an entity has.
 * 
 * <p>
 * By giving each unique {@link #mask component mask} an identity with an id,
 * these instances can be reused for each entity with the same composition.
 * </p>
 * 
 * <p>
 * The fields {@link #add} and {@link #remove} can cache the component mask
 * when {@link ComponentMaskManager#addComponent(ComponentMask, int)} adding} 
 * or {@link ComponentMaskManager#removeComponent(ComponentMask, int) removing} 
 * a component from this current composition. 
 * The {@link ComponentData#id()} is used as an index into the bag for a fast 
 * look up.
 * </p>
 */
public class ComponentMask {

    private final int id;
    private final BitVector mask;
    private final ComponentData<?>[] components;

    private final IntBag lookup;
    private final Bag<ComponentMask> add;
    private final Bag<ComponentMask> remove;

    public ComponentMask(int id, BitVector mask, ComponentData<?>[] components, IntBag lookup, Bag<ComponentMask> add, Bag<ComponentMask> remove) {
        this.id = id;
        this.mask = mask;
        this.components = components;

        this.lookup = lookup;
        this.add = add;
        this.remove = remove;
    }

    public int getId() {
        return id;
    }

    public boolean contains(int componentId) {
        return lookup.get(componentId) == 1;
    }

    public BitVector getMask() {
        return mask;
    }

    public ComponentData<?>[] getComponents() {
        return components;
    }

    public Bag<ComponentMask> getAddMapping() {
        return add;
    }

    public Bag<ComponentMask> getRemoveMapping() {
        return remove;
    }

}
