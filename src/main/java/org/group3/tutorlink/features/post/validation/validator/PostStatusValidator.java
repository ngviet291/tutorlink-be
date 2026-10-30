package org.group3.tutorlink.features.post.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.group3.tutorlink.features.post.validation.annotation.ValidPostStatus;

import java.util.EnumSet;

public class PostStatusValidator
        implements ConstraintValidator<ValidPostStatus, PostStatus> {

    @Override
    public boolean isValid(
            PostStatus value,
            ConstraintValidatorContext context
    ) {
        return value == null
                || EnumSet.of(PostStatus.DRAFT, PostStatus.PUBLISHED)
                          .contains(value);
    }
}