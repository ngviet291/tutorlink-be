package org.group3.tutorlink.features.chat.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.group3.tutorlink.features.chat.validation.validator.MessageContentValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MessageContentValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMessageContent {

    String message() default "Message must have content or media";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}