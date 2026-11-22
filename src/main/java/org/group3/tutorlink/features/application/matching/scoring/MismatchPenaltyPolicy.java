package org.group3.tutorlink.features.application.matching.scoring;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.config.MatchingProperties;
import org.group3.tutorlink.features.application.enums.MatchWarning;
import org.group3.tutorlink.features.application.matching.AreaMatcher;
import org.group3.tutorlink.features.application.matching.AvailabilityChecker;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.EducationLevel;
import org.group3.tutorlink.features.post.enums.TeachingMode;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MismatchPenaltyPolicy {
    private final AreaMatcher areaMatcher;
    private final AvailabilityChecker availabilityChecker;
    private final MatchingProperties properties;

    public List<MatchWarning> detect(Tutor tutor, Post post) {
        List<MatchWarning> warnings = new ArrayList<>();
        if (!subjectMatches(tutor, post)) warnings.add(MatchWarning.SUBJECT_MISMATCH);
        if (!levelMatches(tutor.getTeachingLevels(), post.getEducationLevel())) warnings.add(MatchWarning.LEVEL_MISMATCH);
        if (!modeMatches(tutor.getTeachingMode(), post.getTeachingMode())) warnings.add(MatchWarning.MODE_MISMATCH);
        if (!areaMatcher.matches(tutor, post)) warnings.add(MatchWarning.AREA_MISMATCH);
        if (!availabilityChecker.hasCapacity(tutor, post)) warnings.add(MatchWarning.AVAILABILITY_MISMATCH);
        return warnings;
    }

    /** Không loại hẳn, chỉ giảm điểm để học viên vẫn lướt xem được. */
    public double factorOf(List<MatchWarning> warnings) {
        MatchingProperties.Penalty penalty = properties.getPenalty();
        return warnings.stream()
                .mapToDouble(warning -> switch (warning) {
                    case SUBJECT_MISMATCH -> penalty.getSubject();
                    case LEVEL_MISMATCH -> penalty.getLevel();
                    case MODE_MISMATCH -> penalty.getMode();
                    case AREA_MISMATCH -> penalty.getArea();
                    case AVAILABILITY_MISMATCH -> penalty.getAvailability();
                })
                .reduce(1.0, (a, b) -> a * b);
    }

    private boolean subjectMatches(Tutor tutor, Post post) {
        if (post.getSubject() == null || tutor.getSubjects() == null) return false;
        UUID postSubjectId = post.getSubject().getId();
        return tutor.getSubjects().stream().anyMatch(s -> s.getId().equals(postSubjectId));
    }

    /** Thiếu dữ liệu (gia sư cũ) thì coi là trung tính, không phạt. */
    private boolean levelMatches(Set<EducationLevel> tutorLevels, EducationLevel postLevel) {
        if (tutorLevels == null || tutorLevels.isEmpty() || postLevel == null) return true;
        return tutorLevels.contains(postLevel);
    }

    private boolean modeMatches(TeachingMode tutorMode, TeachingMode postMode) {
        if (tutorMode == null || postMode == null) return true;
        if (tutorMode == TeachingMode.BOTH || postMode == TeachingMode.BOTH) return true;
        return tutorMode == postMode;
    }
}