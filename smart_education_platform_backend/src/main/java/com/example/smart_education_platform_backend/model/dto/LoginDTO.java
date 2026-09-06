package com.example.smart_education_platform_backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "验证码标识不能为空")
    private String uuid;

    @NotBlank(message = "图形验证码不能为空")
    private String captcha;
}
