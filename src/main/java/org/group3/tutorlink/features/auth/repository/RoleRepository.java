package org.group3.tutorlink.features.auth.repository;

import org.group3.tutorlink.features.auth.entity.Role;
import org.group3.tutorlink.features.auth.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByName(RoleName name);
}