package io.hashmatrix.starter.web;

import org.springframework.http.HttpStatus;

/**
 * 业务异常：携带业务码与建议的 HTTP 状态，由 {@link GlobalExceptionHandler} 统一转 {@link ApiResponse}。
 *
 * <p>业务可继承本类定义领域异常；默认 HTTP 状态为 {@code 400 Bad Request}。
 */
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BusinessException(String code, String message) {
        this(HttpStatus.BAD_REQUEST, code, message);
    }

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
