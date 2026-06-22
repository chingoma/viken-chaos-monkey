package viken.chaos.monkey.common.exception;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tz.dse.trading.core.api.ApiPayloadCode;
import tz.dse.trading.core.api.GenericRestResponse;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Wraps validation and unexpected errors in GenericRestResponse envelope.
 * HTTP transport returns 200 for business/validation outcomes per DSE guidelines.
 */
@RestControllerAdvice
public class GlobalRestControllerAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericRestResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());
        GenericRestResponse<Void> body = GenericRestResponse.validationFailed("Validation failed", errors);
        body.setRequestId(MDC.get("requestId"));
        return ResponseEntity.ok(body);
    }

    @ExceptionHandler(ChaosBusinessException.class)
    public ResponseEntity<GenericRestResponse<Void>> handleChaosBusiness(ChaosBusinessException ex) {
        GenericRestResponse<Void> body = GenericRestResponse.error(ex.getMessage(), ex.getCode());
        body.setRequestId(MDC.get("requestId"));
        return ResponseEntity.ok(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericRestResponse<Void>> handleUnexpected(Exception ex) {
        GenericRestResponse<Void> body = GenericRestResponse.error(
                "Internal server error", ApiPayloadCode.INTERNAL_ERROR);
        body.setRequestId(MDC.get("requestId"));
        return ResponseEntity.status(HttpStatus.OK).body(body);
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
