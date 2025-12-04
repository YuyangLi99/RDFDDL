package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

/**
 * Times a single KeYmaera X proof run for the tank example.
 * Provide a .kyx file path as the first argument to measure that obligation; otherwise a minimal inline one is used.
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
               ArchiveEntry "timing-tank"
               ProgramVariables
                 Real h1, h2;
               End.
               Problem
               (h1>=0 & h2>=0) -> (h1>=0 & h2>=0)
               End.
               End.
               """;
    }
}
