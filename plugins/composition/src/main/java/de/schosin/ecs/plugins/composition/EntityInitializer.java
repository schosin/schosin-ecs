package de.schosin.ecs.plugins.composition;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.data.DataProcessor;
import de.schosin.ecs.storage.api.components.ComponentProvider;
import de.schosin.ecs.storage.api.components.ComponentProvider.EnumComponent;
import de.schosin.ecs.storage.api.components.ComponentProvider.PooledComponent;
import de.schosin.ecs.storage.api.components.ComponentProvider.SuppliedComponent;
import de.schosin.ecs.utils.collections.ImmutableBag;

/**
 * Provider of component instances used for
 * {@link CompositionPlugin#initialize(Composition.Builder, EntityInitializer...) initialization of entities during creation}.
 * 
 * <p>
 * If components require initialization based on other components of that entity, use this provider to add the components
 * and {@link Composition#inserted(IntConsumer)} or {@link CompositionData#inserted(DataProcessor)} to initialize them.
 */
public sealed interface EntityInitializer {

    default void todo() {
        // TODO don't expose ComponentProvider in API
        var todo = true;
    }

    ImmutableBag<ComponentProvider> components();

    static EntityInitializer.Builder builder() {
        return new EntityInitializerBuilder();
    }

    sealed interface Builder {

        @SuppressWarnings("unchecked")
        Builder with(Class<? extends Pooled>... components);

        Builder with(Enum<?>... components);

        <T> Builder with(Class<T> component, Supplier<T> supplier);

        <R, T> Builder withRelation(Class<R> relationship, Supplier<R> relationshipSupplier, Class<R> target, Supplier<T> targetSupplier);

        <R> Builder withRelation(Class<R> relationship, Supplier<R> relationshipSupplier, Enum<?> target);

        <T> Builder withRelation(Enum<?> relationship, Class<T> target, Supplier<T> targetSupplier);

        Builder withRelation(Enum<?> relationship, Enum<?> target);

        EntityInitializer build();

    }

}

record EntityInitializerImpl(ImmutableBag<ComponentProvider> components) implements EntityInitializer {
}

final class EntityInitializerBuilder implements EntityInitializer.Builder {

    private final Map<RegularComponentType<?, ?>, ComponentProvider> components = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public EntityInitializer.Builder with(Class<? extends Pooled>... components) {
        for (var component : components) {
            var type = ComponentType.component(component);
            this.components.put(type, new PooledComponent(type));
        }

        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EntityInitializer.Builder with(Enum<?>... components) {
        for (var component : components) {
            var type = (ClassType<? extends Enum<?>>) ComponentType.component(component.getClass());
            this.components.put(type, new EnumComponent(type, component));
        }

        return this;
    }

    @Override
    public <T> EntityInitializer.Builder with(Class<T> component, Supplier<T> supplier) {
        var type = ComponentType.component(component);
        this.components.put(type, new SuppliedComponent(type, supplier));

        return this;
    }

    @Override
    public <R, T> EntityInitializer.Builder withRelation(Class<R> relationship, Supplier<R> relationshipSupplier, Class<R> target, Supplier<T> targetSupplier) {
        var type = ComponentType.relation(relationship, target);
        this.components.put(type, new SuppliedComponent(type, () -> Relation.create(relationshipSupplier.get(), targetSupplier.get())));

        return this;
    }

    @Override
    public <T> EntityInitializer.Builder withRelation(Enum<?> relationship, Class<T> target, Supplier<T> targetSupplier) {
        var type = ComponentType.relation(relationship.getClass(), target);
        this.components.put(type, new SuppliedComponent(type, () -> Relation.create(relationship, targetSupplier.get())));

        return this;
    }

    @Override
    public <R> EntityInitializer.Builder withRelation(Class<R> relationship, Supplier<R> relationshipSupplier, Enum<?> target) {
        var type = ComponentType.relation(relationship, target.getClass());
        this.components.put(type, new SuppliedComponent(type, () -> Relation.create(relationshipSupplier.get(), target)));

        return this;
    }

    @Override
    public EntityInitializer.Builder withRelation(Enum<?> relationship, Enum<?> target) {
        var type = ComponentType.relation(relationship.getClass(), target.getClass());
        this.components.put(type, new SuppliedComponent(type, () -> Relation.create(relationship, target)));

        return this;
    }

    @Override
    public EntityInitializer build() {
        var components = this.components.values().toArray(ComponentProvider[]::new);

        return new EntityInitializerImpl(ImmutableBag.of(components));
    }

}
