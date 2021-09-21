package com.szepep.dixa.primes.proxy;

import com.google.common.base.Preconditions;
import com.szepep.dixa.primes.proxy.service.PrimeService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/prime")
@AllArgsConstructor
public class PrimeController {

    private final PrimeService service;

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
    public Flux<String> primes(@PathVariable("number") int number) {
        Preconditions.checkArgument(number >= 0, "The number must be greater or equal to 0");
        AtomicBoolean first = new AtomicBoolean(true);
        return service.prime(number)
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
        String message = MessageFormat.format("{0}: {1}", e.getClass().getCanonicalName(), e.getMessage());
        return new ResponseEntity(message, null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
