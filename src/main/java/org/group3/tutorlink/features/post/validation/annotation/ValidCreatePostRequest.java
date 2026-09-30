package org.group3.tutorlink.features.post.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.group3.tutorlink.features.post.validation.validator.CreatePostRequestValidator;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CreatePostRequestValidator.class)
@Documented
public @interface ValidCreatePostRequest {

    String message() default "Invalid post request data";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}