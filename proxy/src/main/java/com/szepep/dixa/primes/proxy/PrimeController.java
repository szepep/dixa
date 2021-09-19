package com.szepep.dixa.primes.proxy;

import com.google.common.base.Preconditions;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
        Preconditions.checkArgument(number >= 0, "The number must be greater or equal to 0");
        AtomicBoolean first = new AtomicBoolean(true);
        return service
                .prime(number)
                .map(Object::toString)
                .map(prime -> first.compareAndSet(true, false)
                        ? prime
                        : "," + prime
                );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handlerIllegalArgument(IllegalArgumentException e) {
        return new ResponseEntity(e.getMessage(), null, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handlerException(Exception e) {
        return new ResponseEntity(e.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
