package com.szepep.dixa.primes.proxy;

import reactor.core.publisher.Flux;

public interface PrimeService {

    Flux<Long> prime(Long number);
}
