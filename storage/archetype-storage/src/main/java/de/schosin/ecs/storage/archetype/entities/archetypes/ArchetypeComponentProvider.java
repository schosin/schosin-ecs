package de.schosin.ecs.storage.archetype.entities.archetypes;

import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;

import de.schosin.ecs.storage.api.components.ComponentProvider;
import de.schosin.ecs.storage.archetype.components.ComponentIndex;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.Pool;

public final class ArchetypeComponentProvider {

    private final Bag<PooledComponent> pooledProviders = new Bag<>(PooledComponent.class, 4);
    private final Bag<EnumComponent> enumProviders = new Bag<>(EnumComponent.class, 4);
    private final Bag<SuppliedComponent> suppliedProviders = new Bag<>(SuppliedComponent.class, 4);

    public ArchetypeComponentProvider(ComponentIndex componentIndex, ArchetypeData archetype, ImmutableBag<ComponentProvider> providers) {
        for (int i = 0, s = providers.getSize(); i < s; i++) {
            var provider = providers.get(i);

            var componentType = provider.type();
            if (archetype.getComponentTypes().contains(componentType)) {
                continue;
            }

            var index = archetype.getComponentIndex(componentIndex.getId(componentType));

            switch (provider) {
                case ComponentProvider.PooledComponent(var type) -> pooledProviders.add(new PooledComponent(index, componentIndex.getPool(type.clazz())));
                case ComponentProvider.EnumComponent(var type, var component) when type.clazz().getEnumConstants().length != 1 -> enumProviders.add(new EnumComponent(index, component));
                case ComponentProvider.EnumComponent(var type, var component) -> {
                    // zero-sized component skipped
                    // TODO this will cause a validate error on creator (ArchetypeDataSoaImpl) -> calls bitset must not contain zero-sized components 
                    var todo = true;
                }
                case ComponentProvider.SuppliedComponent(var type, var supplier) -> suppliedProviders.add(new SuppliedComponent(index, supplier));
            }
        }
    }
    
    

    public void apply(ObjIntConsumer<Object> components) {
        var pooledData = pooledProviders.getData();
        for (int i = 0, s = pooledProviders.getSize(); i < s; i++) {
            var component = pooledData[i];
            components.accept(component.pool.getInstance(), component.index);
        }

        var enumData = enumProviders.getData();
        for (int i = 0, s = enumProviders.getSize(); i < s; i++) {
            var component = enumData[i];
            components.accept(component.component, component.index);
        }

        var suppliedData = suppliedProviders.getData();
        for (int i = 0, s = suppliedProviders.getSize(); i < s; i++) {
            var component = suppliedData[i];
            components.accept(component.supplier.get(), component.index);
        }
    }

    private record PooledComponent(int index, Pool<?> pool) {
    }

    private record EnumComponent(int index, Enum<?> component) {
    }

    private record SuppliedComponent(int index, Supplier<?> supplier) {
    }

}
