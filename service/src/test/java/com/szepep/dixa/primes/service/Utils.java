package com.szepep.dixa.primes.service;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ThreadLocalRandom;

final class Utils {
    private Utils() {
    }

    static int nextFreePort(int from, int to) {
        int port;
        //noinspection StatementWithEmptyBody
        while (!isLocalPortFree(port = random(from, to))) {/* noop */}
        return port;
    }

    private static int random(int from, int to) {
        return ThreadLocalRandom.current().nextInt(from, to);
    }

    private static boolean isLocalPortFree(int port) {
        try {
            new ServerSocket(port).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
