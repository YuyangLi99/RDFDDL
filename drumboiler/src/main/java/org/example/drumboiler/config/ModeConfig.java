package org.example.drumboiler.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a mode-specific ODE and evolution/domain conditions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModeConfig {
    private String id;
    private String label;
    private String evolutionDomain;
    private String startCondition;
    private String endCondition;
    private List<OdeEquation> equations = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getEvolutionDomain() {
        return evolutionDomain;
    }

    public void setEvolutionDomain(String evolutionDomain) {
        this.evolutionDomain = evolutionDomain;
    }

    public String getStartCondition() {
        return startCondition;
    }

    public void setStartCondition(String startCondition) {
        this.startCondition = startCondition;
    }

    public String getEndCondition() {
        return endCondition;
    }

    public void setEndCondition(String endCondition) {
        this.endCondition = endCondition;
    }

    public List<OdeEquation> getEquations() {
        return equations;
    }

    public void setEquations(List<OdeEquation> equations) {
        this.equations = equations;
    }
}
