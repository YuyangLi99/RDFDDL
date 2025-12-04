package org.example.drumboiler.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Directed candidate transition between two discretized states.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransitionConfig {
    private String from;
    private String to;
    private String type; // isNext (continuous) or ModeChange (discrete)

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
