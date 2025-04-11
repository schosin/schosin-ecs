package de.schosin.ecs.engine.entities;

import java.util.Arrays;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.archetype.Archetype;
import de.schosin.ecs.engine.components.ComponentData;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMask;
import de.schosin.ecs.engine.components.ComponentMaskManager;
import de.schosin.ecs.engine.utils.collections.ArrayUtils;

public class ArchetypeManager implements Archetype.Creator {

    private final ComponentManager componentManager;
    private final ComponentMaskManager componentMaskManager;
    private final EntityManager entityManager;

    public ArchetypeManager(ComponentManager componentManager, ComponentMaskManager componentMaskManager, EntityManager entityManager) {
        this.componentManager = componentManager;
        this.componentMaskManager = componentMaskManager;
        this.entityManager = entityManager;
    }

    @Override
    public <T1> Archetype.Of1<T1> createArchetype(Class<T1> component1) {
        return new ArchetypeImpl1<>(component1);
    }

    @Override
    public <T1, T2> Archetype.Of2<T1, T2> createArchetype(Class<T1> component1, Class<T2> component2) {
        return new ArchetypeImpl2<>(component1, component2);
    }

    @Override
    public <T1, T2, T3> Archetype.Of3<T1, T2, T3> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3) {
        return new ArchetypeImpl3<>(component1, component2, component3);
    }

    @Override
    public <T1, T2, T3, T4> Archetype.Of4<T1, T2, T3, T4> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4) {
        return new ArchetypeImpl4<>(component1, component2, component3, component4);
    }

    @Override
    public <T1, T2, T3, T4, T5> Archetype.Of5<T1, T2, T3, T4, T5> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4, Class<T5> component5) {
        return new ArchetypeImpl5<>(component1, component2, component3, component4, component5);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6> Archetype.Of6<T1, T2, T3, T4, T5, T6> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4, Class<T5> component5,
            Class<T6> component6) {

        return new ArchetypeImpl6<>(component1, component2, component3, component4, component5, component6);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7> Archetype.Of7<T1, T2, T3, T4, T5, T6, T7> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
            Class<T5> component5, Class<T6> component6, Class<T7> component7) {

        return new ArchetypeImpl7<>(component1, component2, component3, component4, component5, component6, component7);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, T8> Archetype.Of8<T1, T2, T3, T4, T5, T6, T7, T8> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
            Class<T5> component5, Class<T6> component6, Class<T7> component7, Class<T8> component8) {

        return new ArchetypeImpl8<>(component1, component2, component3, component4, component5, component6, component7, component8);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, T8> Archetype.OfN<T1, T2, T3, T4, T5, T6, T7, T8> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
            Class<T5> component5, Class<T6> component6, Class<T7> component7, Class<T8> component8, Class<?>... others) {

        return new ArchetypeImplN<>(component1, component2, component3, component4, component5, component6, component7, component8, others);
    }

    private class AbstractArchetypeImpl implements Archetype {

        private final ComponentMask componentMask;
        private final ComponentData<?>[] dataLookup;

        protected AbstractArchetypeImpl(Class<?>... components) {
            this.componentMask = componentMaskManager.getComponentMask(components);

            this.dataLookup = Arrays.stream(components)
                    .map(componentManager::getData)
                    .toArray(ComponentData[]::new);
        }

        @Override
        public <T extends Pooled> T getInstance(Class<T> clazz) {
            return componentManager.getData(clazz).getInstance();
        }

        protected final int createEntity(Object... components) {
            return entityManager.create(componentMask, components);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        protected final int[] createEntities(int count, Archetype.Initialize initialize) {
            var init = new InitializeImpl();
            init.size = this.componentMask.getComponents().length;
            init.added = init.size;
            init.components = new Object[init.size];

            var data = new Object[init.size][count];

            for (int i = 0; i < count; i++) {
                init.valid = false;
                initialize.initialize(i, init);

                if (!init.valid) {
                    throw new IllegalStateException("Initialization callback not called for entity %d/%d".formatted(i + 1, count));
                }

                for (int c = 0; c < init.added; c++) {
                    data[c][i] = init.components[c];
                }
            }

            return entityManager.createEntities(this.componentMask, data, this.dataLookup);
        }

    }

    @SuppressWarnings("rawtypes")
    private class InitializeImpl implements Archetype.Of1.Init, Archetype.Of2.Init, Archetype.Of3.Init, Archetype.Of4.Init, Archetype.Of5.Init, Archetype.Of6.Init, Archetype.Of7.Init,
            Archetype.Of8.Init, Archetype.OfN.Init {

        private int size;
        private Object[] components;

        private int added;
        private boolean valid;

        @Override
        public void initialize(Object component1) {
            this.components[0] = component1;

            this.valid = true;
        }

        @Override
        public void initialize(Object component1, Object component2) {
            this.components[0] = component1;
            this.components[1] = component2;

            this.valid = true;
        }

        @Override
        public void initialize(Object component1, Object component2, Object component3) {
            this.components[0] = component1;
            this.components[1] = component2;
            this.components[2] = component3;

            this.valid = true;
        }

        @Override
        public void initialize(Object component1, Object component2, Object component3, Object component4) {
            this.components[0] = component1;
            this.components[1] = component2;
            this.components[2] = component3;
            this.components[3] = component4;

            this.valid = true;
        }

        @Override
        public void initialize(Object component1, Object component2, Object component3, Object component4, Object component5) {
            this.components[0] = component1;
            this.components[1] = component2;
            this.components[2] = component3;
            this.components[3] = component4;
            this.components[4] = component5;

            this.valid = true;
        }

        @Override
        public void initialize(Object component1, Object component2, Object component3, Object component4, Object component5, Object component6) {
            this.components[0] = component1;
            this.components[1] = component2;
            this.components[2] = component3;
            this.components[3] = component4;
            this.components[4] = component5;
            this.components[5] = component6;

            this.valid = true;
        }

        @Override
        public void initialize(Object component1, Object component2, Object component3, Object component4, Object component5, Object component6, Object component7) {
            this.components[0] = component1;
            this.components[1] = component2;
            this.components[2] = component3;
            this.components[3] = component4;
            this.components[4] = component5;
            this.components[5] = component6;
            this.components[6] = component7;

            this.valid = true;
        }

        @Override
        public void initialize(Object component1, Object component2, Object component3, Object component4, Object component5, Object component6, Object component7, Object component8) {
            this.components[0] = component1;
            this.components[1] = component2;
            this.components[2] = component3;
            this.components[3] = component4;
            this.components[4] = component5;
            this.components[5] = component6;
            this.components[6] = component7;
            this.components[7] = component8;

            this.valid = true;
        }

        @Override
        public void initialize(Object component1, Object component2, Object component3, Object component4, Object component5, Object component6, Object component7, Object component8,
                Object... components) {

            this.components[0] = component1;
            this.components[1] = component2;
            this.components[2] = component3;
            this.components[3] = component4;
            this.components[4] = component5;
            this.components[5] = component6;
            this.components[6] = component7;
            this.components[7] = component8;

            for (int i = 0, s = components.length; i < s; i++) {
                this.components[8 + i] = components[i];
            }

            this.added = 8 + components.length;
            this.valid = true;
        }

    }

    private class ArchetypeImpl1<T1> extends AbstractArchetypeImpl implements Archetype.Of1<T1> {

        private ArchetypeImpl1(Class<T1> component1) {
            super(component1);
        }

        @Override
        public int create(T1 component1) {
            return createEntity(component1);
        }

        @Override
        public int[] createBatch(int count, Initialize<Init<T1>> init) {
            return createEntities(count, init);
        }

    }

    private class ArchetypeImpl2<T1, T2> extends AbstractArchetypeImpl implements Archetype.Of2<T1, T2> {

        private ArchetypeImpl2(Class<T1> component1, Class<T2> component2) {
            super(component1, component2);
        }

        @Override
        public int create(T1 component1, T2 component2) {
            return createEntity(component1, component2);
        }

        @Override
        public int[] createBatch(int count, Initialize<Init<T1, T2>> init) {
            return createEntities(count, init);
        }

    }

    private class ArchetypeImpl3<T1, T2, T3> extends AbstractArchetypeImpl implements Archetype.Of3<T1, T2, T3> {

        private ArchetypeImpl3(Class<T1> component1, Class<T2> component2, Class<T3> component3) {
            super(component1, component2, component3);
        }

        @Override
        public int create(T1 component1, T2 component2, T3 component3) {
            return createEntity(component1, component2, component3);
        }

        @Override
        public int[] createBatch(int count, Initialize<Init<T1, T2, T3>> init) {
            return createEntities(count, init);
        }

    }

    private class ArchetypeImpl4<T1, T2, T3, T4> extends AbstractArchetypeImpl implements Archetype.Of4<T1, T2, T3, T4> {

        private ArchetypeImpl4(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4) {
            super(component1, component2, component3, component4);
        }

        @Override
        public int create(T1 component1, T2 component2, T3 component3, T4 component4) {
            return createEntity(component1, component2, component3, component4);
        }

        @Override
        public int[] createBatch(int count, Initialize<Init<T1, T2, T3, T4>> init) {
            return createEntities(count, init);
        }

    }

    private class ArchetypeImpl5<T1, T2, T3, T4, T5> extends AbstractArchetypeImpl implements Archetype.Of5<T1, T2, T3, T4, T5> {

        private ArchetypeImpl5(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
                Class<T5> component5) {

            super(component1, component2, component3, component4, component5);
        }

        @Override
        public int create(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5) {
            return createEntity(component1, component2, component3, component4, component5);
        }

        @Override
        public int[] createBatch(int count, Initialize<Init<T1, T2, T3, T4, T5>> init) {
            return createEntities(count, init);
        }

    }

    private class ArchetypeImpl6<T1, T2, T3, T4, T5, T6> extends AbstractArchetypeImpl implements Archetype.Of6<T1, T2, T3, T4, T5, T6> {

        private ArchetypeImpl6(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
                Class<T5> component5, Class<T6> component6) {

            super(component1, component2, component3, component4, component5, component6);
        }

        @Override
        public int create(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6) {
            return createEntity(component1, component2, component3, component4, component5, component6);
        }

        @Override
        public int[] createBatch(int count, Initialize<Init<T1, T2, T3, T4, T5, T6>> init) {
            return createEntities(count, init);
        }

    }

    private class ArchetypeImpl7<T1, T2, T3, T4, T5, T6, T7> extends AbstractArchetypeImpl implements Archetype.Of7<T1, T2, T3, T4, T5, T6, T7> {

        private ArchetypeImpl7(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
                Class<T5> component5, Class<T6> component6, Class<T7> component7) {

            super(component1, component2, component3, component4, component5, component6, component7);
        }

        @Override
        public int create(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, T7 component7) {
            return createEntity(component1, component2, component3, component4, component5, component6, component7);
        }

        @Override
        public int[] createBatch(int count, Initialize<Init<T1, T2, T3, T4, T5, T6, T7>> init) {
            return createEntities(count, init);
        }

    }

    private class ArchetypeImpl8<T1, T2, T3, T4, T5, T6, T7, T8> extends AbstractArchetypeImpl implements Archetype.Of8<T1, T2, T3, T4, T5, T6, T7, T8> {

        private ArchetypeImpl8(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
                Class<T5> component5, Class<T6> component6, Class<T7> component7, Class<T8> component8) {

            super(component1, component2, component3, component4, component5, component6, component7, component8);
        }

        @Override
        public int create(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, T7 component7, T8 component8) {
            return createEntity(component1, component2, component3, component4, component5, component6, component7, component8);
        }

        @Override
        public int[] createBatch(int count, Initialize<Init<T1, T2, T3, T4, T5, T6, T7, T8>> init) {
            return createEntities(count, init);
        }

    }

    private class ArchetypeImplN<T1, T2, T3, T4, T5, T6, T7, T8> extends AbstractArchetypeImpl implements Archetype.OfN<T1, T2, T3, T4, T5, T6, T7, T8> {

        private ArchetypeImplN(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
                Class<T5> component5, Class<T6> component6, Class<T7> component7, Class<T8> component8, Class<?>[] others) {

            super(ArrayUtils.concat(Class.class, new Class<?>[] { component1, component2, component3, component4, component5, component6, component7, component8 }, others));
        }

        @Override
        public int create(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, T7 component7, T8 component8, Object... others) {
            return createEntity(ArrayUtils.concat(Object.class, new Object[] { component1, component2, component3, component4, component5, component6, component7, component8 }, others));
        }

        @Override
        public int[] createBatch(int count, Initialize<Init<T1, T2, T3, T4, T5, T6, T7, T8>> init) {
            return createEntities(count, init);
        }

    }

}
