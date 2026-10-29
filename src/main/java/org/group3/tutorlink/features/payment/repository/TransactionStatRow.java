package org.group3.tutorlink.features.payment.repository;

import org.group3.tutorlink.features.payment.enums.TransactionStatus;
import org.group3.tutorlink.features.payment.enums.TransactionType;

import java.math.BigDecimal;


public interface TransactionStatRow {

    TransactionType getTransactionType();

    TransactionStatus getTransactionStatus();

    long getTransactionCount();

    BigDecimal getTotalAmount();
}
