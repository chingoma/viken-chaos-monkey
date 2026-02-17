package viken.chaos.monkey.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Standard API response format per enterprise guidelines.
 * HTTP transport layer returns 200 OK; business outcome in payload code.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String code,
        T data,
        Instant timestamp,
        List<String> errors,
        String requestId
) {
    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>(
                "00",
                data,
                Instant.now(),
                Collections.emptyList(),
                requestId
        );
    }

    public static <T> ApiResponse<T> error(String code, List<String> errors, String requestId) {
        return new ApiResponse<>(
                code,
                null,
                Instant.now(),
                errors != null ? errors : Collections.emptyList(),
                requestId
        );
    }

    public static <T> ApiResponse<T> error(String code, String error, String requestId) {
        return error(code, List.of(error), requestId);
    }

    public static <T> ApiResponse<T> of(String code, T data, List<String> errors, String requestId) {
        return new ApiResponse<>(code, data, Instant.now(),
                errors != null ? errors : Collections.emptyList(), requestId);
    }
}
