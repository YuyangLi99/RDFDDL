package org.example.drumboiler.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Root configuration document containing discretization information and ODEs.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BoilerConfig {
    private List<String> trackedVariables = new ArrayList<>();
    private List<StateShapeConfig> stateShapes = new ArrayList<>();
    private List<TransitionConfig> transitions = new ArrayList<>();
    private OdeConfig ode;
    private List<ModeConfig> modes = new ArrayList<>();

    public List<String> getTrackedVariables() {
        return trackedVariables;
    }

    public void setTrackedVariables(List<String> trackedVariables) {
        this.trackedVariables = trackedVariables;
    }

    public List<StateShapeConfig> getStateShapes() {
        return stateShapes;
    }

    public void setStateShapes(List<StateShapeConfig> stateShapes) {
        this.stateShapes = stateShapes;
    }

    public List<TransitionConfig> getTransitions() {
        return transitions;
    }

    public void setTransitions(List<TransitionConfig> transitions) {
        this.transitions = transitions;
    }

    public OdeConfig getOde() {
        return ode;
    }

    public void setOde(OdeConfig ode) {
        this.ode = ode;
    }

    public List<ModeConfig> getModes() {
        return modes;
    }

    public void setModes(List<ModeConfig> modes) {
        this.modes = modes;
    }
}
