package org.group3.tutorlink.features.auth.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.group3.tutorlink.features.auth.validation.validator.ImageFilesValidator;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ImageFilesValidator.class)
public @interface ValidImageFiles {
    String message() default "Danh sách ảnh không hợp lệ";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    int maxFiles() default 5;
    long maxSizeBytes() default 5L * 1024 * 1024;
}