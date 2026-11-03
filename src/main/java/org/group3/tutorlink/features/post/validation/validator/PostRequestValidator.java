package org.group3.tutorlink.features.post.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.group3.tutorlink.features.post.dto.request.CreatePostRequest;
import org.group3.tutorlink.features.post.dto.request.UpdatePostRequest;
import org.group3.tutorlink.features.post.validation.ValidPostRequest;

import java.math.BigDecimal;

public class PostRequestValidator
        implements ConstraintValidator<ValidPostRequest, Object> {

    @Override
    public boolean isValid(
            Object request,
            ConstraintValidatorContext context
    ) {

        if (request == null) {
            return true;
        }

        BigDecimal minBudget;
        BigDecimal maxBudget;

        if (request instanceof CreatePostRequest createRequest) {

            minBudget = createRequest.getMinBudget();
            maxBudget = createRequest.getMaxBudget();

        } else if (request instanceof UpdatePostRequest updateRequest) {

            minBudget = updateRequest.getMinBudget();
            maxBudget = updateRequest.getMaxBudget();

        } else {
            return true;
        }

        if (minBudget != null
                && maxBudget != null
                && minBudget.compareTo(maxBudget) > 0) {

            context.disableDefaultConstraintViolation();

            context.buildConstraintViolationWithTemplate(
                            "Minimum budget must not exceed maximum budget"
                    )
                    .addPropertyNode("minBudget")
                    .addConstraintViolation();

            return false;
        }

        return true;
    }
}