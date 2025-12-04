package org.example.drumboiler.stats;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Fits a simple linear ODE to simulated DrumBoiler trajectories.
 * This is a linear approximation of the nonlinear DAE, intended only for the verification case study.
 */
public class DrumBoilerOdeFitter {

    /**
     * Fit linear ODE coefficients from a CSV trace.
     *
     * @param csvPath path to exportedVariables.csv with header time,T_S,p_S,V_l,qm_S (or aliases Ts/Ps)
     */
    public static OdeFitResult fitOdeFromCsv(Path csvPath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            String header = reader.readLine();
            if (header == null) throw new IOException("Empty CSV: " + csvPath);
            String[] cols = header.split(",");
            int timeIdx = find(cols, "time");
            int tIdx = find(cols, "T_S");
            if (tIdx == -1) tIdx = find(cols, "Ts");
            int pIdx = find(cols, "p_S");
            if (pIdx == -1) pIdx = find(cols, "Ps");
            int vIdx = find(cols, "V_l");
            int qmIdx = find(cols, "qm_S");
            if (timeIdx < 0 || tIdx < 0 || pIdx < 0 || vIdx < 0 || qmIdx < 0) {
                throw new IOException("Missing required columns in CSV header: time,(T_S|Ts),(p_S|Ps),V_l,qm_S");
            }

            List<Double> time = new ArrayList<>();
            List<Double> tList = new ArrayList<>();
            List<Double> pList = new ArrayList<>();
            List<Double> vList = new ArrayList<>();
            List<Double> qmList = new ArrayList<>();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",");
                if (parts.length <= Math.max(Math.max(timeIdx, tIdx), Math.max(pIdx, Math.max(vIdx, qmIdx)))) continue;
                try {
                    time.add(parse(parts[timeIdx]));
                    tList.add(parse(parts[tIdx]));
                    pList.add(parse(parts[pIdx]));
                    vList.add(parse(parts[vIdx]));
                    qmList.add(parse(parts[qmIdx]));
                } catch (NumberFormatException ignored) {
                }
            }

            List<double[]> rowsT = new ArrayList<>();
            List<double[]> rowsP = new ArrayList<>();
            List<double[]> rowsV = new ArrayList<>();
            List<Double> yT = new ArrayList<>();
            List<Double> yP = new ArrayList<>();
            List<Double> yV = new ArrayList<>();

            for (int i = 0; i < time.size() - 1; i++) {
                double dt = time.get(i + 1) - time.get(i);
                if (dt <= 0) continue;
                double dT = (tList.get(i + 1) - tList.get(i)) / dt;
                double dP = (pList.get(i + 1) - pList.get(i)) / dt;
                double dV = (vList.get(i + 1) - vList.get(i)) / dt;
                rowsT.add(new double[]{1.0, tList.get(i), pList.get(i)});
                rowsP.add(new double[]{1.0, tList.get(i), pList.get(i)});
                rowsV.add(new double[]{1.0, vList.get(i), qmList.get(i)});
                yT.add(dT);
                yP.add(dP);
                yV.add(dV);
            }

            double[] betaT = ols3(rowsT, yT); // [aT, bT, cT]
            double[] betaP = ols3(rowsP, yP); // [aP, bP, cP]
            double[] betaV = ols3(rowsV, yV); // [aV, bV, cV]

            return new OdeFitResult(betaT[0], betaT[1], betaT[2],
                    betaP[0], betaP[1], betaP[2],
                    betaV[0], betaV[1], betaV[2]);
        }
    }

    private static int find(String[] cols, String name) {
        for (int i = 0; i < cols.length; i++) {
            if (strip(cols[i]).equals(name)) return i;
        }
        return -1;
    }

    private static double parse(String s) {
        return Double.parseDouble(strip(s));
    }

    private static String strip(String s) {
        String t = s.trim();
        if (t.startsWith("\"") && t.endsWith("\"") && t.length() >= 2) {
            t = t.substring(1, t.length() - 1);
        }
        return t;
    }

    /**
     * Ordinary least squares for 3 parameters using normal equations.
     */
    private static double[] ols3(List<double[]> Xrows, List<Double> y) {
        if (Xrows.isEmpty() || y.isEmpty()) {
            return new double[]{0, 0, 0};
        }
        double[][] xtx = new double[3][3];
        double[] xty = new double[3];
        for (int i = 0; i < Xrows.size(); i++) {
            double[] x = Xrows.get(i);
            double yi = y.get(i);
            for (int r = 0; r < 3; r++) {
                xty[r] += x[r] * yi;
                for (int c = 0; c < 3; c++) {
                    xtx[r][c] += x[r] * x[c];
                }
            }
        }
        double[][] inv = invert3x3(xtx);
        return new double[]{
                inv[0][0] * xty[0] + inv[0][1] * xty[1] + inv[0][2] * xty[2],
                inv[1][0] * xty[0] + inv[1][1] * xty[1] + inv[1][2] * xty[2],
                inv[2][0] * xty[0] + inv[2][1] * xty[1] + inv[2][2] * xty[2]
        };
    }

    private static double[][] invert3x3(double[][] m) {
        double a = m[0][0], b = m[0][1], c = m[0][2];
        double d = m[1][0], e = m[1][1], f = m[1][2];
        double g = m[2][0], h = m[2][1], k = m[2][2];
        double A =   e*k - f*h;
        double B = -(d*k - f*g);
        double C =   d*h - e*g;
        double D = -(b*k - c*h);
        double E =   a*k - c*g;
        double F = -(a*h - b*g);
        double G =   b*f - c*e;
        double H = -(a*f - c*d);
        double K =   a*e - b*d;
        double det = a*A + b*B + c*C;
        if (Math.abs(det) < 1e-12) {
            return new double[][]{{0,0,0},{0,0,0},{0,0,0}};
        }
        double invDet = 1.0 / det;
        return new double[][]{
                {A*invDet, D*invDet, G*invDet},
                {B*invDet, E*invDet, H*invDet},
                {C*invDet, F*invDet, K*invDet}
        };
    }
}
