package com.szepep.dixa.primes.proxy;

import com.szepep.dixa.primes.proxy.service.GrpcPrimeService;
import com.szepep.dixa.proto.ReactorServiceGrpc;
import com.szepep.dixa.proto.Response;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GrpcPrimeServiceTest {


    private final ReactorServiceGrpc.ServiceImplBase mockService = mock(ReactorServiceGrpc.ServiceImplBase.class);
    private GrpcPrimeService grpcPrimeService;

    private Server server;
    private ManagedChannel channel;

    static Flux<Response> primes(int... primes) {
        var stream = Arrays.stream(primes).mapToObj(p -> Response.newBuilder().setPrime(p).build());
        return Flux.fromStream(stream);
    }

    @BeforeEach
    public void setUp() throws IOException {
        String serverName = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder
                .forName(serverName)
                .directExecutor()
                .addService(mockService)
                .build()
                .start();

        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        var stub = ReactorServiceGrpc.newReactorStub(channel);

        var config = new GrpcConfiguration.GrpcConfig();
        config.setRetryTimeoutMills(10);
        config.setMaxRetry(5);

        grpcPrimeService = new GrpcPrimeService(stub, config);
    }

    @AfterEach
    public void tearDown() {
        server.shutdown();
        channel.shutdown();
    }

    @Test
    public void testHappyPath() {
        when(mockService.get(any())).thenReturn(primes(2, 3, 5, 7, 11));

        var result = grpcPrimeService.prime(11)
                .collectList()
                .block();

        assertEquals(Lists.newArrayList(2, 3, 5, 7, 11), result);
    }

    @Test
    public void retry() {
        //noinspection unchecked
        when(mockService.get(any())).thenReturn(
                Flux.error(new IllegalStateException()),
                Flux.error(new IllegalStateException()),
                primes(2, 3, 5, 7, 11)
        );

        var result = grpcPrimeService.prime(11)
                .collectList()
                .block();

        assertEquals(Lists.newArrayList(2, 3, 5, 7, 11), result);
    }

    @Test
    public void retryFailsDueToExceedingLimit() {
        //noinspection unchecked
        when(mockService.get(any())).thenReturn(
                Flux.error(new IllegalStateException()),
                Flux.error(new IllegalStateException()),
                Flux.error(new IllegalStateException()),
                Flux.error(new IllegalStateException()),
                Flux.error(new IllegalStateException()),
                Flux.error(new IllegalStateException()),
                primes(2, 3, 5, 7, 11)
        );

        assertThrows(IllegalStateException.class, () ->
                grpcPrimeService.prime(11)
                        .collectList()
                        .block()
        );
    }

    @Test
    public void retryFailsDueToIllegalArgument() {
        //noinspection unchecked
        when(mockService.get(any())).thenReturn(
                Flux.error(new StatusRuntimeException(Status.INVALID_ARGUMENT)),
                primes(2, 3, 5, 7, 11)
        );

        assertThrows(StatusRuntimeException.class, () ->
                grpcPrimeService.prime(11)
                        .collectList()
                        .block()
        );
    }

    @Test
    public void unknownIsRetried() {
        //noinspection unchecked
        when(mockService.get(any())).thenReturn(
                Flux.error(new StatusRuntimeException(Status.UNKNOWN)),
                primes(2, 3, 5, 7, 11)
        );

        var result = grpcPrimeService.prime(11)
                .collectList()
                .block();

        assertEquals(Lists.newArrayList(2, 3, 5, 7, 11), result);
    }

}