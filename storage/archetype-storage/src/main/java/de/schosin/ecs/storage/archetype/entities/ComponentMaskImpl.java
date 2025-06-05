package de.schosin.ecs.storage.archetype.entities;

import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BitVector;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.IntBag;

public class ComponentMaskImpl implements ComponentMask {

    private final int id;
    private final BitVector mask;
    private final ImmutableBag<Component<?, ?>> components;
    private final ImmutableBag<RegularComponentType<?, ?>> componentTypes;

    private final IntBag lookup;
    private final Bag<ComponentMaskImpl> add = new Bag<>(ComponentMaskImpl.class, 64);
    private final Bag<ComponentMaskImpl> remove = new Bag<>(ComponentMaskImpl.class, 64);

    private String toString;

    public ComponentMaskImpl(int id, BitVector mask, ImmutableBag<Component<?, ?>> components) {
        this.id = id;
        this.mask = mask;
        this.components = components;
        this.componentTypes = buildComponentTypes(components);

        this.lookup = buildLookup(components);
    }

    private static ImmutableBag<RegularComponentType<?, ?>> buildComponentTypes(ImmutableBag<Component<?, ?>> components) {
        var bag = new Bag<RegularComponentType<?, ?>>(RegularComponentType.class, components.getSize());

        for (int i = 0, s = components.getSize(); i < s; i++) {
            bag.add(components.get(i).type());
        }

        return ImmutableBag.create(bag);
    }

    private static IntBag buildLookup(ImmutableBag<Component<?, ?>> components) {
        var lookup = new IntBag(components.getSize());

        for (int i = 0, s = components.getSize(); i < s; i++) {
            var component = components.get(i);
            lookup.set(component.id(), 1);
        }

        return lookup;
    }

    @Override
    public int getId() {
        return this.id;
    }

    public BitVector getMask() {
        return this.mask;
    }

    @Override
    public boolean contains(int componentId) {
        return componentId < lookup.getSize() && lookup.get(componentId) == 1;
    }

    @Override
    public ImmutableBag<Component<?, ?>> getComponents() {
        return this.components;
    }

    @Override
    public ImmutableBag<RegularComponentType<?, ?>> getComponentTypes() {
        return this.componentTypes;
    }

    public Bag<ComponentMaskImpl> getAdd() {
        return this.add;
    }

    public Bag<ComponentMaskImpl> getRemove() {
        return this.remove;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public String toString() {
        if (toString == null) {
            var builder = new StringBuilder().append("ComponentMask(id = ").append(id).append(", components = (");
            for (int i = 0, s = components.getSize(); i < s; i++) {
                if (i > 0) {
                    builder.append(", ");
                }

                builder.append(components.get(i).display());
            }

            this.toString = builder.append(")").toString();
        }

        return toString;
    }

}
