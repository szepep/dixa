package com.szepep.dixa.primes.service;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.concurrent.ThreadSafe;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Generator using Eratosthenes sieve. Complex solution using read-write lock to lock parts of the computation.
 */
@ThreadSafe
@Slf4j
public final class NonBlockingEratosthenesGenerator implements Generator {

    private final int batchSize = 1_000;

    private final BitSet bits = new BitSet();
    private final ConcurrentHashMap<Integer, ReadWriteLock> locks = new ConcurrentHashMap<>();

    private volatile int max = 2;

    NonBlockingEratosthenesGenerator() {
        bits.clear();
        bits.set(2);
    }

    private void sieve(int n) {
        if (n <= max) return; // other thread already computed

        log.trace("Entering sieve for {}", n);

        // the first thread do the computation, others will wait for the response.
        AtomicBoolean calculateNewPrimes = new AtomicBoolean(false);
        var lock = locks.computeIfAbsent(n, k -> {
            calculateNewPrimes.set(true);
            var l = new ReentrantReadWriteLock();
            l.writeLock().lock();
            log.trace("locker for write for {}", n);
            return l;
        });

        if (calculateNewPrimes.get()) {
            if (n > max) {
                sieveCriticalSection(n);
            } else {
                log.trace("Won the race but already computed {}", n);
            }
            lock.writeLock().unlock();
            locks.remove(n);
            log.trace("unlocked for {}", n);
        } else {
            if (n <= max) {
                log.trace("don't bother, already computed {}", n);
                return;
            }
            log.trace("waiting for read lock for {}", n);
            lock.readLock().lock();
            log.trace("read lock gained for {}", n);
            lock.readLock().unlock();
        }
    }

    private void sieveCriticalSection(int n) {
        log.trace("entering critical section for {}", n);
        int nPlus1 = n + 1;

        bits.clear(max + 1, nPlus1);
        bits.flip(max + 1, nPlus1);
        double sqrt = Math.ceil(Math.sqrt(n));
        for (int i = 2; i < sqrt; ++i)
            if (bits.get(i)) {
                int start = max <= i * i
                        ? (i * i)                           // start from the beginning
                        : max - ((max - (i * i)) % i) + i;  // continue where we stopped
                for (int j = start; j >= 0 && j < nPlus1; j += i)
                    bits.set(j, false);
            }
        max = n;
        log.trace("exiting critical section for {}", n);
    }

    @Override
    public Stream<Integer> primesUntil(final int number) throws IllegalArgumentException {
        Preconditions.checkArgument(number >= 0,
                "The number must be zero or positive");
        Preconditions.checkArgument(number <= Integer.MAX_VALUE - 1,
                "The number must be less than " + Integer.MAX_VALUE);

        return IntStream.range(0, number / batchSize + 1)
                .boxed()
                .flatMap(i -> {
                            int from = i * batchSize;
                            int to = (i + 1) * batchSize;

                            // fetching the volatile max ensures fetching of bits
                            if (to > max) sieve(to);
                            return IntStream.range(from, to)
                                    .filter(bits::get)
                                    .boxed();
                        }
                )
                .takeWhile(p -> p <= number);
    }
}