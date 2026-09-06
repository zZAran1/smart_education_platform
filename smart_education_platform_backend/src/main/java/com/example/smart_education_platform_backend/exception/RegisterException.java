package com.example.smart_education_platform_backend.exception;

public class RegisterException extends BaseBusinessException {
    public RegisterException(String message) {
        super(1000, message);
    }
}
