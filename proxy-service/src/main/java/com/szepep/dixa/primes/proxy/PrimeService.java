package com.szepep.dixa.primes.proxy;

import reactor.core.publisher.Flux;

/**
 * Service for generating prime numbers
 */
public interface PrimeService {

    /**
     * Returns a flux of prime numbers
     */
    Flux<Integer> prime(int number);
}
