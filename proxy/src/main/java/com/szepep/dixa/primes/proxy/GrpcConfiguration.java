package com.szepep.dixa.primes.proxy;

import com.szepep.dixa.proto.ReactorServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration(proxyBeanMethods = false)
@Slf4j
public class GrpcConfiguration {


    @Bean
    public ReactorServiceGrpc.ReactorServiceStub reactorServiceStub(GrpcConfig config) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(config.getHost(), config.getPort())
                .usePlaintext()
                .build();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
                log.info("gRPC client closed");
            } catch (InterruptedException e) {
                log.error("Interrupted", e);
            }
        }));

        ReactorServiceGrpc.ReactorServiceStub stub = ReactorServiceGrpc.newReactorStub(channel);
        log.info("gRPC client started {}:{}", config.getHost(), config.getPort());
        return stub;
    }


    @Configuration
    @ConfigurationProperties(prefix = "grpc")
    @Data
    static class GrpcConfig {
        private String host = "localhost";
        private Integer port = 8080;
        private Integer retryTimeoutMills = 5000;
        private Integer maxRetry = 3;
    }

}
