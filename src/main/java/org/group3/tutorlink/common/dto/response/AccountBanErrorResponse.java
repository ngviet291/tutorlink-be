package org.group3.tutorlink.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountBanErrorResponse extends ErrorResponse {
    private Instant lockedUntil;
    private Long remainingSeconds;
}