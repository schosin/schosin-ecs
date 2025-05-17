package de.schosin.ecs.plugins.transmuter;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.Set;

import org.jspecify.annotations.NullMarked;

import de.schosin.ecs.api.components.ComponentType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.codegen.EcsCodegen;

@NullMarked
@EcsCodegen
public interface BaseTransmuter {
}

abstract class AbstractTransmuterBuilder<SELF> implements Transmuter.Builder {

    protected final SequencedSet<RegularComponentType<?>> add;
    protected final SequencedSet<ComponentType<?>> remove;

    protected AbstractTransmuterBuilder(RegularComponentType<?>... add) {
        this.add = new LinkedHashSet<>(List.of(add));
        this.remove = new LinkedHashSet<>();
    }

    public SELF remove(Class<?>... components) {
        return remove(convert(components));
    }

    private static RegularComponentType<?>[] convert(Class<?>... classes) {
        return Arrays.stream(classes).map(ComponentType::component).toArray(RegularComponentType<?>[]::new);
    }

    @SuppressWarnings("unchecked")
    public SELF remove(ComponentType<?>... components) {
        for (var clazz : components) {
            if (this.add.contains(clazz)) {
                throw new IllegalArgumentException("Cannot remove component marked for adding: " + clazz);
            }

            this.remove.add(clazz);
        }

        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    protected SELF remove(Set<ComponentType<?>> components) {
        for (var clazz : components) {
            if (this.add.contains(clazz)) {
                throw new IllegalArgumentException("Cannot remove component marked for adding: " + clazz);
            }

            this.remove.add(clazz);
        }

        return (SELF) this;
    }

    @Override
    public SequencedSet<RegularComponentType<?>> getAdd() {
        return add;
    }

    @Override
    public SequencedSet<ComponentType<?>> getRemove() {
        return remove;
    }

    @Override
    public int hashCode() {
        return Objects.hash(add, remove);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractTransmuterBuilder other = (AbstractTransmuterBuilder) obj;
        return Objects.equals(add, other.add) && Objects.equals(remove, other.remove);
    }

}