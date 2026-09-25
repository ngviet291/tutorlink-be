package org.group3.tutorlink.features.auth.exception;

import lombok.Getter;
import org.group3.tutorlink.common.exception.AppException;

import java.time.Instant;

@Getter
public class AccountBanException extends AppException {
    public AccountBanException() {
        super(ErrorCodeAuth.ACCOUNT_BANNED);
        this.lockedUntil = null;
    }

    private final Instant lockedUntil;

    public AccountBanException(Instant lockedUntil) {
        super(ErrorCodeAuth.ACCOUNT_BANNED);
        this.lockedUntil = lockedUntil;
    }

}
