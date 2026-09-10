package com.example.smart_education_platform_backend.exception;

public class ProfileException extends BaseBusinessException {
    public ProfileException(String message) {
        super(1004, message);
    }
}
