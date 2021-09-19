package com.szepep.dixa.primes.proxy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest
class PrimeControllerTest {

    @MockBean
    private PrimeService primeService;

    @Autowired
    private WebTestClient client;

    @Test
    public void testOutput() {
        when(primeService.prime(any())).thenReturn(Flux.just(2L, 3L, 5L, 7L, 11L));

        client.get()
                .uri("/prime/12")
                .exchange()
                .expectStatus().isOk()
                .expectHeader()
                .value("content-type", ct -> assertEquals("application/stream+json;charset=UTF-8", ct))
                .expectBody(String.class)
                .consumeWith(body -> assertEquals("2,3,5,7,11", body.getResponseBody()));

        verify(primeService).prime(eq(12L));
    }

    @Test
    public void testSinglePrime() {
        when(primeService.prime(any())).thenReturn(Flux.just(2L));

        client.get()
                .uri("/prime/2")
                .exchange()
                .expectStatus().isOk()
                .expectHeader()
                .value("content-type", ct -> assertEquals("application/stream+json;charset=UTF-8", ct))
                .expectBody(String.class)
                .consumeWith(body -> assertEquals("2", body.getResponseBody()));

        verify(primeService).prime(eq(2L));
    }
}