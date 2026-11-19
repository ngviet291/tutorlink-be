package org.group3.tutorlink.features.post.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.group3.tutorlink.features.post.validation.validator.PostRequestValidator;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = PostRequestValidator.class)
public @interface ValidPostRequest {

    String message() default "Invalid post request";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}