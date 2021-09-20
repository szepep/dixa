package com.szepep.dixa.primes.service;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Generates lazy stream of prime numbers and caches already identified primes.
 */
@Slf4j
@ThreadSafe
public class LazyGenerator implements Generator {

    /**
     * Cache of already computed prime numbers.
     * <p>
     * In single threaded environment it is better to use an ArrayList for this purpose.
     * However in multithreaded environment we can use the atomic
     * {@link ConcurrentHashMap#computeIfAbsent(Object, Function)} to ensure no new prime is calculated multiple times.
     */
    private final ConcurrentHashMap<Integer, Integer> primeNumbers;
    private final AtomicInteger max;

    public LazyGenerator() {
        primeNumbers = new ConcurrentHashMap<>();
        primeNumbers.put(0, 2);
        logPrime(2);

        max = new AtomicInteger(3);
    }

    private static void logPrime(long prime) {
        log.trace("{} identified as prime", prime);
    }

    private static <T> Stream<T> toStream(Iterator<T> primes) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(primes, Spliterator.ORDERED), false);
    }

    private Integer calcNext() {
        int prime;
        //noinspection StatementWithEmptyBody
        while (!isPrime(prime = max.getAndAdd(2))) { /* noop */ }
        return prime;
    }

    private boolean isPrime(int n) {
        boolean prime = true;
        for (int i = 0; prime && primeNumbers.get(i) * primeNumbers.get(i) <= n; ++i) {
            prime = n % primeNumbers.get(i) != 0;
        }
        if (prime) {
            logPrime(n);
        }
        return prime;
    }

    /**
     * Stream of prime numbers. Should not be accessed from multiple threads simultaneously.
     *
     * @param number The limit of the prime numbers in result. No result is larger than number.
     * @return Stream of prime numbers
     * @throws IllegalArgumentException if the input number is wrong.
     */
    @Override
    public Stream<Integer> primesUntil(final int number) throws IllegalArgumentException {
        Preconditions.checkArgument(number >= 0, "The number must be zero or positive");

        var primes = primeNumbersJustAfter(number);
        var primesStream = toStream(primes);
        return primesStream.takeWhile(i -> i <= number);
    }

    /**
     * Returns an iterator of prime numbers, the last element can be larger than number.
     *
     * @param number Limit, the last element can exceed the limit.
     * @return Iterator of prime numbers.
     */
    private Iterator<Integer> primeNumbersJustAfter(int number) {
        return new Iterator<>() {

            private int idx = 0;
            private int lastPrime = -1;

            @Override
            public boolean hasNext() {
                return lastPrime < number;
            }

            @Override
            public Integer next() {
                lastPrime = primeNumbers.computeIfAbsent(idx++, idx -> calcNext());
                return lastPrime;
            }
        };
    }

}
