package com.iceeboot.common.exception;

/** Authentication could not be checked; this does not establish that the credential is invalid. */
public class AuthServiceUnavailableException extends RuntimeException {
    public AuthServiceUnavailableException(Throwable cause) {
        super("登录验证服务暂不可用，请稍后重试", cause);
    }
}
