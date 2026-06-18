package io.hashmatrix.starter.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：把异常统一转为 {@link ApiResponse} 出参，避免各服务各写一套错误结构。
 *
 * <ul>
 *   <li>{@link BusinessException} → 其携带的 HTTP 状态 + 业务码。</li>
 *   <li>{@link MethodArgumentNotValidException} → 400 + {@code VALIDATION_ERROR}。</li>
 *   <li>其它未捕获异常 → 500 + {@code INTERNAL_ERROR}，并记录日志；不向客户端泄露内部细节。</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 参数校验失败业务码。 */
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    /** 兜底内部错误业务码。 */
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        log.debug("Business exception [{}]: {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null
                ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                : "Validation failed";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(VALIDATION_ERROR, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(INTERNAL_ERROR, "Internal server error"));
    }
}
