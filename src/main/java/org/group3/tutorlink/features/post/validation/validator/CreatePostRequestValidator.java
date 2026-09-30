package org.group3.tutorlink.features.post.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.group3.tutorlink.features.post.dto.request.CreatePostRequest;
import org.group3.tutorlink.features.post.enums.TeachingMode;
import org.group3.tutorlink.features.post.validation.annotation.ValidCreatePostRequest;

import java.math.BigDecimal;

public class CreatePostRequestValidator
        implements ConstraintValidator<ValidCreatePostRequest, CreatePostRequest> {

    @Override
    public boolean isValid(
            CreatePostRequest request,
            ConstraintValidatorContext context
    ) {

        if (request == null) {
            return true;
        }

        boolean valid = true;

        context.disableDefaultConstraintViolation();

        /*
         * 1. Validate budget range
         */
        BigDecimal minBudget = request.getMinBudget();
        BigDecimal maxBudget = request.getMaxBudget();

        if (minBudget != null
                && maxBudget != null
                && minBudget.compareTo(maxBudget) > 0) {

            context.buildConstraintViolationWithTemplate(
                            "Minimum budget must not exceed maximum budget"
                    )
                    .addPropertyNode("minBudget")
                    .addConstraintViolation();

            valid = false;
        }

        /*
         * 2. Validate address based on teaching mode
         */
        TeachingMode teachingMode = request.getTeachingMode();

        if (teachingMode == TeachingMode.OFFLINE
                || teachingMode == TeachingMode.BOTH) {

            if (request.getAddress() == null) {

                context.buildConstraintViolationWithTemplate(
                                "Address is required for OFFLINE or BOTH teaching mode"
                        )
                        .addPropertyNode("address")
                        .addConstraintViolation();

                valid = false;
            }
        }

        return valid;
    }
}