package de.schosin.ecs.storage.defaultimpl.entities;

import java.util.Map;

import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.storage.defaultimpl.components.DefaultComponent;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BitVector;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.IntBag;

public class ComponentMaskImpl implements ComponentMask {

    private final int id;
    private final BitVector mask;
    private final ImmutableBag<Component<?, ?>> components;
    private final ImmutableBag<RegularComponentType<?, ?>> componentTypes;
    private final Map<RegularComponentType<?, ?>, Component<?, ?>> componentTypeLookup;

    private final IntBag lookup;
    private final Bag<ComponentMaskImpl> add = new Bag<>(ComponentMaskImpl.class, 64);
    private final Bag<ComponentMaskImpl> remove = new Bag<>(ComponentMaskImpl.class, 64);

    private String toString;

    public ComponentMaskImpl(int id, BitVector mask, ImmutableBag<Component<?, ?>> components, Map<RegularComponentType<?, ?>, Component<?, ?>> componentTypeLookup) {
        this.id = id;
        this.mask = mask;
        this.components = components;
        this.componentTypes = buildComponentTypes(components);
        this.componentTypeLookup = componentTypeLookup;

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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addComponents(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, ImmutableBag<Object> components) {
        for (int i = 0; i < components.getSize(); i++) {
            var component = (DefaultComponent) this.componentTypeLookup.get(componentTypes.get(i));
            component.addComponent(entityId, components.get(i));
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addComponents(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        for (int i = 0; i < components.length; i++) {
            var componentType = componentTypes.get(i);
            var component = (DefaultComponent) this.componentTypeLookup.get(componentType);

            component.addComponent(entityId, components[i]);
        }
    }

    @SuppressWarnings("rawtypes")
    public void removeComponents(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes) {
        for (int i = 0, s = componentTypes.getSize(); i < s; i++) {
            var component = (DefaultComponent) this.componentTypeLookup.get(componentTypes.get(i));
            component.removeComponent(entityId);
        }
    }

    @SuppressWarnings("rawtypes")
    public void removeComponents(int entityId) {
        for (int i = 0, s = components.getSize(); i < s; i++) {
            var component = (DefaultComponent) this.components.get(i);
            component.removeComponent(entityId);
        }
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
