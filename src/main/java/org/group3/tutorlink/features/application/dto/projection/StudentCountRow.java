package org.group3.tutorlink.features.application.dto.projection;

import java.util.UUID;

public interface StudentCountRow {
    UUID getStudentId();

    Long getCnt();
}