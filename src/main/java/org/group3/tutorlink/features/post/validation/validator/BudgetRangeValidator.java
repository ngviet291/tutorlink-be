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
    private static final BigDecimal MAX = new BigDecimal("5000000");

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;

        try {
            BeanWrapper wrapper = new BeanWrapperImpl(value);

            BigDecimal minBudget = toBigDecimal(wrapper.getPropertyValue("minBudget"));
            BigDecimal maxBudget = toBigDecimal(wrapper.getPropertyValue("maxBudget"));

            if (minBudget == null || maxBudget == null) return true;

            boolean valid = minBudget.compareTo(MIN) > 0
                    && maxBudget.compareTo(MIN) > 0
                    && maxBudget.compareTo(MAX) <= 0
                    && maxBudget.compareTo(minBudget) > 0;

            return valid;

        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Class must contain minBudget and maxBudget fields", ex
            );
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return null;
    }


}