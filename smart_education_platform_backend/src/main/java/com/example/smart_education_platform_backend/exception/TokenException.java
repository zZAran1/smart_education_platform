package com.example.smart_education_platform_backend.exception;

public class TokenException extends BaseBusinessException {
    public TokenException(String message) {
        super(1002, message);
    }
}
