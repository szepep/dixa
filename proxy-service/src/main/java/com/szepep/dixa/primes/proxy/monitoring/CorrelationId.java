package com.szepep.dixa.primes.proxy.monitoring;

public interface CorrelationId {

    String CORRELATION_ID_HEADER = "X-Correlation-ID";
    String CORRELATION_KEY = "correlationID";

    String generate();
}
