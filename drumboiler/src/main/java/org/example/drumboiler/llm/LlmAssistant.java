package org.example.drumboiler.llm;

import org.apache.jena.rdf.model.Model;
import org.example.drumboiler.fmu.FmuVariable;

import java.util.List;

/**
 * Abstraction for LLM-assisted configuration and documentation generation.
 * Concrete implementations may call OpenAI/Gemini/HF models, but callers should always
 * validate outputs (JSON schema, SHACL, dL checks) before using them.
 */
public interface LlmAssistant {

    /**
     * Suggest a config JSON (as string) that follows the drumboiler config schema.
     * The implementation may use process descriptions plus FMU variables as hints.
     */
    String suggestConfig(List<FmuVariable> variables, String processDescription);

    /**
     * Produce a human-readable Markdown summary of state shapes from an RDF/SHACL model.
     */
    String summarizeShapes(Model shapesModel);
}
