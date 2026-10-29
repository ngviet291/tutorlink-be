package org.group3.tutorlink.features.user.specification;

import org.group3.tutorlink.features.auth.enums.RoleName;
import org.group3.tutorlink.features.user.entity.User;
import org.group3.tutorlink.features.user.enums.UserStatus;
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
}