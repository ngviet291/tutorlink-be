package org.group3.tutorlink.features.application.matching.signal;

import org.group3.tutorlink.features.application.entity.Application;

import java.util.OptionalDouble;

public interface MatchSignal {

    /** Khóa dùng cho trọng số (matching.weights.<key>) và breakdown trả về FE. */
    String key();

    /** Điểm trong [0, 1]; empty nghĩa là thiếu dữ liệu, tín hiệu bị bỏ qua. */
    OptionalDouble score(MatchContext context, Application application);
}