package com.example.smart_education_platform_backend.exception;

public class CourseException extends BaseBusinessException {
    public CourseException(String message) {
        super(1010, message);
    }
}
