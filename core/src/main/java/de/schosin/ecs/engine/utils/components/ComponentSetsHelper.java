package de.schosin.ecs.engine.utils.components;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.ComponentSet.AccessorFactory;
import de.schosin.ecs.api.components.ComponentSet.ArchetypeIterator;
import de.schosin.ecs.api.components.ComponentSet.ComponentData;
import de.schosin.ecs.api.components.ComponentSet.ComponentSetData;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.data.ArchetypeComponentAccessor;
import de.schosin.ecs.api.data.ComponentAccessor;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.api.data.DataProcessor;
import de.schosin.ecs.api.data.IterableAccessor;
import de.schosin.ecs.engine.utils.exceptions.EcsComponentSetException;

public class ComponentSetsHelper {

    public interface ComponentSetFactory<S extends ComponentSet<?>> {
        List<ComponentData<S, ?, ?>> getComponents();

        ComponentAccessor<S> getComponentAccessor(DataAccessor accessor, Components<?, ?>[] mappers);

        <P extends DataProcessor<S>> ArchetypeIterator<P> getArchetypeIterator(IterableAccessor accessor, ArchetypeComponentAccessor<?>[] accessors);
    }

    private static final Map<Class<? extends ComponentSet<?>>, ComponentSetFactory<?>> COMPONENT_SET_DATA = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <S extends ComponentSet<?>> ComponentSetFactory<S> getFactory(Class<S> componentSet) {
        var result = COMPONENT_SET_DATA.get(componentSet);
        if (result != null) {
            return (ComponentSetFactory<S>) result;
        }

        return (ComponentSetFactory<S>) COMPONENT_SET_DATA.computeIfAbsent(componentSet, ComponentSetsHelper::resolveFactory);
    }

    private static <S extends ComponentSet<?>> ComponentSetFactory<S> resolveFactory(Class<S> componentSet) {
        var data = getData(componentSet);

        return new ComponentSetFactoryImpl<>(data);
    }

    private static class ComponentSetFactoryImpl<S extends ComponentSet<?>> implements ComponentSetFactory<S> {

        private final List<ComponentData<S, ?, ?>> componentTypes;
        private final AccessorFactory<S> accessorFactory;
        private final BiFunction<IterableAccessor, ArchetypeComponentAccessor<?>[], ArchetypeIterator<?>> iteratorFactory;

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private ComponentSetFactoryImpl(ComponentSetData<S, ?> data) {
            this.componentTypes = data.components();
            this.accessorFactory = data.accessorFactory();
            this.iteratorFactory = (BiFunction) data.iteratorFactory();
        }

        @Override
        public List<ComponentData<S, ?, ?>> getComponents() {
            return componentTypes;
        }

        @Override
        public ComponentAccessor<S> getComponentAccessor(DataAccessor accessor, Components<?, ?>[] mappers) {
            return accessorFactory.create(accessor, mappers);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <P extends DataProcessor<S>> ArchetypeIterator<P> getArchetypeIterator(IterableAccessor accessor, ArchetypeComponentAccessor<?>[] accessors) {
            return (ArchetypeIterator<P>) iteratorFactory.apply(accessor, accessors);
        }

    }

    @SuppressWarnings("unchecked")
    public static <S extends ComponentSet<?>, P extends DataProcessor<S>> ComponentSetData<S, P> getData(Class<S> componentSet) {
        try {
            return (ComponentSetData<S, P>) componentSet.getField("DATA").get(null);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException ex) {
            var message = "ComponentSet '%s' does not declare ComponentSetData<S>: static ComponentSetData<MyComponentSet> DATA = ComponentSet.builder(MyComponentSet::get).build();"
                    .formatted(componentSet.getName());

            throw new EcsComponentSetException(componentSet, message, ex);
        }
    }

    private ComponentSetsHelper() {
    }

}
