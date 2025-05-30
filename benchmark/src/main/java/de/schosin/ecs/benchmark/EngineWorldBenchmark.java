package de.schosin.ecs.benchmark;

import static de.schosin.ecs.benchmark.EcsBenchmark.benchmarkName;

import java.util.ArrayList;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.plugins.archetype.Archetype1;
import de.schosin.ecs.plugins.archetype.Archetype2;
import de.schosin.ecs.plugins.archetype.Archetype3;
import de.schosin.ecs.plugins.archetype.Archetype4;
import de.schosin.ecs.plugins.archetype.Archetype5;
import de.schosin.ecs.plugins.archetype.Archetype6;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.transmuter.Transmuter;

public class EngineWorldBenchmark {

    public static void main(String[] args) throws Exception {
        var options = new OptionsBuilder()
                .include(benchmarkName(AddEntityBenchmark.class))
                .include(benchmarkName(RemoveEntityBenchmark.class))
                .include(benchmarkName(AddComponentBenchmark.class))
                .include(benchmarkName(IterateCompositionBenchmark.class))
                .build();

        new Runner(options).run();
    }

    public static class AddEntityBenchmark extends EcsBenchmark {

        public static void main(String[] args) throws Exception {
            var options = new OptionsBuilder()
                    .include(benchmarkName(AddEntityBenchmark.class))
                    .build();

            new Runner(options).run();
        }

        private Archetype1<Component1> archetype1;
        private Archetype2<Component1, Component2> archetype2;
        private Archetype3<Component1, Component2, Component3> archetype3;
        private Archetype4<Component1, Component2, Component3, Component4> archetype4;
        private Archetype5<Component1, Component2, Component3, Component4, Component5> archetype5;
        private Archetype6<Component1, Component2, Component3, Component4, Component5, Component6> archetype6;

        private Archetype1<Pooled1> pooledArchetype1;
        private Archetype2<Pooled1, Pooled2> pooledArchetype2;
        private Archetype3<Pooled1, Pooled2, Pooled3> pooledArchetype3;
        private Archetype4<Pooled1, Pooled2, Pooled3, Pooled4> pooledArchetype4;
        private Archetype5<Pooled1, Pooled2, Pooled3, Pooled4, Pooled5> pooledArchetype5;
        private Archetype6<Pooled1, Pooled2, Pooled3, Pooled4, Pooled5, Pooled6> pooledArchetype6;

        @Param({ "1000000" })
        private int entityCount;

        @Param({ "2", "6" })
        private int components;

        @Param({ "0", "2", "8" })
        private int compositions;

        private int[] entities;

        @Setup(Level.Trial)
        public void init(Blackhole bh) {
            setupWorld();

            if (components < 1 || components > 6) {
                throw new IllegalArgumentException("components parameter only implemented for values 1..6 inclusive");
            }

            if (compositions < 0 || compositions > 36) {
                throw new IllegalArgumentException("compositions parameter only implemented for values 0..36 inclusive");
            }

            this.archetype1 = world.createArchetype(Component1.class);
            this.archetype2 = world.createArchetype(Component1.class, Component2.class);
            this.archetype3 = world.createArchetype(Component1.class, Component2.class, Component3.class);
            this.archetype4 = world.createArchetype(Component1.class, Component2.class, Component3.class, Component4.class);
            this.archetype5 = world.createArchetype(Component1.class, Component2.class, Component3.class, Component4.class, Component5.class);
            this.archetype6 = world.createArchetype(Component1.class, Component2.class, Component3.class, Component4.class, Component5.class, Component6.class);

            this.pooledArchetype1 = world.createArchetype(Pooled1.class);
            this.pooledArchetype2 = world.createArchetype(Pooled1.class, Pooled2.class);
            this.pooledArchetype3 = world.createArchetype(Pooled1.class, Pooled2.class, Pooled3.class);
            this.pooledArchetype4 = world.createArchetype(Pooled1.class, Pooled2.class, Pooled3.class, Pooled4.class);
            this.pooledArchetype5 = world.createArchetype(Pooled1.class, Pooled2.class, Pooled3.class, Pooled4.class, Pooled5.class);
            this.pooledArchetype6 = world.createArchetype(Pooled1.class, Pooled2.class, Pooled3.class, Pooled4.class, Pooled5.class, Pooled6.class);

            if (compositions > 0) {
                var classes = new Class<?>[] { Component1.class, Component2.class, Component3.class, Component4.class, Component5.class, Component6.class };

                var all = new ArrayList<Class<?>>(6);
                var one = new ArrayList<Class<?>>(6);
                var none = new ArrayList<Class<?>>(6);

                for (int i = 0; i < compositions; i++) {
                    var type = i / 6;
                    if (type == 0) {
                        all.add(classes[i % 6]);
                    } else if (type == 1) {
                        one.add(classes[i % 6]);
                    } else if (type == 2) {
                        none.add(classes[i % 6]);
                    }

                    var composition = Composition
                            .all(all.toArray(Class<?>[]::new))
                            .one(one.toArray(Class<?>[]::new))
                            .none(none.toArray(Class<?>[]::new));

                    world.createComposition(composition).inserted(bh::consume);
                }
            }

            this.entities = new int[entityCount];
        }

        @Setup(Level.Invocation)
        public void clean() {
            for (int i = 0; i < entityCount; i++) {
                world.deleteEntity(entities[i]);
            }
            world.process();
        }

        @Benchmark
        public void dynamic(Blackhole bh) {
            for (int i = 0; i < entityCount; i++) {
                bh.consume(entities[i] = switch (components) {
                    case 1 -> world.createEntity(new Component1());
                    case 2 -> world.createEntity(new Component1(), new Component2());
                    case 3 -> world.createEntity(new Component1(), new Component2(), new Component3());
                    case 4 -> world.createEntity(new Component1(), new Component2(), new Component3(), new Component4());
                    case 5 -> world.createEntity(new Component1(), new Component2(), new Component3(), new Component4(), new Component5());
                    case 6 -> world.createEntity(new Component1(), new Component2(), new Component3(), new Component4(), new Component5(), new Component6());
                    default -> throw new IllegalArgumentException("Unexpected value: " + components);
                });
            }
        }

        @Benchmark
        public void archetype(Blackhole bh) {
            for (int i = 0; i < entityCount; i++) {
                bh.consume(entities[i] = switch (components) {
                    case 1 -> archetype1.create(new Component1());
                    case 2 -> archetype2.create(new Component1(), new Component2());
                    case 3 -> archetype3.create(new Component1(), new Component2(), new Component3());
                    case 4 -> archetype4.create(new Component1(), new Component2(), new Component3(), new Component4());
                    case 5 -> archetype5.create(new Component1(), new Component2(), new Component3(), new Component4(), new Component5());
                    case 6 -> archetype6.create(new Component1(), new Component2(), new Component3(), new Component4(), new Component5(), new Component6());
                    default -> throw new IllegalArgumentException("Unexpected value: " + components);
                });
            }
        }

        @Benchmark
        public void archetypeBatch(Blackhole bh) {
            bh.consume(entities = switch (components) {
                case 1 -> archetype1.createBatch(entityCount, () -> new Component1());
                case 2 -> archetype2.createBatch(entityCount, init -> init.create(new Component1(), new Component2()));
                case 3 -> archetype3.createBatch(entityCount, init -> init.create(new Component1(), new Component2(), new Component3()));
                case 4 -> archetype4.createBatch(entityCount, init -> init.create(new Component1(), new Component2(), new Component3(), new Component4()));
                case 5 -> archetype5.createBatch(entityCount, init -> init.create(new Component1(), new Component2(), new Component3(), new Component4(), new Component5()));
                case 6 -> archetype6.createBatch(entityCount, init -> init.create(new Component1(), new Component2(), new Component3(), new Component4(), new Component5(), new Component6()));
                default -> throw new IllegalArgumentException("Unexpected value: " + components);
            });
        }

        @Benchmark
        public void pooledArchetypeBatch(Blackhole bh) {
            bh.consume(entities = switch (components) {
                case 1 -> pooledArchetype1.createBatch(entityCount, () -> pooledArchetype1.getInstance(Pooled1.class));
                case 2 -> pooledArchetype2.createBatch(entityCount, init -> init.create(pooledArchetype1.getInstance(Pooled1.class), pooledArchetype1.getInstance(Pooled2.class)));
                case 3 -> pooledArchetype3.createBatch(entityCount, init -> init.create(pooledArchetype1.getInstance(Pooled1.class), pooledArchetype1.getInstance(Pooled2.class),
                        pooledArchetype1.getInstance(Pooled3.class)));
                case 4 -> pooledArchetype4.createBatch(entityCount, init -> init.create(pooledArchetype1.getInstance(Pooled1.class), pooledArchetype1.getInstance(Pooled2.class),
                        pooledArchetype1.getInstance(Pooled3.class), pooledArchetype1.getInstance(Pooled4.class)));
                case 5 -> pooledArchetype5.createBatch(entityCount, init -> init.create(pooledArchetype1.getInstance(Pooled1.class), pooledArchetype1.getInstance(Pooled2.class),
                        pooledArchetype1.getInstance(Pooled3.class), pooledArchetype1.getInstance(Pooled4.class), pooledArchetype1.getInstance(Pooled5.class)));
                case 6 -> pooledArchetype6.createBatch(entityCount, init -> init.create(pooledArchetype1.getInstance(Pooled1.class), pooledArchetype1.getInstance(Pooled2.class),
                        pooledArchetype1.getInstance(Pooled3.class), pooledArchetype1.getInstance(Pooled4.class), pooledArchetype1.getInstance(Pooled5.class),
                        pooledArchetype1.getInstance(Pooled6.class)));
                default -> throw new IllegalArgumentException("Unexpected value: " + components);
            });
        }

    }

    public static class RemoveEntityBenchmark extends EcsBenchmark {

        public static void main(String[] args) throws Exception {
            var options = new OptionsBuilder()
                    .include(benchmarkName(RemoveEntityBenchmark.class))
                    .build();

            new Runner(options).run();
        }

        private Archetype1<Component1> archetype1;
        private Archetype2<Component1, Component2> archetype2;
        private Archetype3<Component1, Component2, Component3> archetype3;
        private Archetype4<Component1, Component2, Component3, Component4> archetype4;
        private Archetype5<Component1, Component2, Component3, Component4, Component5> archetype5;
        private Archetype6<Component1, Component2, Component3, Component4, Component5, Component6> archetype6;

        @Param({ "1000000" })
        private int entityCount;

        @Param({ "2", "6" })
        private int components;

        @Param({ "0", "2", "8" })
        private int compositions;

        private int[] entities;

        @Setup(Level.Trial)
        public void init(Blackhole bh) {
            setupWorld();

            if (components < 1 || components > 6) {
                throw new IllegalArgumentException("components parameter only implemented for values 1..6 inclusive");
            }

            if (compositions < 0 || compositions > 36) {
                throw new IllegalArgumentException("compositions parameter only implemented for values 0..36 inclusive");
            }

            this.archetype1 = world.createArchetype(Component1.class);
            this.archetype2 = world.createArchetype(Component1.class, Component2.class);
            this.archetype3 = world.createArchetype(Component1.class, Component2.class, Component3.class);
            this.archetype4 = world.createArchetype(Component1.class, Component2.class, Component3.class, Component4.class);
            this.archetype5 = world.createArchetype(Component1.class, Component2.class, Component3.class, Component4.class, Component5.class);
            this.archetype6 = world.createArchetype(Component1.class, Component2.class, Component3.class, Component4.class, Component5.class, Component6.class);

            if (compositions > 0) {
                var classes = new Class<?>[] { Component1.class, Component2.class, Component3.class, Component4.class, Component5.class, Component6.class };

                var all = new ArrayList<Class<?>>(6);
                var one = new ArrayList<Class<?>>(6);
                var none = new ArrayList<Class<?>>(6);

                for (int i = 0; i < compositions; i++) {
                    var type = i / 6;
                    if (type == 0) {
                        all.add(classes[i % 6]);
                    } else if (type == 1) {
                        one.add(classes[i % 6]);
                    } else if (type == 2) {
                        none.add(classes[i % 6]);
                    }

                    var composition = Composition
                            .all(all.toArray(Class<?>[]::new))
                            .one(one.toArray(Class<?>[]::new))
                            .none(none.toArray(Class<?>[]::new));

                    world.createComposition(composition).removed(bh::consume);
                }
            }

            this.entities = new int[entityCount];
        }

        @Setup(Level.Invocation)
        public void clean(Blackhole bh) {
            for (int i = 0; i < entityCount; i++) {
                bh.consume(entities[i] = switch (components) {
                    case 1 -> archetype1.create(new Component1());
                    case 2 -> archetype2.create(new Component1(), new Component2());
                    case 3 -> archetype3.create(new Component1(), new Component2(), new Component3());
                    case 4 -> archetype4.create(new Component1(), new Component2(), new Component3(), new Component4());
                    case 5 -> archetype5.create(new Component1(), new Component2(), new Component3(), new Component4(), new Component5());
                    case 6 -> archetype6.create(new Component1(), new Component2(), new Component3(), new Component4(), new Component5(), new Component6());
                    default -> throw new IllegalArgumentException("Unexpected value: " + components);
                });
            }

            world.process();
        }

        @Benchmark
        public void deleteEntity() {
            for (int i = 0; i < entityCount; i++) {
                world.deleteEntity(entities[i]);
            }

            world.process();
        }

    }

    public static class AddComponentBenchmark extends EcsBenchmark {

        public static void main(String[] args) throws Exception {
            var options = new OptionsBuilder()
                    .include(benchmarkName(AddComponentBenchmark.class))
                    .build();

            new Runner(options).run();
        }

        private ComponentMapper<Component1> component1;
        private ComponentMapper<Component2> component2;
        private ComponentMapper<Component3> component3;
        private ComponentMapper<Component4> component4;
        private ComponentMapper<Component5> component5;
        private ComponentMapper<Component6> component6;

        private Transmuter.Add1<Component1> transmuter1;
        private Transmuter.Add2<Component1, Component2> transmuter2;
        private Transmuter.Add3<Component1, Component2, Component3> transmuter3;
        private Transmuter.Add4<Component1, Component2, Component3, Component4> transmuter4;
        private Transmuter.Add5<Component1, Component2, Component3, Component4, Component5> transmuter5;
        private Transmuter.Add6<Component1, Component2, Component3, Component4, Component5, Component6> transmuter6;

        @Param({ "1000000" })
        private int entityCount;

        @Param({ "2", "6" })
        private int components;

        @Param({ "0", "2", "8" })
        private int compositions;

        private int[] entities;

        @Setup(Level.Trial)
        public void init(Blackhole bh) {
            setupWorld();

            if (components < 1 || components > 6) {
                throw new IllegalArgumentException("components parameter only implemented for values 1..6 inclusive");
            }

            if (compositions < 0 || compositions > 36) {
                throw new IllegalArgumentException("compositions parameter only implemented for values 0..36 inclusive");
            }

            this.component1 = world.getComponents(Component1.class);
            this.component2 = world.getComponents(Component2.class);
            this.component3 = world.getComponents(Component3.class);
            this.component4 = world.getComponents(Component4.class);
            this.component5 = world.getComponents(Component5.class);
            this.component6 = world.getComponents(Component6.class);

            this.transmuter1 = world.createTransmuter(Transmuter.add(Component1.class));
            this.transmuter2 = world.createTransmuter(Transmuter.add(Component1.class, Component2.class));
            this.transmuter3 = world.createTransmuter(Transmuter.add(Component1.class, Component2.class, Component3.class));
            this.transmuter4 = world.createTransmuter(Transmuter.add(Component1.class, Component2.class, Component3.class, Component4.class));
            this.transmuter5 = world.createTransmuter(Transmuter.add(Component1.class, Component2.class, Component3.class, Component4.class, Component5.class));
            this.transmuter6 = world.createTransmuter(Transmuter.add(Component1.class, Component2.class, Component3.class, Component4.class, Component5.class, Component6.class));

            if (compositions > 0) {
                var classes = new Class<?>[] { Component1.class, Component2.class, Component3.class, Component4.class, Component5.class, Component6.class };

                var all = new ArrayList<Class<?>>(6);
                var one = new ArrayList<Class<?>>(6);
                var none = new ArrayList<Class<?>>(6);

                for (int i = 0; i < compositions; i++) {
                    var type = i / 6;
                    if (type == 0) {
                        all.add(classes[i % 6]);
                    } else if (type == 1) {
                        one.add(classes[i % 6]);
                    } else if (type == 2) {
                        none.add(classes[i % 6]);
                    }

                    var builder = Composition
                            .all(all.toArray(Class<?>[]::new))
                            .one(one.toArray(Class<?>[]::new))
                            .none(none.toArray(Class<?>[]::new));

                    var composition = world.createComposition(builder);
                    composition.inserted(bh::consume);
                    composition.removed(bh::consume);
                }
            }

            this.entities = new int[entityCount];
        }

        @Setup(Level.Invocation)
        public void clean() {
            for (int i = 0; i < entityCount; i++) {
                entities[i] = world.createEntity();
            }

            world.process();
        }

        @TearDown(Level.Invocation)
        public void tearDown() {
            for (int i = 0; i < entityCount; i++) {
                world.deleteEntity(entities[i]);
            }

            world.process();
        }

        @Benchmark
        public void components() {
            for (int i = 0; i < entityCount; i++) {
                var entityId = entities[i];

                if (components > 0) {
                    component1.add(entityId, new Component1());
                }
                if (components > 1) {
                    component2.add(entityId, new Component2());
                }
                if (components > 2) {
                    component3.add(entityId, new Component3());
                }
                if (components > 3) {
                    component4.add(entityId, new Component4());
                }
                if (components > 4) {
                    component5.add(entityId, new Component5());
                }
                if (components == 6) {
                    component6.add(entityId, new Component6());
                }
            }

            world.process();
        }

        @Benchmark
        public void transmuter() {
            for (int i = 0; i < entityCount; i++) {
                var entityId = entities[i];

                if (components == 1) {
                    transmuter1.apply(entityId, new Component1());
                } else if (components == 2) {
                    transmuter2.apply(entityId, new Component1(), new Component2());
                } else if (components == 3) {
                    transmuter3.apply(entityId, new Component1(), new Component2(), new Component3());
                } else if (components == 4) {
                    transmuter4.apply(entityId, new Component1(), new Component2(), new Component3(), new Component4());
                } else if (components == 5) {
                    transmuter5.apply(entityId, new Component1(), new Component2(), new Component3(), new Component4(), new Component5());
                } else if (components == 6) {
                    transmuter6.apply(entityId, new Component1(), new Component2(), new Component3(), new Component4(), new Component5(), new Component6());
                }
            }

            world.process();
        }

    }

    public static class IterateCompositionBenchmark extends EcsBenchmark {

        public static void main(String[] args) throws Exception {
            var options = new OptionsBuilder()
                    .include(benchmarkName(IterateCompositionBenchmark.class))
                    .build();

            new Runner(options).run();
        }

        private Archetype1<Component1> archetype1;
        private Archetype2<Component1, Component2> archetype12;

        private Composition composition1;
        private Composition composition1not2;
        private Composition compositionnot2;

        @Param({ "1000000" })
        private int entityCount;

        private int[] entities;

        @Setup(Level.Trial)
        public void init() {
            setupWorld();

            this.archetype1 = world.createArchetype(Component1.class);
            this.archetype12 = world.createArchetype(Component1.class, Component2.class);

            this.composition1 = world.createComposition(Composition.all(Component1.class));
            this.composition1not2 = world.createComposition(Composition.all(Component1.class).none(Component2.class));
            this.compositionnot2 = world.createComposition(Composition.none(Component2.class));

            this.entities = new int[entityCount];
            for (int i = 0; i < entityCount; i++) {
                if (i % 2 == 0) {
                    entities[i] = archetype1.create(new Component1());
                } else {
                    entities[i] = archetype12.create(new Component1(), new Component2());
                }
            }

            world.process();
        }

        @TearDown
        public void tearDown() {
            for (int i = 0; i < entityCount; i++) {
                world.deleteEntity(entities[i]);
            }

            world.process();
        }

        @Benchmark
        public void composition1(Blackhole bh) {
            composition1.process(bh::consume);
        }

        @Benchmark
        public void composition1not2(Blackhole bh) {
            composition1not2.process(bh::consume);
        }

        @Benchmark
        public void compositionnot2(Blackhole bh) {
            compositionnot2.process(bh::consume);
        }

    }

    private record Component1() {
    }

    private record Component2() {
    }

    private record Component3() {
    }

    private record Component4() {
    }

    private record Component5() {
    }

    private record Component6() {
    }

    public record Pooled1() implements Pooled {
    }

    public record Pooled2() implements Pooled {
    }

    public record Pooled3() implements Pooled {
    }

    public record Pooled4() implements Pooled {
    }

    public record Pooled5() implements Pooled {
    }

    public record Pooled6() implements Pooled {
    }

}
