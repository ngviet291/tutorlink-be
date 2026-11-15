package org.group3.tutorlink.features.subject.constant;

/** Giới hạn dữ liệu và phân trang dùng chung cho nghiệp vụ môn học. */
public final class SubjectConstraints {

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 50;
    public static final String DEFAULT_PAGE_SIZE_PARAMETER = "" + DEFAULT_PAGE_SIZE;
    public static final int TEXT_MAX_LENGTH = 255;

    private SubjectConstraints() {
    }
}
