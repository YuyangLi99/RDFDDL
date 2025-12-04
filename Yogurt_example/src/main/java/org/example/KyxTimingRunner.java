package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

/**
 * Quick harness to time a single dL obligation against KeYmaera X for the yogurt example.
 * Pass a .kyx path as the first argument to time a real obligation; otherwise an inline placeholder is used.
 */
public final class KyxTimingRunner {
    private KyxTimingRunner() {
    }

    public static void main(String[] args) throws IOException {
        Path argPath = args.length > 0 ? Path.of(args[0]) : null;
        String obligation = argPath != null ? Files.readString(argPath) : defaultObligation();

        Instant start = Instant.now();
        String result = Prove_helper.prove(obligation);
        Duration elapsed = Duration.between(start, Instant.now());

        System.out.printf("KeYmaera X result: %s (%.3f s)%n", result, elapsed.toMillis() / 1000.0);
    }

    private static String defaultObligation() {
        return """
               ArchiveEntry "timing-yogurt"
               ProgramVariables
                 Real x, p;
               End.
               Problem
               (x>=0 & p>=0) -> (x>=0 & p>=0)
               End.
               End.
               """;
    }
}
