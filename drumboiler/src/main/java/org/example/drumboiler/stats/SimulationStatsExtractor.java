package org.example.drumboiler.stats;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Reads a CSV exported from simulation and computes basic statistics per variable.
 */
public class SimulationStatsExtractor {

    /**
     * Extract statistics for the requested variables.
     *
     * @param csvPath   path to exportedVariables.csv
     * @param variables variables to track (column names)
     * @return map variable -> SimulationStats
     */
    public Map<String, SimulationStats> extract(Path csvPath, List<String> variables) throws IOException {
        List<String> lines = Files.readAllLines(csvPath);
        if (lines.isEmpty()) {
            throw new IOException("CSV is empty: " + csvPath);
        }
        String[] headers = lines.get(0).split(",");
        Map<String, Integer> colIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String h = strip(headers[i]);
            colIndex.put(h, i);
            colIndex.put(h.replace("_", ""), i); // allow Ts/Ps lookups for T_S/p_S
        }
        Map<String, List<Double>> series = new HashMap<>();
        for (String v : variables) {
            series.put(v, new ArrayList<>());
        }

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(",");
            for (String v : variables) {
                Integer idx = colIndex.get(v);
                if (idx == null) {
                    idx = colIndex.get(v.replace("_", ""));
                }
                if (idx == null || idx >= parts.length) continue;
                try {
                    double val = Double.parseDouble(strip(parts[idx]));
                    series.get(v).add(val);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        Map<String, SimulationStats> out = new LinkedHashMap<>();
        for (String v : variables) {
            List<Double> values = series.getOrDefault(v, List.of());
            out.put(v, compute(values));
        }
        return out;
    }

    private SimulationStats compute(List<Double> values) {
        SimulationStats stats = new SimulationStats();
        if (values.isEmpty()) {
            stats.setMin(Double.NaN);
            stats.setMax(Double.NaN);
            stats.setMean(Double.NaN);
            stats.setP33(Double.NaN);
            stats.setP66(Double.NaN);
            stats.setCount(0);
            return stats;
        }
        double min = values.stream().min(Double::compareTo).orElse(Double.NaN);
        double max = values.stream().max(Double::compareTo).orElse(Double.NaN);
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        List<Double> sorted = values.stream().sorted().collect(Collectors.toList());
        stats.setMin(min);
        stats.setMax(max);
        stats.setMean(sum / values.size());
        stats.setP33(quantile(sorted, 0.33));
        stats.setP66(quantile(sorted, 0.66));
        stats.setCount(values.size());
        return stats;
    }

    private double quantile(List<Double> sorted, double q) {
        if (sorted.isEmpty()) return Double.NaN;
        double pos = q * (sorted.size() - 1);
        int idx = (int) pos;
        if (idx >= sorted.size() - 1) return sorted.get(sorted.size() - 1);
        double frac = pos - idx;
        return sorted.get(idx) * (1 - frac) + sorted.get(idx + 1) * frac;
    }

    private String strip(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.startsWith("\"") && t.endsWith("\"") && t.length() >= 2) {
            t = t.substring(1, t.length() - 1);
        }
        return t;
    }
}
