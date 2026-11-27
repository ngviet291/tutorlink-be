package org.group3.tutorlink.features.post.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.group3.tutorlink.features.post.validation.annotation.ValidBudgetRange;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.math.BigDecimal;

public class BudgetRangeValidator
        implements ConstraintValidator<ValidBudgetRange, Object> {

    private static final BigDecimal MIN = new BigDecimal("10000");
    private static final BigDecimal MAX = new BigDecimal("50000000");

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        try {
            BeanWrapper wrapper = new BeanWrapperImpl(value);
            BigDecimal minBudget = toBigDecimal(wrapper.getPropertyValue("minBudget"));
            BigDecimal maxBudget = toBigDecimal(wrapper.getPropertyValue("maxBudget"));
            if (minBudget == null || maxBudget == null) {
                return true;
            }

            boolean minValid = minBudget.compareTo(MIN) > 0;

            boolean maxValid = maxBudget.compareTo(MIN) > 0 && maxBudget.compareTo(MAX) <= 0;

            boolean rangeValid = maxBudget.compareTo(minBudget) > 0;
            if (minValid && maxValid && rangeValid) {
                return true;
            }

            context.disableDefaultConstraintViolation();

            if (!minValid) {
                context.buildConstraintViolationWithTemplate("Minimum budget must be greater than 10,000 VND")
                        .addPropertyNode("minBudget")
                        .addConstraintViolation();
            }

            if (!maxValid) {
                context.buildConstraintViolationWithTemplate("Maximum budget must be greater than 10,000 and not exceed 5,000,000 VND")
                        .addPropertyNode("maxBudget")
                        .addConstraintViolation();
            }

            if (minValid && maxValid && !rangeValid) {
                context.buildConstraintViolationWithTemplate("Maximum budget must be greater than minimum budget")
                        .addPropertyNode("maxBudget")
                        .addConstraintViolation();
            }

            return false;

        } catch (Exception ex) {
            throw new IllegalArgumentException("Class must contain minBudget and maxBudget fields", ex);
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal decimal) {
            return decimal;
        }

        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return null;
    }
}