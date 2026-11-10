package org.group3.tutorlink.features.post.specification;

import jakarta.persistence.criteria.JoinType;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.EducationLevel;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.enums.TeachingMode;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;

import java.math.BigDecimal;

public class PostSpecification {
    public static Specification<Post> keyword(String keyword) {

        return (root, query, cb) -> {

            if (keyword == null || keyword.isBlank()) {
                return null;
            }

            String value = "%" + keyword.toLowerCase().trim() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("title")), value),
                    cb.like(cb.lower(root.get("content")), value)
            );
        };
    }

    public static Specification<Post> hasStatus(PostStatus... statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.length == 0) {
                return null;
            }

            List<PostStatus> validStatuses = Arrays.stream(statuses)
                    .filter(Objects::nonNull)
                    .toList();

            if (validStatuses.isEmpty()) {
                return null;
            }

            return root.get("status").in(validStatuses);
        };
    }

    public static Specification<Post> hasType(PostType type) {

        return (root, query, cb) -> {

            if (type == null) {
                return null;
            }

            return cb.equal(
                    root.get("type"),
                    type
            );
        };
    }

    public static Specification<Post> hasStatusNotRemoved(PostStatus postStatus) {
        return (root, query, cb) -> {
            if (postStatus == null) {
                return cb.or(
                        cb.notEqual(root.get("status"), PostStatus.REMOVED),
                        cb.isNull(root.get("status"))
                );
            }

            return cb.and(
                    cb.equal(root.get("status"), postStatus),
                    cb.notEqual(root.get("status"), PostStatus.REMOVED)
            );
        };
    }

    public static Specification<Post> hasAuthorId(UUID authorId) {
        return (root, query, cb) -> {
            if (authorId == null) {
                return null;
            }

            return cb.equal(
                    root.get("author").get("id"),
                    authorId
            );
        };
    }
    public static Specification<Post> hasTeachingMode(
            TeachingMode teachingMode) {

        return (root, query, cb) -> {

            if (teachingMode == null) {
                return null;
            }

            return cb.equal(
                    root.get("teachingMode"),
                    teachingMode
            );
        };
    }

    public static Specification<Post> hasSubject(String subject) {

        return (root, query, cb) -> {

            if (subject == null) {
                return null;
            }

            return cb.equal(
                    root.get("subject").get("name"),
                    subject
            );
        };
    }

    public static Specification<Post> hasEducationLevel(
            EducationLevel educationLevel) {

        return (root, query, cb) -> {

            if (educationLevel == null) {
                return null;
            }

            return cb.equal(
                    root.get("educationLevel"),
                    educationLevel
            );
        };
    }






    public static Specification<Post> minBudgetGreaterThanOrEqual(
            BigDecimal minBudget) {

        return (root, query, cb) -> {

            if (minBudget == null) {
                return null;
            }

            return cb.greaterThanOrEqualTo(
                    root.get("minBudget"),
                    minBudget
            );
        };
    }
    public static Specification<Post> maxBudgetLessThanOrEqual(
            BigDecimal maxBudget) {

        return (root, query, cb) -> {

            if (maxBudget == null) {
                return null;
            }

            return cb.lessThanOrEqualTo(
                    root.get("maxBudget"),
                    maxBudget
            );
        };
    }

    public static Specification<Post> cursor(UUID cursor) {

        return (root, query, cb) -> {

            if (cursor == null) {
                return null;
            }

            return cb.lessThan(
                    root.get("id"),
                    cursor
            );
        };
    }

    public static Specification<Post> fetchSubject() {
        return (root, query, cb) -> {

            if (query.getResultType() != Long.class
                    && query.getResultType() != long.class) {
                root.fetch("subject", JoinType.LEFT);
            }

            return null;
        };
    }
    public static Specification<Post> fetchAuthor() {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class
                    && query.getResultType() != long.class) {
                root.fetch("author", JoinType.LEFT);
            }
            return null;
        };
    }

    public static Specification<Post> notExpired(Instant now) {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("deadline")),
                cb.greaterThan(root.<Instant>get("deadline"), now)
        );
    }

    public static Specification<Post> subjectContains(String subjectName) {
        return (root, query, cb) -> !StringUtils.hasText(subjectName) ? null
                : cb.like(cb.lower(root.get("subject").<String>get("name")), like(subjectName));
    }

    public static Specification<Post> provinceContains(String province) {
        return (root, query, cb) -> !StringUtils.hasText(province) ? null
                : cb.like(cb.lower(root.get("address").<String>get("province")), like(province));
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
}