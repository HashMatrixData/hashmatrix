package io.hashmatrix.starter.web;

/**
 * 统一 REST 返回结构。
 *
 * <p>约定：{@code code} 为业务码字符串，{@link #SUCCESS_CODE "0"} 表示成功；失败时 {@code data} 为
 * {@code null}，{@code code}/{@code message} 给出业务错误信息。HTTP 状态码与业务码解耦——HTTP 表达
 * 传输层语义，{@code code} 表达业务语义。
 *
 * @param code    业务码（{@code "0"} = 成功）
 * @param message 提示信息
 * @param data    业务数据（失败为 {@code null}）
 * @param <T>     数据类型
 */
public record ApiResponse<T>(String code, String message, T data) {

    /** 成功业务码。 */
    public static final String SUCCESS_CODE = "0";

    /** 成功默认提示。 */
    public static final String SUCCESS_MESSAGE = "OK";

    /** 成功（带数据）。 */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    /** 成功（无数据）。 */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    /** 失败。 */
    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /** 是否成功（{@code code == "0"}）。 */
    public boolean isSuccess() {
        return SUCCESS_CODE.equals(code);
    }
}
