package com.szepep.dixa.primes.service;

import com.google.common.annotations.VisibleForTesting;
import com.szepep.dixa.proto.ReactorServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class GrpcService {

    private final ReactorServiceGrpc.ServiceImplBase service;
    private final GrpcConfig config;

    private Server server;

    public void start() throws IOException {
        log.info("Starting gRPC on port {}.", config.getPort());
        server = ServerBuilder
                .forPort(config.getPort())
                .addService(service)
                .build()
                .start();
        log.info("gRPC server started, listening on {}.", config.getPort());

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