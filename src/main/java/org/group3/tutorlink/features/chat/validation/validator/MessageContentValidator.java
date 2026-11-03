package org.group3.tutorlink.features.chat.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.group3.tutorlink.features.chat.dto.request.SendMessageRequest;
import org.group3.tutorlink.features.chat.validation.annotation.ValidMessageContent;

public class MessageContentValidator
        implements ConstraintValidator<ValidMessageContent, SendMessageRequest> {

    @Override
    public boolean isValid(
            SendMessageRequest request,
            ConstraintValidatorContext context) {

        if (request == null) {
            return true;
        }

        boolean hasContent = request.getContent() != null
                && !request.getContent().isBlank();

        boolean hasMedia = request.getMediaList() != null
                && !request.getMediaList().isEmpty();

        if (hasContent || hasMedia) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                        context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("content")
                .addConstraintViolation();

        return false;
    }
}