package com.szepep.dixa.primes.service;

import java.util.stream.Stream;

public interface Generator {
    Stream<Long> primesUntil(long number);
}
