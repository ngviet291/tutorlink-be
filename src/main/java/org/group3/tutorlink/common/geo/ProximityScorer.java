package org.group3.tutorlink.common.geo;

/**
 * Quy khoảng cách ra điểm (0..1): càng gần càng cao, bằng 0 khi xa hơn bán kính.
 * Dùng chung cho chatbot tìm bài và xếp hạng ứng viên để hai nơi không lệch công thức.
 */
public final class ProximityScorer {

    private ProximityScorer() {
    }

    public static double score(double distanceKm, double radiusKm) {
        if (Double.isNaN(distanceKm) || radiusKm <= 0) {
            return 0;
        }
        return Math.max(0, 1 - distanceKm / radiusKm);
    }
}