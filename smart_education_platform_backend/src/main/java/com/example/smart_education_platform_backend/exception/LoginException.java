package com.example.smart_education_platform_backend.exception;

public class LoginException extends BaseBusinessException {
    public LoginException(String message) {
        super(1001, message);
    }
}
