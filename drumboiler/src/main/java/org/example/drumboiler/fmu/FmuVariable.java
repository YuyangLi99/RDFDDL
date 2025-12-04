package org.example.drumboiler.fmu;

/**
 * Minimal projection of a Modelica ScalarVariable entry from modelDescription.xml.
 */
public class FmuVariable {
    private final String name;
    private final String valueReference;
    private final String type;
    private final String unit;
    private final String causality;
    private final String variability;

    public FmuVariable(String name,
                       String valueReference,
                       String type,
                       String unit,
                       String causality,
                       String variability) {
        this.name = name;
        this.valueReference = valueReference;
        this.type = type;
        this.unit = unit;
        this.causality = causality;
        this.variability = variability;
    }

    public String getName() {
        return name;
    }

    public String getValueReference() {
        return valueReference;
    }

    public String getType() {
        return type;
    }

    public String getUnit() {
        return unit;
    }

    public String getCausality() {
        return causality;
    }

    public String getVariability() {
        return variability;
    }

    @Override
    public String toString() {
        return "FmuVariable{" +
                "name='" + name + '\'' +
                ", valueReference='" + valueReference + '\'' +
                ", type='" + type + '\'' +
                ", unit='" + unit + '\'' +
                ", causality='" + causality + '\'' +
                ", variability='" + variability + '\'' +
                '}';
    }
}
