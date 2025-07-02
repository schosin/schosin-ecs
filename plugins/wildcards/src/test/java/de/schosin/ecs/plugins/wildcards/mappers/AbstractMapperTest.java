package de.schosin.ecs.plugins.wildcards.mappers;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.plugins.wildcards.WildcardWorld;
import de.schosin.ecs.test.AbstractEcsTest;

public abstract class AbstractMapperTest extends AbstractEcsTest<WildcardWorld> {

    interface Regular {
    }

    public record Component1() implements Regular {
    }

    public record Component2(String data) implements Regular {
    }

    public record Component3() implements Regular {
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
