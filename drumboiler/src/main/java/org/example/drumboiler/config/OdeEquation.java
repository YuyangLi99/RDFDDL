package org.example.drumboiler.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Mapping between a variable and its derivative expression T' = f(...).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OdeEquation {
    private String variable;
    private String expression;

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }
}
