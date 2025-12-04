package org.example.drumboiler.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Numeric constraint on a physical variable, mapped from the discretization config.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VariableConstraint {
    private String variable;
    private Double hasValue;
    private Double minInclusive;
    private Double maxInclusive;
    private Double minExclusive;
    private Double maxExclusive;

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public Double getHasValue() {
        return hasValue;
    }

    public void setHasValue(Double hasValue) {
        this.hasValue = hasValue;
    }

    public Double getMinInclusive() {
        return minInclusive;
    }

    public void setMinInclusive(Double minInclusive) {
        this.minInclusive = minInclusive;
    }

    public Double getMaxInclusive() {
        return maxInclusive;
    }

    public void setMaxInclusive(Double maxInclusive) {
        this.maxInclusive = maxInclusive;
    }

    public Double getMinExclusive() {
        return minExclusive;
    }

    public void setMinExclusive(Double minExclusive) {
        this.minExclusive = minExclusive;
    }

    public Double getMaxExclusive() {
        return maxExclusive;
    }

    public void setMaxExclusive(Double maxExclusive) {
        this.maxExclusive = maxExclusive;
    }
}
