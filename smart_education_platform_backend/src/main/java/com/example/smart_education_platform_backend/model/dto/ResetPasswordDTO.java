package com.example.smart_education_platform_backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordDTO {
    @NotBlank(message = "邮箱或手机号不能为空")
    private String target;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度须为6~32位")
    private String new_password;

    @NotBlank(message = "确认密码不能为空")
    private String confirm_password;
}
