package com.szepep.dixa.primes.proxy;

import com.google.common.collect.Sets;
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
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Set;

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
    public Flux<Long> prime(Long number) {
        return stub.get(Request.newBuilder().setN(number).build())
                .map(Response::getPrime)
                .retryWhen(Retry
                        .backoff(config.getMaxRetry(), Duration.ofMillis(config.getRetryTimeoutMills()))
                        .filter(this::retry)
                );
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
