package org.group3.tutorlink.features.chatbot.specification;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.EducationLevel;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;


public final class PostSearchSpecification {

    private PostSearchSpecification() {
    }

    public static Specification<Post> fetchSubjectAndAuthor() {
        return (root, query, cb) -> {
            // Câu COUNT của Page không được fetch join nên phải bỏ qua
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("subject", JoinType.LEFT);
                root.fetch("author", JoinType.LEFT);
            }
            return null;
        };
    }

    public static Specification<Post> isOpen(Instant now) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), PostStatus.PUBLISHED),
                cb.or(cb.isNull(root.get("deadline")), cb.greaterThan(root.<Instant>get("deadline"), now))
        );
    }

    public static Specification<Post> hasType(PostType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<Post> subjectContains(String subjectName) {
        return (root, query, cb) -> isBlank(subjectName) ? null
                : cb.like(cb.lower(root.get("subject").<String>get("name")), like(subjectName));
    }

    public static Specification<Post> provinceContains(String province) {
        return (root, query, cb) -> isBlank(province) ? null
                : cb.like(cb.lower(root.get("address").<String>get("province")), like(province));
    }

    public static Specification<Post> hasEducationLevel(EducationLevel level) {
        return (root, query, cb) -> level == null ? null : cb.equal(root.get("educationLevel"), level);
    }
    public static Specification<Post> authorTutorMatches(Integer minExperienceYears,
                                                         Double minRate,
                                                         Double maxRate,
                                                         Double minRating) {
        boolean noTutorCondition = minExperienceYears == null && minRate == null
                && maxRate == null && minRating == null;
        if (noTutorCondition) {
            return (root, query, cb) -> null;
        }
        return (root, query, cb) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<Tutor> tutor = subquery.from(Tutor.class);

            List<Predicate> conditions = new ArrayList<>();
            conditions.add(cb.equal(tutor.get("id"), root.get("author").get("id")));
            if (minExperienceYears != null) {
                conditions.add(cb.greaterThanOrEqualTo(tutor.<Integer>get("experienceYears"), minExperienceYears));
            }
            if (minRate != null) {
                conditions.add(cb.greaterThanOrEqualTo(tutor.<Double>get("hourlyRate"), minRate));
            }
            if (maxRate != null) {
                conditions.add(cb.lessThanOrEqualTo(tutor.<Double>get("hourlyRate"), maxRate));
            }
            if (minRating != null) {
                conditions.add(cb.greaterThanOrEqualTo(tutor.<Double>get("averageRating"), minRating));
            }
            subquery.select(tutor.<UUID>get("id")).where(conditions.toArray(Predicate[]::new));
            return cb.exists(subquery);
        };
    }

    private static String like(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
