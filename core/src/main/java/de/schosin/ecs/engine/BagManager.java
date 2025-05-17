package de.schosin.ecs.engine;

import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.IntBag;

@SuppressWarnings("rawtypes")
public class BagManager {

    private static final int ENTITY_SIZE = 1024;
    private static final int COMPONENT_SIZE = 64;

    private final Bag<Bag> entityBags = new Bag<>(Bag.class, 32);
    private final Bag<IntBag> entityIntBags = new Bag<>(IntBag.class, 32);

    private final Bag<Bag> componentBags = new Bag<>(Bag.class, 32);
    private final Bag<IntBag> componentIntBags = new Bag<>(IntBag.class, 32);

    private volatile int entitySize = ENTITY_SIZE;
    private volatile int componentSize = COMPONENT_SIZE;

    public int getEntitySize() {
        return entitySize;
    }

    public void ensureEntitySize(int entityId) {
        if (entityId >= this.entitySize) {
            this.entitySize = Math.max(this.entitySize * 2, Integer.highestOneBit(entityId) * 2);

            for (int i = 0, s = this.entityBags.getSize(); i < s; i++) {
                this.entityBags.get(i).ensureCapacity(this.entitySize);
            }

            for (int i = 0, s = this.entityIntBags.getSize(); i < s; i++) {
                this.entityIntBags.get(i).ensureCapacity(this.entitySize);
            }
        }
    }

    public int getComponentSize() {
        return componentSize;
    }

    public void ensureComponentSize(int componentId) {
        if (componentId >= this.componentSize) {
            this.componentSize = Math.max(this.componentSize * 2, Integer.highestOneBit(componentId) * 2);

            for (int i = 0, s = this.componentBags.getSize(); i < s; i++) {
                this.componentBags.get(i).ensureCapacity(this.componentSize);
            }

            for (int i = 0, s = this.componentIntBags.getSize(); i < s; i++) {
                this.componentIntBags.get(i).ensureCapacity(this.componentSize);
            }
        }
    }

    public <T> Bag<T> createEntityBag(Class<? super T> clazz) {
        return createEntityBag(clazz, entitySize);
    }

    public <T> Bag<T> createEntityBag(Class<? super T> clazz, int size) {
        var bag = new Bag<T>(clazz, size);
        this.entityBags.add(bag);

        return bag;
    }

    public IntBag createEntityIntBag() {
        return createEntityIntBag(entitySize);
    }

    public IntBag createEntityIntBag(int size) {
        var bag = new IntBag(size);
        this.entityIntBags.add(bag);

        return bag;
    }

    public <T> Bag<T> createComponentBag(Class<? super T> clazz) {
        return createComponentBag(clazz, componentSize);
    }

    public <T> Bag<T> createComponentBag(Class<? super T> clazz, int size) {
        var bag = new Bag<T>(clazz, size);
        this.componentBags.add(bag);

        return bag;
    }

    public IntBag createComponentIntBag() {
        return createComponentIntBag(componentSize);
    }

    public IntBag createComponentIntBag(int size) {
        var bag = new IntBag(size);
        this.componentIntBags.add(bag);

        return bag;
    }

    public void free(Bag bag) {
        this.entityBags.remove(bag);
        this.componentBags.remove(bag);
    }

    public void free(IntBag bag) {
        this.entityIntBags.remove(bag);
        this.componentIntBags.remove(bag);
    }

}
