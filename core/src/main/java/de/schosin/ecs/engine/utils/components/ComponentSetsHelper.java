package de.schosin.ecs.engine.utils.components;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.ComponentSet.ComponentData;
import de.schosin.ecs.api.components.ComponentSet.ComponentSetData;
import de.schosin.ecs.api.components.ComponentSet.Factory;
import de.schosin.ecs.api.components.ComponentSets;
import de.schosin.ecs.engine.utils.exceptions.EcsComponentSetException;

public class ComponentSetsHelper {

    public interface ComponentSetFactory<S extends ComponentSet> {
        List<ComponentData<S, ?, ?>> getComponents();

        S getInstance(int entityId, Object... components);
    }

    private static final boolean COMPONENT_SETS_AVAILABLE = checkComponentSetsAvailable();

    private static final Map<Class<? extends ComponentSet>, ComponentSetFactory<?>> COMPONENT_SET_DATA = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <S extends ComponentSet> ComponentSetFactory<S> getFactory(Class<S> componentSet) {
        var result = COMPONENT_SET_DATA.get(componentSet);
        if (result != null) {
            return (ComponentSetFactory<S>) result;
        }

        return (ComponentSetFactory<S>) COMPONENT_SET_DATA.computeIfAbsent(componentSet, ComponentSetsHelper::resolveFactory);
    }

    private static <S extends ComponentSet> ComponentSetFactory<S> resolveFactory(Class<S> componentSet) {
        var data = getData(componentSet);

        return new ComponentSetFactoryImpl<>(data);
    }

    private static class ComponentSetFactoryImpl<S extends ComponentSet> implements ComponentSetFactory<S> {

        private final List<ComponentData<S, ?, ?>> componentTypes;
        private final Factory<S> factory;

        private ComponentSetFactoryImpl(ComponentSetData<S> data) {
            this.componentTypes = data.components();
            this.factory = data.factory();
        }

        @Override
        public List<ComponentData<S, ?, ?>> getComponents() {
            return componentTypes;
        }

        @Override
        public S getInstance(int entityId, Object... components) {
            return factory.create(entityId, components);
        }
    }

    @SuppressWarnings("unchecked")
    private static <S extends ComponentSet> ComponentSetData<S> getData(Class<S> componentSet) {
        if (COMPONENT_SETS_AVAILABLE) {
            var result = ComponentSets.getData(componentSet);
            if (result != null) {
                return result;
            }
        }

        try {
            return (ComponentSetData<S>) componentSet.getField("DATA").get(null);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException ex) {
            var message = "ComponentSet '%s' does not declare ComponentSetData<S>: static ComponentSetData<MyComponentSet> DATA = ComponentSet.builder(MyComponentSet::get).build();"
                    .formatted(componentSet.getName());

            throw new EcsComponentSetException(componentSet, message, ex);
        }
    }

    private static boolean checkComponentSetsAvailable() {
        try {
            Class.forName("de.schosin.ecs.api.components.ComponentSets");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private ComponentSetsHelper() {
    }

}
