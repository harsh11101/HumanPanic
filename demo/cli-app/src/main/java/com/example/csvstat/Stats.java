package com.example.csvstat;

import java.util.List;
import java.util.Locale;

/** Summary statistics for a list of amounts. */
public record Stats(int count, double sum, double min, double max) {

    public static Stats of(List<Double> amounts) {
        double sum = 0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double a : amounts) {
            sum += a;
            min = Math.min(min, a);
            max = Math.max(max, a);
        }
        return new Stats(amounts.size(), sum, amounts.isEmpty() ? 0 : min, amounts.isEmpty() ? 0 : max);
    }

    public double mean() {
        return count == 0 ? 0 : sum / count;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "count=%d sum=%.2f mean=%.2f min=%.2f max=%.2f",
                count, sum, mean(), min, max);
    }
}
