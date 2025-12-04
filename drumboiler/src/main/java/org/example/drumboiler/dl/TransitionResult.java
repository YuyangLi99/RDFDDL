package org.example.drumboiler.dl;

import java.nio.file.Path;

/**
 * Metadata about an emitted obligation, written to the summary file.
 */
public class TransitionResult {
    private final String from;
    private final String to;
    private final Path obligationFile;
    private final String status;

    public TransitionResult(String from, String to, Path obligationFile, String status) {
        this.from = from;
        this.to = to;
        this.obligationFile = obligationFile;
        this.status = status;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public Path getObligationFile() {
        return obligationFile;
    }

    public String getStatus() {
        return status;
    }

    public TransitionResult withStatus(String newStatus) {
        return new TransitionResult(this.from, this.to, this.obligationFile, newStatus);
    }
}
