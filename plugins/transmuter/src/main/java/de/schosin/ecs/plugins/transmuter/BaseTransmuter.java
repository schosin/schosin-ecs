package de.schosin.ecs.plugins.transmuter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.NullMarked;

import de.schosin.ecs.codegen.EcsCodegen;

@NullMarked
@EcsCodegen
public interface BaseTransmuter {
}

abstract class AbstractTransmuterBuilder<SELF> implements Transmuter.Builder {

    protected final Set<Class<?>> add;
    protected final Set<Class<?>> remove;

    protected AbstractTransmuterBuilder(Class<?>... add) {
        this.add = Set.of(add);
        this.remove = new HashSet<>();
    }

    @SuppressWarnings("unchecked")
    public SELF remove(Class<?>... components) {
        for (var clazz : components) {
            if (this.add.contains(clazz)) {
                throw new IllegalArgumentException("Cannot remove component marked for adding: " + clazz);
            }

            this.remove.add(clazz);
        }

        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    protected SELF remove(Set<Class<?>> components) {
        for (var clazz : components) {
            if (this.add.contains(clazz)) {
                throw new IllegalArgumentException("Cannot remove component marked for adding: " + clazz);
            }

            this.remove.add(clazz);
        }

        return (SELF) this;
    }

    @Override
    public Set<Class<?>> getAdd() {
        return add;
    }

    @Override
    public Set<Class<?>> getRemove() {
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