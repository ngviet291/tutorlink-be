package org.group3.tutorlink.features.application.matching.signal;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.matching.AreaMatcher;
import org.springframework.stereotype.Component;

import java.util.OptionalDouble;

/** Khu vực nhận dạy gần địa chỉ bài đăng thì điểm cao (khoảng cách Haversine, giống chatbot). */
@Component
@RequiredArgsConstructor
public class AreaSignal implements MatchSignal {
    private final AreaMatcher areaMatcher;

    @Override
    public String key() {
        return "area";
    }

    @Override
    public OptionalDouble score(MatchContext context, Application application) {
        return areaMatcher.evaluate(application.getTutor(), context.getPost())
                .map(fit -> OptionalDouble.of(fit.score()))
                .orElseGet(OptionalDouble::empty);
    }
}