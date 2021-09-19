package com.szepep.dixa.primes.proxy;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/prime")
public class PrimeController {

    private final PrimeService service;

    public PrimeController(PrimeService service) {
        this.service = service;
    }

    /**
     * The endpoint returns prime numbers until number.
     * <p>
     * APPLICATION_STREAM_JSON is deprecated but Chrome shows the continuous response.
     * APPLICATION_NDJSON should be used but chrome downloads the response.
     *
     * @param number The uppor limit of prime numbers
     * @return All prime numbers less than equal to number.
     */
    @SuppressWarnings("deprecation")
    @GetMapping(value = "/{number}", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public Flux<String> primes(@PathVariable("number") long number) {
        AtomicBoolean first = new AtomicBoolean(true);
        return service
                .prime(number)
                .map(Object::toString)
                .map(prime -> first.compareAndSet(true, false)
                        ? prime
                        : "," + prime
                );
    }
}
