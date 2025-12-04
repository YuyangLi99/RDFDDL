package org.example.drumboiler.proof;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Times a single dL obligation run through KeYmaera X.
 * Give a .kyx path to time that obligation; otherwise the first build/obligations file is used when present,
 * falling back to a minimal inline placeholder.
 */
public final class KyxTimingRunner {
    private KyxTimingRunner() {
    }

    public static void main(String[] args) throws IOException {
        Path requested = args.length > 0 ? Path.of(args[0]) : null;
        Obligation obligation = pickObligation(requested);

        System.out.printf("Using obligation: %s%n", obligation.source());
        KyxProver prover = new KyxProver();

        Instant start = Instant.now();
        String result = prover.prove(obligation.content());
        Duration elapsed = Duration.between(start, Instant.now());

        System.out.printf("KeYmaera X result: %s (%.3f s)%n", result, elapsed.toMillis() / 1000.0);
    }

    private static Obligation pickObligation(Path requested) throws IOException {
        if (requested != null) {
            return new Obligation(Files.readString(requested), requested.toString());
        }
        Optional<Path> buildPath = findFirstBuildObligation();
        if (buildPath.isPresent()) {
            Path path = buildPath.get();
            return new Obligation(Files.readString(path), path.toString());
        }
        return new Obligation(defaultObligation(), "inline default");
    }

    private static Optional<Path> findFirstBuildObligation() {
        Path dir = Path.of("build", "obligations");
        if (!Files.isDirectory(dir)) {
            return Optional.empty();
        }
        try (Stream<Path> paths = Files.list(dir)
                .filter(p -> p.toString().endsWith(".kyx"))
                .sorted()) {
            return paths.findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static String defaultObligation() {
        return """
               ArchiveEntry "timing-drumboiler"
               ProgramVariables
                 Real p;
               End.
               Problem
               (p>=1) -> (p>=1)
               End.
               End.
               """;
    }

    private record Obligation(String content, String source) {
    }
}
