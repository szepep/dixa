package com.szepep.dixa.primes.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
@AllArgsConstructor
public class PrimesApplication implements CommandLineRunner {

    public static void main(String... args) {
        SpringApplication.run(PrimesApplication.class, args);
    }

    private final GrpcService grpcService;

    @Override
    public void run(String... args) throws Exception {
        grpcService.start();
        startBackgroundAwaitThread();
    }

    private void startBackgroundAwaitThread() {
        Thread awaitThread = new Thread(() -> {
            try {
                grpcService.block();
            } catch (InterruptedException e) {
                log.error("gRPC server stopped.", e);
            }
        });
        awaitThread.setName("gRPC-await-termination");
        awaitThread.setDaemon(false);
        awaitThread.start();
    }
}