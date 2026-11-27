package org.group3.tutorlink.features.auth.validation.annotation;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.group3.tutorlink.features.auth.validation.validator.DifferentPasswordValidator;

import java.lang.annotation.*;


@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DifferentPasswordValidator.class)
@Documented
public @interface DifferentPassword {
    String message() default "New password must be different from current password";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}