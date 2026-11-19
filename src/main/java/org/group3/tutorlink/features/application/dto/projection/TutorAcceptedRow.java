package org.group3.tutorlink.features.application.dto.projection;

import java.util.UUID;

public interface TutorAcceptedRow {
    UUID getTutorId();

    Long getCnt();
}