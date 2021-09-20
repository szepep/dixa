package com.szepep.dixa.primes.service;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterators;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class EratosthenesGeneratorTest {

    @Test
    @Disabled("Long running test")
    public void multithreadedAccessOfEratosthenes() {

        var pool = Executors.newCachedThreadPool();

        var erat = new EratosthenesGenerator();
        var lazy = new LazyGenerator();

        var futures = IntStream.range(0, 100)
                .mapToObj(i -> pool.submit(() -> {
                    var n = ThreadLocalRandom.current().nextInt(1_000, 5_000_000);

                    var eratPrimes = erat.primesUntil(n).iterator();
                    var lazyPrimes = lazy.primesUntil(n).iterator();

                    return Iterators.elementsEqual(eratPrimes, lazyPrimes);

                })).collect(Collectors.toList());

        futures.forEach(f -> {
            try {
                assertTrue(f.get(), "EratosthenesGenerator produced different result");
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

    }

    @Test
    @Disabled("Long running test")
    public void performanceComparison() {

        AtomicLong eratTime = new AtomicLong();
        AtomicLong noboTime = new AtomicLong();
        AtomicLong lazyTime = new AtomicLong();

        var runs = 20;

        for (int j = 0; j < runs; ++j) {
            var erat = new EratosthenesGenerator();
            var nobo = new NonBlockingEratosthenesGenerator();
            var lazy = new LazyGenerator();
            var numbers = IntStream.range(0, 20)
                    .mapToObj(i -> ThreadLocalRandom.current().nextInt(1_000_000, 5_000_000))
                    .collect(Collectors.toList());

            runTest(numbers, n -> eratTime.addAndGet(measure(() -> erat.primesUntil(n).forEach(p -> {/* do nothing*/}))));
            runTest(numbers, n -> noboTime.addAndGet(measure(() -> nobo.primesUntil(n).forEach(p -> {/* do nothing*/}))));
            runTest(numbers, n -> lazyTime.addAndGet(measure(() -> lazy.primesUntil(n).forEach(p -> {/* do nothing*/}))));
        }

        log.info("\n" +
                        "EratosthenesGenerator: {}ms\n" +
                        "NonBlockingEratosthenesGenerator: {}ms\n" +
                        "LazyGenerator: {}ms",
                eratTime.get() / runs, noboTime.get() / runs, lazyTime.get() / runs);
    }

    private void runTest(List<Integer> numbers, Consumer<Integer> block) {
        var pool = Executors.newCachedThreadPool();
        var futures = numbers.stream()
                .map(n -> pool.submit(() -> block.accept(n)))
                .collect(Collectors.toList());

        futures.forEach(f -> {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
        pool.shutdown();
    }

    private long measure(Runnable block) {
        var sw = Stopwatch.createStarted();
        block.run();
        return sw.elapsed(TimeUnit.MILLISECONDS);
    }

}