package de.schosin.ecs.api.archetype;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.NullMarked;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.Components;
import de.schosin.ecs.api.components.Composition;

/**
 * Entity transmuter for changing the component composition of entities
 * in one go. Provides better performance than using {@link Components}
 * individually. 
 * 
 * <p>
 * Entities modified by a transmuter with more than one modification 
 * won't cause intermediate {@link Composition composition updates}.
 * If a transmuter does not change the component composition of an 
 * entity, no composition updates will be triggered.
 * </p>
 */
@NullMarked
public interface Transmuter {

    interface Remove extends Transmuter {
        boolean apply(int entityId);
    }

    interface Add extends Transmuter {

        /**
         * Returns an unused instance for the {@link Pooled pooled} component.
         * 
         * <p>
         * Can be used for applying an adding transmuter, as well as for
         * {@link Components#add(int, Object)}, {@link Archetype} or 
         * {@link World#createEntity(Object...)}.
         * </p>
         * 
         * @param <T> type of component
         * @param clazz class of component
         * @return unused instance
         */
        <T extends Pooled> T getInstance(Class<T> clazz);

    }

    interface Add1<T1> extends Add {
        boolean apply(int entityId, T1 component1);
    }

    interface Add2<T1, T2> extends Add {
        boolean apply(int entityId, T1 component1, T2 component2);
    }

    interface Add3<T1, T2, T3> extends Add {
        boolean apply(int entityId, T1 component1, T2 component2, T3 component3);
    }

    interface Add4<T1, T2, T3, T4> extends Add {
        boolean apply(int entityId, T1 component1, T2 component2, T3 component3, T4 component4);
    }

    interface Add5<T1, T2, T3, T4, T5> extends Add {
        boolean apply(int entityId, T1 component1, T2 component2, T3 component3, T4 component4, T5 component5);
    }

    interface Add6<T1, T2, T3, T4, T5, T6> extends Add {
        boolean apply(int entityId, T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6);
    }

    interface Add7<T1, T2, T3, T4, T5, T6, T7> extends Add {
        boolean apply(int entityId, T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, T7 component7);
    }

    interface Add8<T1, T2, T3, T4, T5, T6, T7, T8> extends Add {
        boolean apply(int entityId, T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, T7 component7, T8 component8);
    }

    interface AddN<T1, T2, T3, T4, T5, T6, T7, T8> extends Add {
        boolean apply(int entityId, T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, T7 component7, T8 component8, Object... others);
    }

    static Builder.Remove remove(Class<?> clazz, Class<?>... components) {
        return new Builder.Remove(components).remove(clazz);
    }

    static <T1> Builder.Add1<T1> add(Class<T1> component1) {
        return new Builder.Add1<>(component1);
    }

    static <T1, T2> Builder.Add2<T1, T2> add(Class<T1> component1, Class<T2> component2) {
        return new Builder.Add2<>(component1, component2);
    }

    static <T1, T2, T3> Builder.Add3<T1, T2, T3> add(Class<T1> component1, Class<T2> component2, Class<T3> component3) {
        return new Builder.Add3<>(component1, component2, component3);
    }

    static <T1, T2, T3, T4> Builder.Add4<T1, T2, T3, T4> add(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4) {
        return new Builder.Add4<>(component1, component2, component3, component4);
    }

    static <T1, T2, T3, T4, T5> Builder.Add5<T1, T2, T3, T4, T5> add(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4, Class<T5> component5) {
        return new Builder.Add5<>(component1, component2, component3, component4, component5);
    }

    static <T1, T2, T3, T4, T5, T6> Builder.Add6<T1, T2, T3, T4, T5, T6> add(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4, Class<T5> component5,
            Class<T6> component6) {

        return new Builder.Add6<>(component1, component2, component3, component4, component5, component6);
    }

    static <T1, T2, T3, T4, T5, T6, T7> Builder.Add7<T1, T2, T3, T4, T5, T6, T7> add(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4, Class<T5> component5,
            Class<T6> component6, Class<T7> component7) {

        return new Builder.Add7<>(component1, component2, component3, component4, component5, component6, component7);
    }

    static <T1, T2, T3, T4, T5, T6, T7, T8> Builder.Add8<T1, T2, T3, T4, T5, T6, T7, T8> add(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
            Class<T5> component5, Class<T6> component6, Class<T7> component7, Class<T8> component8) {

        return new Builder.Add8<>(component1, component2, component3, component4, component5, component6, component7, component8);
    }

    static <T1, T2, T3, T4, T5, T6, T7, T8> Builder.AddN<T1, T2, T3, T4, T5, T6, T7, T8> add(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
            Class<T5> component5, Class<T6> component6, Class<T7> component7, Class<T8> component8, Class<?>... others) {

        return new Builder.AddN<>(component1, component2, component3, component4, component5, component6, component7, component8, others);
    }

    sealed interface Builder {

        final class Remove extends AbstractBuilder<Remove> {
            private Remove(Class<?>... components) {
                remove(components);
            }

            <T1> Builder.Add1<T1> add(Class<T1> component1) {
                return new Builder.Add1<>(component1).remove(this.remove);
            }

            // TODO 2..7

            <T1, T2, T3, T4, T5, T6, T7, T8> Builder.Add8<T1, T2, T3, T4, T5, T6, T7, T8> add(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
                    Class<T5> component5, Class<T6> component6, Class<T7> component7, Class<T8> component8) {

                return new Builder.Add8<>(component1, component2, component3, component4, component5, component6, component7, component8).remove(this.remove);
            }

            <T1, T2, T3, T4, T5, T6, T7, T8> Builder.AddN<T1, T2, T3, T4, T5, T6, T7, T8> add(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
                    Class<T5> component5, Class<T6> component6, Class<T7> component7, Class<T8> component8, Class<?>... others) {

                return new Builder.AddN<>(component1, component2, component3, component4, component5, component6, component7, component8, others).remove(this.remove);
            }

        }

        final class Add1<T1> extends AbstractBuilder<Add1<T1>> {
            private Add1(Class<T1> component1) {
                super(component1);
            }
        }

        final class Add2<T1, T2> extends AbstractBuilder<Add2<T1, T2>> {
            private Add2(Class<T1> component1, Class<T2> component2) {
                super(component1, component2);
            }
        }

        final class Add3<T1, T2, T3> extends AbstractBuilder<Add3<T1, T2, T3>> {
            private Add3(Class<T1> component1, Class<T2> component2, Class<T3> component3) {
                super(component1, component2, component3);
            }
        }

        final class Add4<T1, T2, T3, T4> extends AbstractBuilder<Add4<T1, T2, T3, T4>> {
            private Add4(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4) {
                super(component1, component2, component3, component4);
            }
        }

        final class Add5<T1, T2, T3, T4, T5> extends AbstractBuilder<Add5<T1, T2, T3, T4, T5>> {
            private Add5(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4, Class<T5> component5) {
                super(component1, component2, component3, component4, component5);
            }
        }

        final class Add6<T1, T2, T3, T4, T5, T6> extends AbstractBuilder<Add6<T1, T2, T3, T4, T5, T6>> {
            private Add6(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4, Class<T5> component5, Class<T6> component6) {
                super(component1, component2, component3, component4, component5, component6);
            }
        }

        final class Add7<T1, T2, T3, T4, T5, T6, T7> extends AbstractBuilder<Add7<T1, T2, T3, T4, T5, T6, T7>> {
            private Add7(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4, Class<T5> component5, Class<T6> component6, Class<T7> component7) {
                super(component1, component2, component3, component4, component5, component6, component7);
            }
        }

        final class Add8<T1, T2, T3, T4, T5, T6, T7, T8> extends AbstractBuilder<Add8<T1, T2, T3, T4, T5, T6, T7, T8>> {
            private Add8(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4, Class<T5> component5, Class<T6> component6, Class<T7> component7,
                    Class<T8> component8) {

                super(component1, component2, component3, component4, component5, component6, component7, component8);
            }
        }

        final class AddN<T1, T2, T3, T4, T5, T6, T7, T8> extends AbstractBuilder<AddN<T1, T2, T3, T4, T5, T6, T7, T8>> {
            private AddN(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4, Class<T5> component5, Class<T6> component6, Class<T7> component7,
                    Class<T8> component8, Class<?>... others) {

                super(concat(Class.class, new Class<?>[] { component1, component2, component3, component4, component5, component6, component7, component8 }, others));
            }

            @SuppressWarnings("unchecked")
            public static <T> T[] concat(Class<T> arrayClazz, T[] first, T... others) {
                var array = (T[]) Array.newInstance(arrayClazz, first.length + others.length);
                System.arraycopy(first, 0, array, 0, first.length);
                System.arraycopy(others, 0, array, first.length, others.length);

                return array;
            }
        }

        abstract static sealed class AbstractBuilder<SELF> implements Builder {

            protected final Set<Class<?>> add;
            protected final Set<Class<?>> remove;

            private AbstractBuilder(Class<?>... add) {
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

            public Set<Class<?>> getAdd() {
                return add;
            }

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
                AbstractBuilder other = (AbstractBuilder) obj;
                return Objects.equals(add, other.add) && Objects.equals(remove, other.remove);
            }

        }

    }

    interface Creator {

        Remove createTransmuter(Builder.Remove builder);

        <T1> Add1<T1> createTransmuter(Builder.Add1<T1> builder);

        <T1, T2> Add2<T1, T2> createTransmuter(Builder.Add2<T1, T2> builder);

        <T1, T2, T3> Add3<T1, T2, T3> createTransmuter(Builder.Add3<T1, T2, T3> builder);

        <T1, T2, T3, T4> Add4<T1, T2, T3, T4> createTransmuter(Builder.Add4<T1, T2, T3, T4> builder);

        <T1, T2, T3, T4, T5> Add5<T1, T2, T3, T4, T5> createTransmuter(Builder.Add5<T1, T2, T3, T4, T5> builder);

        <T1, T2, T3, T4, T5, T6> Add6<T1, T2, T3, T4, T5, T6> createTransmuter(Builder.Add6<T1, T2, T3, T4, T5, T6> builder);

        <T1, T2, T3, T4, T5, T6, T7> Add7<T1, T2, T3, T4, T5, T6, T7> createTransmuter(Builder.Add7<T1, T2, T3, T4, T5, T6, T7> builder);

        <T1, T2, T3, T4, T5, T6, T7, T8> Add8<T1, T2, T3, T4, T5, T6, T7, T8> createTransmuter(Builder.Add8<T1, T2, T3, T4, T5, T6, T7, T8> builder);

        <T1, T2, T3, T4, T5, T6, T7, T8> AddN<T1, T2, T3, T4, T5, T6, T7, T8> createTransmuter(Builder.AddN<T1, T2, T3, T4, T5, T6, T7, T8> builder);

    }

}
