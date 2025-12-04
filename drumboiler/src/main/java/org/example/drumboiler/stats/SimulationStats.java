package org.example.drumboiler.stats;

/**
 * Simple statistics for a simulated time series.
 */
public class SimulationStats {
    private double min;
    private double max;
    private double mean;
    private double p33;
    private double p66;
    private long count;

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public double getP33() {
        return p33;
    }

    public void setP33(double p33) {
        this.p33 = p33;
    }

    public double getP66() {
        return p66;
    }

    public void setP66(double p66) {
        this.p66 = p66;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
