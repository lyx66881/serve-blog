package com.blog.common;

/**
 * 业务错误码 + 对应 HTTP 状态码。
 *
 * 规则：
 *  - 0        成功
 *  - 400xx    请求参数 / 上传文件相关错误
 *  - 401xx    认证错误
 *  - 403xx    权限错误
 *  - 404xx    资源不存在
 *  - 405xx    请求方法不支持
 *  - 409xx    资源冲突
 *  - 500xx    服务器内部错误
 */
public enum ErrorCode {

    SUCCESS(0, 200, "ok"),

    BAD_REQUEST(40000, 400, "请求参数错误"),
    FILE_TYPE_NOT_ALLOWED(40001, 400, "不支持的文件类型"),
    FILE_TOO_LARGE(40004, 400, "文件大小超出限制"),
    FILE_UPLOAD_FAILED(40005, 400, "文件上传失败"),
    PARAM_INVALID(40006, 400, "请求参数校验失败"),

    UNAUTHORIZED(40100, 401, "未登录或登录已过期"),
    INVALID_CREDENTIALS(40101, 401, "用户名或密码错误"),
    TOKEN_EXPIRED(40102, 401, "登录已过期，请重新登录"),
    TOKEN_INVALID(40103, 401, "无效的登录凭证"),
    INVALID_OLD_PASSWORD(40104, 400, "原密码不正确"),
    RESET_CODE_INVALID(40105, 400, "验证码错误或已过期"),
    RESET_CODE_TOO_FREQUENT(40106, 400, "验证码发送过于频繁，请稍后再试"),
    EMAIL_NOT_BOUND(40107, 400, "当前账号未绑定邮箱，无法接收验证码"),

    FORBIDDEN(40300, 403, "无权限执行此操作"),

    NOT_FOUND(40400, 404, "资源不存在"),
    USER_NOT_FOUND(40401, 404, "用户不存在"),
    BLOG_NOT_FOUND(40402, 404, "博客不存在"),
    CATEGORY_NOT_FOUND(40403, 404, "分类不存在"),
    FILE_NOT_FOUND(40404, 404, "文件不存在"),

    METHOD_NOT_ALLOWED(40500, 405, "请求方法不支持"),

    CONFLICT(40900, 409, "资源冲突"),
    USERNAME_EXISTS(40901, 409, "用户名已被占用"),
    EMAIL_EXISTS(40902, 409, "邮箱已被占用"),

    INTERNAL_ERROR(50000, 500, "服务器内部错误");

    private final int code;
    private final int httpStatus;
    private final String message;

    ErrorCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
