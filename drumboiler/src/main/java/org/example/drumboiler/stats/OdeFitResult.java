package org.example.drumboiler.stats;

/**
 * Linear ODE fit coefficients for the simplified DrumBoiler model.
 * This is a linear approximation to the nonlinear DAE; intended for verification examples.
 */
public final class OdeFitResult {
    public final double aT, bT, cT;
    public final double aP, bP, cP;
    public final double aV, bV, cV;

    public OdeFitResult(double aT, double bT, double cT,
                        double aP, double bP, double cP,
                        double aV, double bV, double cV) {
        this.aT = aT;
        this.bT = bT;
        this.cT = cT;
        this.aP = aP;
        this.bP = bP;
        this.cP = cP;
        this.aV = aV;
        this.bV = bV;
        this.cV = cV;
    }

    @Override
    public String toString() {
        return "OdeFitResult{" +
                "aT=" + aT +
                ", bT=" + bT +
                ", cT=" + cT +
                ", aP=" + aP +
                ", bP=" + bP +
                ", cP=" + cP +
                ", aV=" + aV +
                ", bV=" + bV +
                ", cV=" + cV +
                '}';
    }
}
