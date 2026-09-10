package com.example.smart_education_platform_backend.exception;

public class OrderException extends BaseBusinessException {
    public OrderException(String message) {
        super(1012, message);
    }
}
