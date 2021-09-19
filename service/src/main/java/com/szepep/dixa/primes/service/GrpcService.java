package com.szepep.dixa.primes.service;

import com.google.common.annotations.VisibleForTesting;
import com.szepep.dixa.proto.ReactorServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class GrpcService {

    private final ReactorServiceGrpc.ServiceImplBase service;
    private final int port;
    private Server server;


    GrpcService(
            ReactorServiceGrpc.ServiceImplBase service,
            GrpcConfig config
    ) {
        this.service = service;
        this.port = config.getPort();
    }

    public void start() throws IOException {
        log.info("Starting gRPC on port {}.", port);
        server = ServerBuilder
                .forPort(port)
                .addService(service)
                .build()
                .start();
        log.info("gRPC server started, listening on {}.", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down gRPC server.");
            GrpcService.this.stop();
            log.info("gRPC server shut down successfully.");
        }));
    }

    @VisibleForTesting
    void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    public void block() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConfigurationProperties(prefix = "grpc")
    @Data
    static class GrpcConfig {
        private int port;
    }
}