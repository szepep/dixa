package com.szepep.dixa.primes.proxy.monitoring;


import com.google.common.base.Stopwatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.szepep.dixa.primes.proxy.monitoring.CorrelationId.CORRELATION_ID_HEADER;
import static com.szepep.dixa.primes.proxy.monitoring.CorrelationId.CORRELATION_KEY;


@Component
@Slf4j
@RequiredArgsConstructor
class RequestFilter implements WebFilter {

    private final CorrelationId cid;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Stopwatch timer = Stopwatch.createStarted();

        String correlationId = cid.generate();
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

        log.info("[{}] request received from {}", correlationId, exchange.getRequest().getRemoteAddress());

        return chain
                .filter(exchange)
                .contextWrite(Context.of(CORRELATION_KEY, correlationId))
                .doOnEach(signal ->
                        logFinishedRequest(correlationId, exchange, timer.elapsed(TimeUnit.MILLISECONDS)));
    }

    private void logFinishedRequest(String correlationId, ServerWebExchange exchange, long elapsed) {
        int statusCode = Optional
                .ofNullable(exchange.getResponse().getStatusCode())
                .map(HttpStatus::value).orElse(200);
        log.info("[{}] finished in {}ms with status code {}", correlationId, elapsed, statusCode);
    }
}
