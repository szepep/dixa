package com.szepep.dixa.primes.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EratosthenesGeneratorTest {

    @Test
    public void multi() {

        var pool = Executors.newCachedThreadPool();

        var erat = new EratosthenesGenerator();
        var lazy = new LazyGenerator();

        var futures = IntStream.range(0, 100)
                .mapToObj(i -> pool.submit(() -> {

                    var n = ThreadLocalRandom.current().nextInt(1_000, 50_000_000);

                    var primes1 = erat.primesUntil(n).iterator();
                    var primes2 = lazy.primesUntil(n).iterator();

                    while (primes1.hasNext()) {

                        Long p1 = primes1.next();
                        Long p2 = primes2.next();
                        if (!p1.equals(p2)) {
                            System.out.println("" + p1 + " " + p2);
                            return false;
                        }
                    }
                    return true;

                })).collect(Collectors.toList());

        futures.forEach(f -> {
            try {
                assertTrue(f.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

    }

}