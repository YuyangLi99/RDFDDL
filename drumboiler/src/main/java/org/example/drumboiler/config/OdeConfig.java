package org.example.drumboiler.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified representation of the ODE block used when emitting dL obligations.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OdeConfig {
    private String evolutionDomain;
    private List<OdeEquation> equations = new ArrayList<>();

    public String getEvolutionDomain() {
        return evolutionDomain;
    }

    public void setEvolutionDomain(String evolutionDomain) {
        this.evolutionDomain = evolutionDomain;
    }

    public List<OdeEquation> getEquations() {
        return equations;
    }

    public void setEquations(List<OdeEquation> equations) {
        this.equations = equations;
    }
}
