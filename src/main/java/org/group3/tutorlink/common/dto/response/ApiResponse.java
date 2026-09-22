package org.group3.tutorlink.common.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    @Builder.Default
    private String code = "SUCCESS";

    @Builder.Default
    private String message = "Request processed successfully";

    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().data(data).build();
    }

    public static ApiResponse<Void> success() {
        return ApiResponse.<Void>builder().build();
    }
}