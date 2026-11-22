package org.group3.tutorlink.features.post.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.group3.tutorlink.features.post.validation.validator.BudgetRangeValidator;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BudgetRangeValidator.class)
@Documented
public @interface ValidBudgetRange {

    String message() default "Invalid budget range";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}