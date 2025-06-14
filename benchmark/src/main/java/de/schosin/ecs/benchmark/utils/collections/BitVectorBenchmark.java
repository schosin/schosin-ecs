package de.schosin.ecs.benchmark.utils.collections;

import static de.schosin.ecs.benchmark.BaseBenchmark.benchmarkName;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import de.schosin.ecs.benchmark.EcsBenchmark;
import de.schosin.ecs.utils.collections.BitVector;
import de.schosin.ecs.utils.collections.IntBag;

public class BitVectorBenchmark {

    public static void main(String[] args) throws Exception {
        var options = new OptionsBuilder()
                .include(benchmarkName(ContainsBenchmarks.class))
                .include(benchmarkName(IterationBenchmarks.class))
                .build();

        new Runner(options).run();
    }

    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public static class ContainsBenchmarks extends EcsBenchmark {

        public static void main(String[] args) throws Exception {
            var options = new OptionsBuilder()
                    .include(benchmarkName(ContainsBenchmarks.class))
                    .build();

            new Runner(options).run();
        }

        BitVector vector12;
        BitVector vector1;
        BitVector vector2;
        BitVector vector23;

        @Param({ "4", "64", "512", "9172" })
        private int length;

        @Setup(Level.Trial)
        public void init() {
            this.vector12 = new BitVector();
            this.vector1 = new BitVector();
            this.vector2 = new BitVector();
            this.vector23 = new BitVector();

            for (int i = 0; i < length; i++) {
                if (i % 4 == 0) {
                    vector1.set(i);
                    vector12.set(i);
                }
                if (i % 4 == 1) {
                    vector2.set(i);
                    vector12.set(i);
                    vector23.set(i);
                }
                if (i % 4 == 2) {
                    vector23.set(i);
                }
            }
        }

        @Benchmark
        public void containsAll(Blackhole bh) {
            bh.consume(this.vector1.containsAll(vector2));
            bh.consume(this.vector1.containsAll(vector12));
            bh.consume(this.vector1.containsAll(vector23));

            bh.consume(this.vector2.containsAll(vector1));
            bh.consume(this.vector12.containsAll(vector1));
            bh.consume(this.vector23.containsAll(vector1));
        }

        @Benchmark
        public void containsNone(Blackhole bh) {
            bh.consume(this.vector1.containsNone(vector2));
            bh.consume(this.vector1.containsNone(vector12));
            bh.consume(this.vector1.containsNone(vector23));

            bh.consume(this.vector2.containsNone(vector1));
            bh.consume(this.vector12.containsNone(vector1));
            bh.consume(this.vector23.containsNone(vector1));
        }

        @Benchmark
        public void containsSome(Blackhole bh) {
            bh.consume(this.vector1.containsSome(vector2));
            bh.consume(this.vector1.containsSome(vector12));
            bh.consume(this.vector1.containsSome(vector23));

            bh.consume(this.vector2.containsSome(vector1));
            bh.consume(this.vector12.containsSome(vector1));
            bh.consume(this.vector23.containsSome(vector1));
        }

    }

    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 3, timeUnit = TimeUnit.MILLISECONDS, time = 3000)
    @Measurement(iterations = 3, timeUnit = TimeUnit.MILLISECONDS, time = 3000)
    public static class IterationBenchmarks extends EcsBenchmark {

        public static void main(String[] args) throws Exception {
            var options = new OptionsBuilder()
                    .include(benchmarkName(IterationBenchmarks.class))
                    .build();

            new Runner(options).run();
        }

        IntBag bag;

        BitVector vector;

        @Param({ "4", /*"64", "512",*/ "9172", "1000000" })
        private int length;

        @Param({ "5", "50", "75" })
        private int loadPercentage;

        @Setup(Level.Trial)
        public void init() {
            this.vector = new BitVector(length);

            for (int i = 0; i < length; i++) {
                if ((i % 100) <= loadPercentage) {
                    vector.set(i);
                }
            }
        }

        @Setup(Level.Invocation)
        public void clear() {
            this.bag = new IntBag(length * 2);
        }

        @Benchmark
        public void nextSetBit(Blackhole bh) {
            var idx = vector.nextSetBit(0);
            while (idx > -1) {
                bh.consume(idx = vector.nextSetBit(idx + 1));
            }
        }

        @Benchmark
        public void iterate() {
            vector.iterate(bag::add);
        }

    }

}
