package com.szepep.dixa.primes.proxy.service;

import com.google.common.collect.Sets;
import com.szepep.dixa.primes.proxy.GrpcConfiguration;
import com.szepep.dixa.proto.ReactorServiceGrpc;
import com.szepep.dixa.proto.Request;
import com.szepep.dixa.proto.Response;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Set;

import static com.szepep.dixa.primes.proxy.monitoring.CorrelationId.CORRELATION_KEY;
import static io.grpc.Status.Code.*;

@Service
@AllArgsConstructor
@Slf4j
public class GrpcPrimeService implements PrimeService {

    private static final Set<Status.Code> DO_NOT_RETRY = Sets.immutableEnumSet(
            INVALID_ARGUMENT,
            NOT_FOUND,
            PERMISSION_DENIED
    );

    private final ReactorServiceGrpc.ReactorServiceStub stub;
    private final GrpcConfiguration.GrpcConfig config;

    @Override
    public Flux<Integer> prime(final int number) {
        return sendRequest(number)
                .map(Response::getPrime)
                .retryWhen(Retry
                        .backoff(config.getMaxRetry(), Duration.ofMillis(config.getRetryTimeoutMills()))
                        .filter(this::retry)
                );
    }

    private Flux<Response> sendRequest(Integer n) {
        var builder = Request.newBuilder().setNumber(n);
        return Mono.deferContextual(context ->
                Mono.just(context.getOrEmpty(CORRELATION_KEY)
                        .map(cid -> builder.setCorrelationId((String) cid).build())
                        .orElse(builder.build()))
        ).flatMapMany(stub::get);
    }

    private boolean retry(Throwable throwable) {
        Status.Code code;
        if (throwable instanceof StatusException) {
            code = ((StatusException) throwable).getStatus().getCode();
        } else if (throwable instanceof StatusRuntimeException) {
            code = ((StatusRuntimeException) throwable).getStatus().getCode();
        } else {
            return true;
        }
        return !DO_NOT_RETRY.contains(code);
    }

}
