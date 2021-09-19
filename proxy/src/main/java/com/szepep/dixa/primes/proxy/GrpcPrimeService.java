package com.szepep.dixa.primes.proxy;

import com.google.common.collect.Sets;
import com.szepep.dixa.proto.ReactorServiceGrpc;
import com.szepep.dixa.proto.Request;
import com.szepep.dixa.proto.Response;
import io.grpc.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static io.grpc.Status.Code.*;

@Service
@Slf4j
public class GrpcPrimeService implements PrimeService {

    private static final Set<Status.Code> DO_NOT_RETRY = Sets.immutableEnumSet(
            INVALID_ARGUMENT,
            NOT_FOUND,
            PERMISSION_DENIED
    );

    private final ManagedChannel channel;
    private final ReactorServiceGrpc.ReactorServiceStub stub;

    public GrpcPrimeService(GrpcConfig config) {
        channel = ManagedChannelBuilder
                .forAddress(config.getHost(), config.getPort())
                .usePlaintext()
                .build();

        stub = ReactorServiceGrpc.newReactorStub(channel);
        log.info("gRPC client started {}:{}", config.getHost(), config.getPort());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
                log.info("gRPC client closed");
            } catch (InterruptedException e) {
                log.error("Interrupted", e);
            }
        }));
    }

    @Override
    public Flux<Long> prime(Long number) {
        return stub.get(Request.newBuilder().setN(number).build())
                .map(Response::getPrime)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(5)).filter(this::retry));
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

    @Configuration
    @ConfigurationProperties(prefix = "grpc")
    @Data
    static class GrpcConfig {
        private String host = "localhost";
        private Integer port = 8080;
    }

}
