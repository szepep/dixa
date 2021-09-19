# Dixa Backend Engineer test

### Requirements

- Java 11
- gradle
- docker
- docker-compose

### Project structure

```
.
├── proto - definitin of proto and gRPC
├── proxy-service - REST proxy 
├── prime-number-server - gRPC prime number generator
├── docker-compose.yaml
```

### Build

Run `./gradlew build` in the root repository.

### Run

To package and deploy the application you need to run

- `./gradlew bootBuildImage`
- `docker-compose up`

You can access the proxy e.g. on http://localhost:8080/prime/10.

## Implementation choices

Currently I'm most familiar with Kotlin but unfortunately using Kotlin is not supported. Returning back to Java I can
finally appreciate the compactness and easy to use language structure in Kotlin. It is a pain to write code in Java...

As Java is the language I'm most familiar with it is the obvious choice to most production ready implementation. Spring
Boot is the most widely used web framework.

For implementing a proxy WebFlux (or other reactive framework) is the most suitable to increase the throughput. It also
provides a nice functional approach to handle requests. (Personally I like the mixture of WebFlux with Kotlin
coroutines)

A reactive application cannot work without a reactive backend. Luckily there is a reactive implementation of
gRPC https://github.com/salesforce/reactive-grpc.
(I know, you are already tired from Kotlin, but there is native coroutine support in gRPC :) ).

Unfortunately Java (nor Kotlin, shame...) have no nice implementation of immutable collection, especially lazy immutable
collections, like Scala have, e.g. the `LazyList`. It is a joy to see implementations like
https://alvinalexander.com/text/prime-number-algorithm-scala-scala-stream-class/.

To mimic similar laziness and immutability I had to do a pretty complex logic in
[LazyGenerator](prime-number-server/src/main/java/com/szepep/dixa/primes/service/LazyGenerator.java)
backed by `ConcurrentHashMap` to benefit from atomic `computeIfAbsent` method. The picked algorithm is computationally
more expensive than the
[Sieve of Eratosthenes](https://en.wikipedia.org/wiki/Sieve_of_Eratosthenes) but requires less memory.

### Missing from the implementation:

- Swagger documentation
- Actuator endpoints - at least `/info` and `/health`
- Security?
- `correlationId` to connect the events in the systems. It is easy to implement but a pain to use in plain WebFlux as it
  have limited support for MDC. (... and back to Kotlin... there is an easy to use MDC support for coroutines)

### Next steps

As a next step I will implement the assignment in Scala without proper test coverage, DI, configurability.