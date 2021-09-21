package com.szepep.dixa.primes.service;

import com.szepep.dixa.proto.ReactorServiceGrpc;
import com.szepep.dixa.proto.Request;
import com.szepep.dixa.proto.Response;
import io.grpc.Status;
import io.grpc.StatusException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@AllArgsConstructor
public class PrimeServiceImpl extends ReactorServiceGrpc.ServiceImplBase {

    private final Generator generator;

    @Override
    public Flux<Response> get(Mono<Request> request) {
        AtomicReference<String> cid = new AtomicReference<>();
        return request
                .doOnEach(s -> Optional.ofNullable(s.get()).ifPresent(r -> {
                            cid.set(r.getCorrelationId());
                            log.info("[{}] Request received", r.getCorrelationId());
                        })
                )
                .map(Request::getNumber)
                .map(generator::primesUntil)
                .flatMapMany(Flux::fromStream)
                .map(p -> Response.newBuilder().setPrime(p).build())
                .onErrorMap(e -> {
                    Status status = Status.INTERNAL;
                    if (e instanceof IllegalArgumentException) status = Status.INVALID_ARGUMENT;
                    return new StatusException(status.withDescription(e.getMessage()).withCause(e));
                })
                .doOnComplete(() -> log.info("[{}] Request processed", cid.get()));
    }
}
