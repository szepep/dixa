package com.szepep.dixa.primes.proxy.monitoring;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Correlation ID generator based on UUID.
 */
@Service
public class UIIDCorrelationId implements CorrelationId {

    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }

}
