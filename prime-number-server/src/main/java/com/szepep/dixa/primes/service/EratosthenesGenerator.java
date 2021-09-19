package com.szepep.dixa.primes.service;

import java.util.BitSet;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class EratosthenesGenerator implements Generator {

    private final BitSet bits = new BitSet();
    private volatile int max = 2;

    EratosthenesGenerator() {
        bits.clear();
        bits.set(2);
    }

    synchronized void sieve(int n) {
        if (n <= max) return;

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
    public Stream<Long> primesUntil(long number) throws IllegalArgumentException {
        int n = Math.toIntExact(number);
        if (n > max) {
            IntStream.range(0, n / 100 + 1)
                    .forEach(i -> sieve((i + 1) * 100));
        }

        return IntStream.rangeClosed(0, n)
                .filter(bits::get)
                .mapToObj(p -> (long) p);
    }
}