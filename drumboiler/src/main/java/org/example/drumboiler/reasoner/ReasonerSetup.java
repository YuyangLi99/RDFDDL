package org.example.drumboiler.reasoner;

import org.apache.jena.query.Dataset;
import org.apache.jena.reasoner.rulesys.BuiltinRegistry;

/**
 * Helper to register custom builtins used in drumboiler reasoning.
 * Call this before running Jena rules so that mode_change(...) is recognized.
 */
public final class ReasonerSetup {
    private ReasonerSetup() {}

    public static void registerBuiltins(Dataset dataset) {
        BuiltinRegistry.theRegistry.register(new ModeChange(dataset));
        BuiltinRegistry.theRegistry.register(new IsNextBuiltin(dataset));
    }
}
