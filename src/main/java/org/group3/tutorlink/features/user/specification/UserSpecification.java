package org.group3.tutorlink.features.user.specification;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.group3.tutorlink.features.auth.enums.RoleName;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.entity.User;
import org.group3.tutorlink.features.user.enums.UserStatus;
import org.group3.tutorlink.features.user.enums.VerificationStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class UserSpecification {

    private UserSpecification() {}

    public static Specification<User> hasRole(RoleName role) {
        if (role == null) {
            return (root, query, cb) -> cb.conjunction();
        }

        return (root, query, cb) ->
                cb.equal(root.get("role").get("name"), role);
    }

    public static Specification<User> hasStatus(UserStatus status) {
        if (status == null) {
            return (root, query, cb) -> cb.conjunction();
        }

        return (root, query, cb) ->
                cb.equal(root.get("userStatus"), status);
    }

    public static Specification<User> cursor(UUID cursor) {
        if (cursor == null) {
            return (root, query, cb) -> cb.conjunction();
        }

        return (root, query, cb) ->
                cb.lessThan(root.get("id"), cursor);
    }

    public static Specification<User> fetchRole() {
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                root.fetch("role");
            }
            return cb.conjunction();
        };
    }
    public static Specification<User> hasVerificationStatus(VerificationStatus status) {
        if (status == null) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb) -> {
            Subquery<UUID> sub = query.subquery(UUID.class);
            Root<Tutor> tutor = sub.from(Tutor.class);
            sub.select(tutor.get("id")).where(cb.equal(tutor.get("verificationStatus"), status));
            return root.get("id").in(sub);
        };
    }
}