package org.group3.tutorlink.features.post.recommendation.scoring;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Trung bình có trọng số của các tiêu chí còn dữ liệu (tiêu chí thiếu thì không gọi add),
 * cùng khung với StudentApplicantScorer và WeightedScorer.
 */
public class ScoreAccumulator {

    private final Map<String, Double> breakdown = new LinkedHashMap<>();
    private double weightedSum;
    private double weightTotal;

    /** @param value điểm tiêu chí trong [0, 1]; trọng số <= 0 thì bỏ qua tiêu chí. */
    public void add(String key, double weight, double value) {
        if (weight <= 0) {
            return;
        }
        breakdown.put(key, value);
        weightedSum += weight * value;
        weightTotal += weight;
    }

    /** Trung bình có trọng số trong [0, 1]; 0 khi chưa có tiêu chí nào. */
    public double average() {
        return weightTotal == 0 ? 0 : weightedSum / weightTotal;
    }

    public Map<String, Double> breakdown() {
        return Collections.unmodifiableMap(breakdown);
    }
}