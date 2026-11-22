package org.group3.tutorlink.features.application.matching.signal;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.config.MatchingProperties;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.post.entity.Post;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.OptionalDouble;

@Component
@RequiredArgsConstructor
public class PriceSignal implements MatchSignal {
    private final MatchingProperties properties;
    private final ExpectedFeeResolver feeResolver;

    @Override
    public String key() {
        return "price";
    }

    @Override
    public OptionalDouble score(MatchContext context, Application application) {
        Post post = context.getPost();
        MatchingProperties.Price cfg = properties.getPrice();
        Optional<BigDecimal> fee = feeResolver.resolve(application, post);

        // Thiếu ngân sách hoặc thiếu phí: chấm trung tính như trước
        if (fee.isEmpty() || (post.getMinBudget() == null && post.getMaxBudget() == null)) {
            return OptionalDouble.of(cfg.getNoBudgetScore());
        }
        return OptionalDouble.of(fit(fee.get().doubleValue(), post.getMinBudget(), post.getMaxBudget(), cfg));
    }

    private double fit(double fee, BigDecimal min, BigDecimal max, MatchingProperties.Price cfg) {
        double lo = min != null ? min.doubleValue() : 0;
        double hi = max != null ? max.doubleValue() : Double.MAX_VALUE;

        if (fee >= lo && fee <= hi) return 1.0;
        if (fee < lo) return cfg.getBelowBudgetScore();
        if (hi <= 0) return 0;
        return Math.max(0, 1 - (fee - hi) / hi);
    }
}