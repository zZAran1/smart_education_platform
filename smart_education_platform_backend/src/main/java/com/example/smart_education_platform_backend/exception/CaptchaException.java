package com.example.smart_education_platform_backend.exception;

public class CaptchaException extends BaseBusinessException {
    public CaptchaException(String message) {
        super(1003, message);
    }
}
