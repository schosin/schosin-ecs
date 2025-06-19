package de.schosin.ecs.benchmark;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Fork(value = 3)
@Warmup(iterations = 3, timeUnit = TimeUnit.MILLISECONDS, time = 5000)
@Measurement(iterations = 5, timeUnit = TimeUnit.MILLISECONDS, time = 5000)
public class BaseBenchmark {

    static {
        System.setProperty("dominion.show-banner", "false");
    }

    public static String benchmarkName(Class<?> clazz) {
        return clazz.getName().replace('$', '.');
    }

}
