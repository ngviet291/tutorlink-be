package org.group3.tutorlink.features.post.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.group3.tutorlink.features.post.validation.validator.PostStatusValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PostStatusValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPostStatus {

    String message() default "Post status must be DRAFT or PUBLISHED";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}