package com.szepep.dixa.primes.service;

import com.szepep.dixa.proto.ReactorServiceGrpc;
import com.szepep.dixa.proto.Request;
import com.szepep.dixa.proto.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PrimeServiceImpl extends ReactorServiceGrpc.ServiceImplBase {

    private final Generator generator = new LazyGenerator();

    @Override
    public Flux<Response> get(Mono<Request> request) {
        return request
                .map(Request::getN)
                .map(generator::primesUntil)
                .flatMapMany(Flux::fromStream)
                .map(p -> Response.newBuilder().setPrime(p).build());
    }
}
