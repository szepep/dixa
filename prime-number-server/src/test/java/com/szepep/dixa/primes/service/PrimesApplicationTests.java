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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static com.szepep.dixa.primes.service.Utils.nextFreePort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(
        properties = {
                "grpc.port=${port}"
        }
)
@Slf4j
class PrimesApplicationTests {

    @BeforeAll
    public static void beforeAll() {
        var port = nextFreePort(20_000, 30_000);
        System.setProperty("port", Integer.toString(port));
    }

    @AfterAll
    public static void afterAll() {
        System.clearProperty("port");
    }

    @Autowired
    private GrpcService.GrpcConfig config;

    @Test
    void happyPathTest() throws Exception {
        try (var s = new Stub(config.getPort())) {
            var primes = s.stub.get(Request.newBuilder().setN(10).build())
                    .map(Response::getPrime)
                    .collectList()
                    .block();

            assertEquals(Lists.newArrayList(2, 3, 5, 7), primes);
        }
    }

    @Test
    void negativeInput() throws Exception {
        try (var s = new Stub(config.getPort())) {
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
}
