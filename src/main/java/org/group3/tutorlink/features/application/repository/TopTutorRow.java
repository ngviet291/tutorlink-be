package org.group3.tutorlink.features.application.repository;

import java.util.UUID;

public interface TopTutorRow {
    UUID getTutorId();
    String getFullName();
    double getAverageRating();
    long getAcceptedCount();
}
