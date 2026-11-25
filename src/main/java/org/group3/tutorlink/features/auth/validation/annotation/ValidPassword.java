package org.group3.tutorlink.features.auth.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.group3.tutorlink.features.auth.validation.validator.PasswordValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    String message() default "Password must be at least 8 characters and include an uppercase letter, a lowercase letter, a digit, a special character and no spaces";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}