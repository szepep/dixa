package com.szepep.dixa.primes.service;

import com.google.common.collect.Lists;
import com.szepep.dixa.proto.ReactorServiceGrpc;
import com.szepep.dixa.proto.Request;
import com.szepep.dixa.proto.Response;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import(PrimesApplicationTests.TestConfig.class)
@Slf4j
class PrimesApplicationTests {

    @Autowired
    private GrpcService.GrpcConfig config;

    @Test
    void happyPathTest() throws Exception {
        try (var s = new Stub(config.port)) {
            var primes = s.stub.get(Request.newBuilder().setN(10).build())
                    .map(Response::getPrime)
                    .collectList()
                    .block();

            assertEquals(Lists.newArrayList(2L, 3L, 5L, 7L), primes);
        }
    }

    @Test
    void negativeInput() throws Exception {
        try (var s = new Stub(config.port)) {
            var e = assertThrows(StatusRuntimeException.class, () ->
                    s.stub.get(Request.newBuilder().setN(-10).build())
                            .map(Response::getPrime)
                            .collectList()
                            .block()
            );
            assertEquals(Status.INVALID_ARGUMENT.getCode(), e.getStatus().getCode());
        }
    }

    private static class Stub implements AutoCloseable {

        public final ReactorServiceGrpc.ReactorServiceStub stub;
        private final ManagedChannel channel;

        public Stub(Integer port) {
            channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
            stub = ReactorServiceGrpc.newReactorStub(channel);
            log.info("gRPC client started");
        }

        @Override
        public void close() throws Exception {
            channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            log.info("gRPC client shut down successfully.");
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {

        @Bean
        GrpcService.GrpcConfig config() {
            GrpcService.GrpcConfig grpcConfig = new GrpcService.GrpcConfig();
            grpcConfig.port = nextFreePort(20_000, 30_000);
            return grpcConfig;
        }

        private int nextFreePort(int from, int to) {
            int port;
            while (!isLocalPortFree(port = random(from, to))) {/* noop */}
            return port;
        }

        private int random(int from, int to) {
            return ThreadLocalRandom.current().nextInt(from, to);
        }

        private boolean isLocalPortFree(int port) {
            try {
                new ServerSocket(port).close();
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

}
