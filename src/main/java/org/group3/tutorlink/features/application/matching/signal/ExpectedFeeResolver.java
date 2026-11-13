package org.group3.tutorlink.features.application.matching.signal;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.config.MatchingProperties;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.BudgetUnit;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ExpectedFeeResolver {
    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    private final MatchingProperties properties;

    /** Ưu tiên phí gia sư đề xuất; không có thì quy giá theo giờ về đơn vị của bài đăng. */
    public Optional<BigDecimal> resolve(Application application, Post post) {
        if (application.getProposedFee() != null) {
            return Optional.of(application.getProposedFee());
        }
        return convertHourlyRate(application.getTutor().getHourlyRate(), post);
    }

    private Optional<BigDecimal> convertHourlyRate(BigDecimal hourlyRate, Post post) {
        if (hourlyRate == null) return Optional.empty();

        BudgetUnit unit = post.getBudgetUnit();
        if (unit == null || unit == BudgetUnit.PER_HOUR) return Optional.of(hourlyRate);
        if (post.getDurationMinutes() == null) return Optional.empty();

        BigDecimal perSession = hourlyRate
                .multiply(BigDecimal.valueOf(post.getDurationMinutes()))
                .divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP);

        return switch (unit) {
            case PER_SESSION -> Optional.of(perSession);
            case PER_MONTH -> perMonth(perSession, post);
            default -> Optional.empty();
        };
    }

    private Optional<BigDecimal> perMonth(BigDecimal perSession, Post post) {
        return Optional.ofNullable(post.getSessionsPerWeek())
                .map(sessions -> perSession
                        .multiply(BigDecimal.valueOf(sessions))
                        .multiply(BigDecimal.valueOf(properties.getWeeksPerMonth())));
    }
}