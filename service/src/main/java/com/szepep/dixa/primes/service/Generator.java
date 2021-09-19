package com.szepep.dixa.primes.service;

import java.util.stream.Stream;

/**
 * Generator of prime numbers;
 */
public interface Generator {

    /**
     * Generates a stream of prime numbers until the number
     *
     * @param number The limit of the prime numbers in result. No result is larger than number.
     * @return Stream of prime numbers.
     * @throws IllegalArgumentException when the number is not supported, e.g. negative, too large.
     */
    Stream<Long> primesUntil(long number) throws IllegalArgumentException;
}
