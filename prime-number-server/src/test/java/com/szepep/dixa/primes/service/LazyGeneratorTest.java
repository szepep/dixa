package com.szepep.dixa.primes.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

class LazyGeneratorTest {

    @Test
    void generateFirstFewPrimes() {
        var generator = new LazyGenerator();
        var primesUntil10 = generator.primesUntil(10).collect(toList());

        assertEquals(Lists.list(2, 3, 5, 7), primesUntil10);
    }

    @Test
    void generatePrimesUntilPrime() {
        var generator = new LazyGenerator();
        var primesUntil10 = generator.primesUntil(13).collect(toList());

        assertEquals(Lists.list(2, 3, 5, 7, 11, 13), primesUntil10);
    }

    @Test
    void generateLargeNumberDosNotThrowException() {
        var generator = new LazyGenerator();
        generator.primesUntil(100_000).collect(toList());
    }

    @Test
    void primesAreCalculatedOnlyOnce() {
        try (var log = new LogMessages(LazyGenerator.class)) {
            var generator = new LazyGenerator();
            generator.primesUntil(10).collect(toList());
            assertEquals(5, log.messages().size(), "Primes 2, 3, 5, 11 must be logged");

            generator.primesUntil(10).collect(toList());
            assertEquals(5, log.messages().size(), "no new messages should be present");

            generator.primesUntil(13).collect(toList());
            assertEquals(6, log.messages().size(), "13 is identified as prime");
        }
    }

    @Test
    void generatorIsLazy() {
        try (var log = new LogMessages(LazyGenerator.class)) {
            var generator = new LazyGenerator();
            AtomicInteger counter = new AtomicInteger(0);
            generator.primesUntil(10).forEach(p ->
                    assertEquals(counter.incrementAndGet(), log.messages().size(),
                            "Only consumed primes are computed, false for " + p)
            );
        }
    }

    @Test
    void parallelRunsAreCorrect() {
        var sharedGenerator = new LazyGenerator();

        var runs = 20;

        var executor = Executors.newFixedThreadPool(runs);
        var futures = IntStream.range(0, runs)
                .map(i -> (i + 1) * 10)
                .mapToObj(i -> executor.submit(() -> {
                    var shared = sharedGenerator.primesUntil(i).collect(toList());
                    var notShared = new LazyGenerator().primesUntil(i).collect(toList());
                    return shared.equals(notShared);
                }))
                .collect(toList());

        futures.forEach(f -> {
            try {
                assertTrue(f.get());
            } catch (InterruptedException | ExecutionException e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    void parallelRunsAreUsingCachedData() {
        var runs = 20;
        // the underlying algorithm computes until the next prime after
        var expectedPrimes = new LazyGenerator().primesUntil(runs * 10).count() + 1;

        try (var log = new LogMessages(LazyGenerator.class)) {
            var sharedGenerator = new LazyGenerator();

            var executor = Executors.newFixedThreadPool(runs);
            var futures = IntStream.range(0, runs)
                    .map(i -> (i + 1) * 10)
                    .mapToObj(i -> executor.submit(() -> {
                        sharedGenerator.primesUntil(i).collect(toList());
                    }))
                    .collect(toList());

            futures.forEach(f -> {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    fail(e.getMessage());
                }
            });

            assertEquals(expectedPrimes, log.messages().size());
        }
    }

    static final class LogMessages implements AutoCloseable {

        private final Logger logger;
        private final Level level;
        private final ListAppender<ILoggingEvent> appender;

        <T> LogMessages(Class<T> clazz) {
            logger = (Logger) LoggerFactory.getLogger(clazz.getCanonicalName());
            level = logger.getLevel();
            logger.setLevel(Level.ALL);
            appender = new ListAppender<>();
            logger.addAppender(appender);
            appender.start();
        }

        List<ILoggingEvent> messages() {
            return appender.list;
        }

        @Override
        public void close() {
            appender.stop();
            logger.detachAppender(appender);
            logger.setLevel(level);
        }
    }

}