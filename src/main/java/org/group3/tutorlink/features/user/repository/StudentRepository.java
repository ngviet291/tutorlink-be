package org.group3.tutorlink.features.user.repository;

import org.group3.tutorlink.features.user.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
}
