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
backed by `ConcurrentHashMap` to benefit from atomic `computeIfAbsent` method.

Other solution is much faster and requires less memory
[EratosthenesGenerator](prime-number-server/src/main/java/com/szepep/dixa/primes/service/EratosthenesGenerator.java). It
is inspired by [Sieve of Eratosthenes](https://en.wikipedia.org/wiki/Sieve_of_Eratosthenes) but the implementation is
lazy and thread safe. Each prime is only computed once.

A super complex incarnation of Eratosthenes
sieve [NonBlockingEratosthenesGenerator](prime-number-server/src/main/java/com/szepep/dixa/primes/service/NonBlockingEratosthenesGenerator.java)
tries to not use global synchronized block just lock the smallest possible computation fragment using read-write lock.
There is not much performance benefit, the average time spent in providing prime numbers in 20 parallel threads with
limit between 1,000,000 and
5,000,000: [EratosthenesGeneratorTest#performanceComparison](prime-number-server/src/test/java/com/szepep/dixa/primes/service/EratosthenesGeneratorTest.java)

```
EratosthenesGenerator: 6531ms
NonBlockingEratosthenesGenerator: 6287ms 
LazyGenerator: 36475ms
```

The clear winner is `EratosthenesGenerator`, not to complex to understand but performs well.

### Missing from the implementation:

- Swagger documentation
- Actuator endpoints - at least `/info` and `/health`
- Security?

### Next steps

As a next step I will implement the assignment in Scala without proper test coverage, DI, configurability.