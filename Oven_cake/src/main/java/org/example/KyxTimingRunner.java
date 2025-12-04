package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

/**
 * Runs a single dL obligation through KeYmaera X and prints how long it took.
 * Pass a .kyx file path as the first argument to time a real obligation; otherwise an inline toy one is used.
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
               ArchiveEntry "timing-oven"
               ProgramVariables
                 Real temp;
               End.
               Problem
               (temp>=50 & temp<=65) -> (temp>=50)
               End.
               End.
               """;
    }
}
