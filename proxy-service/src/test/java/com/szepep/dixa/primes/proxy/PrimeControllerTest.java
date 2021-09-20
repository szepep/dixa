package com.szepep.dixa.primes.proxy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
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
        when(primeService.prime(anyInt())).thenReturn(Flux.just(2, 3, 5, 7, 11));

        client.get()
                .uri("/prime/12")
                .exchange()
                .expectStatus().isOk()
                .expectHeader()
                .value("content-type", ct -> assertEquals("application/stream+json;charset=UTF-8", ct))
                .expectBody(String.class)
                .consumeWith(body -> assertEquals("2,3,5,7,11", body.getResponseBody()));

        verify(primeService).prime(eq(12));
    }

    @Test
    public void testSinglePrime() {
        when(primeService.prime(anyInt())).thenReturn(Flux.just(2));

        client.get()
                .uri("/prime/2")
                .exchange()
                .expectStatus().isOk()
                .expectHeader()
                .value("content-type", ct -> assertEquals("application/stream+json;charset=UTF-8", ct))
                .expectBody(String.class)
                .consumeWith(body -> assertEquals("2", body.getResponseBody()));

        verify(primeService).prime(eq(2));
    }

    @Test
    public void testNegativeInput() {
        client.get()
                .uri("/prime/-10")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class)
                .consumeWith(body -> assertEquals(
                        "The number must be greater or equal to 0",
                        body.getResponseBody()));
    }
}