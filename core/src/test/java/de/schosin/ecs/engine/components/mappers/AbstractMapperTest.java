package de.schosin.ecs.engine.components.mappers;

import org.junit.jupiter.api.BeforeEach;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.api.components.mappers.ComponentMapper.EnumComponentMapper;
import de.schosin.ecs.api.components.mappers.ComponentMapper.PooledComponentMapper;
import de.schosin.ecs.engine.AbstractWorldTest;

public abstract class AbstractMapperTest extends AbstractWorldTest {

    ComponentMapper<Component1> component1;
    ComponentMapper<Component2> component2;
    PooledComponentMapper<PooledComponent> pooledComponent;
    EnumComponentMapper<EnumComponent> firstEnum;
    EnumComponentMapper<EnumComponent> secondEnum;

    @BeforeEach
    void setup() {
        this.component1 = world.getComponents(Component1.class);
        this.component2 = world.getComponents(Component2.class);
        this.pooledComponent = world.getPooledComponents(PooledComponent.class);
        this.firstEnum = world.getEnumComponents(EnumComponent.FIRST);
        this.secondEnum = world.getEnumComponents(EnumComponent.SECOND);
    }

    interface Regular {
    }

    public record Component1() implements Regular {
    }

    public record Component2(String data) implements Regular {
    }

    public record Component3() implements Regular {
    }

    public enum Related {
        Related
    }

    public enum Parent implements Exclusive {
        Parent
    }

    public static class PooledComponent implements Pooled {

        String data;

        @Override
        public void reset() {
            this.data = null;
        }
    }

    enum EnumComponent {
        FIRST, SECOND
    }

}
