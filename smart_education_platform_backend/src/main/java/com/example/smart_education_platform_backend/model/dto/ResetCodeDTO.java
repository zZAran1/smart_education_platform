package com.example.smart_education_platform_backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetCodeDTO {
    @NotBlank(message = "邮箱或手机号不能为空")
    private String target;
}
