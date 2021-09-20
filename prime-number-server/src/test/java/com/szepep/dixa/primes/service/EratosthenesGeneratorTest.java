package com.szepep.dixa.primes.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class EratosthenesGeneratorTest {

    @Test
    public void multithreadedAccessOfEratosthenes() {

        var pool = Executors.newCachedThreadPool();

        var erat = new EratosthenesGenerator();
        var lazy = new LazyGenerator();

        var futures = IntStream.range(0, 100)
                .mapToObj(i -> pool.submit(() -> {
                    var n = ThreadLocalRandom.current().nextInt(1_000, 5_000_000);

                    var eratPrimes = erat.primesUntil(n).iterator();
                    var lazyPrimes = lazy.primesUntil(n).iterator();

                    while (eratPrimes.hasNext()) {
                        if (!lazyPrimes.hasNext()) {
                            log.error("Different number of primes");
                            return false;
                        }

                        int p1 = eratPrimes.next();
                        int p2 = lazyPrimes.next();
                        if (p1 != p2) {
                            log.error("{} vs {}", p1, p2);
                            return false;
                        }
                    }
                    if (lazyPrimes.hasNext()) {
                        log.error("Different number of primes");
                        return false;
                    }

                    return true;

                })).collect(Collectors.toList());

        futures.forEach(f -> {
            try {
                assertTrue(f.get(), "EratosthenesGenerator produced different result");
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

    }

}