package org.group3.tutorlink.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "matching")
public class MatchingProperties {

    /** Số ứng viên tốt nhất hiển thị ở "top"; bằng hoặc ít hơn số này thì không chấm điểm. */
    @Min(1)
    private int topN = 5;

    /** Trọng số theo khóa của MatchSignal: price, rating, experience, trust, semantic. */
    private Map<String, Double> weights = new HashMap<>();

    // ---------- Rating (trung bình Bayesian) ----------
    /** Số review "ảo" kéo gia sư ít review về mức trung bình hệ thống. */
    @Min(0)
    private int minVotes = 5;

    /** Điểm trung bình toàn hệ thống dùng làm điểm gốc cho Bayesian. */
    @DecimalMin("0.0")
    private double globalAvgRating = 4.0;

    /** Điểm rating tối đa, dùng để chuẩn hóa về [0, 1]. */
    @DecimalMin("0.1")
    private double maxRating = 5.0;

    // ---------- Kinh nghiệm ----------
    /** Số năm kinh nghiệm đạt điểm tối đa. Phải là double để phép chia không bị cắt phần thập phân. */
    @DecimalMin("0.1")
    private double experienceCapYears = 5.0;

    // ---------- Quy đổi phí ----------
    /** Số tuần trung bình mỗi tháng, dùng khi quy học phí theo giờ sang theo tháng. */
    @DecimalMin("0.1")
    private double weeksPerMonth = 4.33;

    @Valid
    private Price price = new Price();

    @Valid
    private Penalty penalty = new Penalty();

    @Valid
    private Trust trust = new Trust();

    @Valid
    private Semantic semantic = new Semantic();

    /** Trọng số của một tín hiệu; chưa cấu hình thì coi như 0 (tín hiệu không đóng góp). */
    public double weightOf(String signalKey) {
        return weights.getOrDefault(signalKey, 0.0);
    }

    @Getter
    @Setter
    public static class Price {
        /** Điểm trung tính khi thiếu ngân sách hoặc thiếu phí để so sánh. */
        @DecimalMin("0.0")
        private double noBudgetScore = 0.5;

        /** Điểm khi phí thấp hơn ngân sách tối thiểu. */
        @DecimalMin("0.0")
        private double belowBudgetScore = 0.8;
    }

    /** Hệ số nhân (0 đến 1) cho từng loại không khớp; càng nhỏ thì phạt càng nặng. */
    @Getter
    @Setter
    public static class Penalty {
        @DecimalMin("0.0")
        private double subject = 0.3;

        @DecimalMin("0.0")
        private double level = 0.7;

        @DecimalMin("0.0")
        private double mode = 0.6;

        @DecimalMin("0.0")
        private double area = 0.7;

        @DecimalMin("0.0")
        private double availability = 0.8;
    }

    @Getter
    @Setter
    public static class Trust {
        /** Số lớp đã được nhận để đạt điểm tin cậy tối đa. */
        @Min(1)
        private int acceptedCap = 10;
    }

    @Getter
    @Setter
    public static class Semantic {
        /** Cosine thô tương ứng với điểm 0. */
        private double floor = 0.35;

        /** Cosine thô tương ứng với điểm 1. */
        private double ceiling = 0.85;

        /** Index lại toàn bộ gia sư vào vector store khi khởi động. */
        private boolean reindexOnStartup = false;

        @AssertTrue(message = "matching.semantic.ceiling phải lớn hơn floor")
        public boolean isRangeValid() {
            return ceiling > floor;
        }
    }
}