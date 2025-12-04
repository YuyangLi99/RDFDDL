package org.example.drumboiler.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration entry describing a SHACL NodeShape representing a boiler state.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StateShapeConfig {
    private String id;
    private String state;
    private String label;
    private List<VariableConstraint> constraints = new ArrayList<>();
    private String mode;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<VariableConstraint> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<VariableConstraint> constraints) {
        this.constraints = constraints;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
