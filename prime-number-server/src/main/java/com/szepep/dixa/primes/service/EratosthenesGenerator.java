package com.szepep.dixa.primes.service;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.concurrent.ThreadSafe;
import java.util.BitSet;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Generator using Eratosthenes sieve. Returns continuous results and does lazy computation.
 */
@Component
@Primary
@ThreadSafe
public final class EratosthenesGenerator implements Generator {

    private final int batchSize = 1000;

    private final BitSet bits = new BitSet();

    // volatile works as memory barrier to synchronize the bits
    // DO NOT CHANGE the order of variables, the volatile must be the last instance variable.
    private volatile int max = 2;

    EratosthenesGenerator() {
        bits.clear();
        bits.set(2);
    }

    synchronized void sieve(int n) {
        if (n <= max) return; // other thread already computed

        int nPlus1 = n + 1;

        bits.clear(max + 1, nPlus1);
        bits.flip(max + 1, nPlus1);
        double sqrt = Math.ceil(Math.sqrt(n));
        for (int i = 2; i < sqrt; ++i)
            if (bits.get(i)) {
                int start = max > i * i
                        ? max - ((max - (i * i)) % i) + i
                        : (i * i);
                for (int j = start; j >= 0 && j < nPlus1; j += i)
                    bits.set(j, false);
            }
        max = n;
    }

    @Override
    public Stream<Integer> primesUntil(final int number) throws IllegalArgumentException {
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