package org.group3.tutorlink.features.auth.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.group3.tutorlink.features.auth.dto.request.ChangePasswordRequest;
import org.group3.tutorlink.features.auth.validation.annotation.DifferentPassword;

public class DifferentPasswordValidator
        implements ConstraintValidator<DifferentPassword, ChangePasswordRequest> {

    @Override
    public boolean isValid(
            ChangePasswordRequest request,
            ConstraintValidatorContext context
    ) {
        if (request == null
                || request.getCurrentPassword() == null
                || request.getNewPassword() == null) {
            return true;
        }

        boolean valid = !request.getCurrentPassword()
                .equals(request.getNewPassword());

        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    context.getDefaultConstraintMessageTemplate()
            )
            .addPropertyNode("newPassword")
            .addConstraintViolation();
        }

        return valid;
    }
}