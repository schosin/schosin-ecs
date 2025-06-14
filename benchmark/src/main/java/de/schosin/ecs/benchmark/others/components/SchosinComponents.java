package de.schosin.ecs.benchmark.others.components;

import de.schosin.ecs.api.Pooled;

public interface SchosinComponents {

    interface Schosin12 {
    }

    interface Schosin1234 {
    }

    interface Schosin {
    }

    class Schosin1 implements Schosin, Schosin1234, Schosin12 {
    }

    class Schosin2 implements Schosin, Schosin1234, Schosin12 {
    }

    class Schosin3 implements Schosin, Schosin1234 {
    }

    class Schosin4 implements Schosin, Schosin1234 {
    }

    class Schosin5 implements Schosin {
    }

    class Schosin6 implements Schosin {
    }

    class Schosin7 implements Schosin {
    }

    class Schosin8 implements Schosin {
    }

    class Pooled1 implements Pooled {
    }

    class Pooled2 implements Pooled {
    }

    class Pooled3 implements Pooled {
    }

    class Pooled4 implements Pooled {
    }

    class Pooled5 implements Pooled {
    }

    class Pooled6 implements Pooled {
    }

}
