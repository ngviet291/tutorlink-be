package org.group3.tutorlink.common.utils;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Chạy Runnable sau khi transaction hiện tại commit.
 * Dùng cho các side-effect không rollback được (vd: ghi Redis delay queue)
 * để tránh job chạy trước khi DB thấy dữ liệu.
 */
@Component
public class AfterCommit {

    public void run(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
        } else {
            runnable.run();
        }
    }
}
