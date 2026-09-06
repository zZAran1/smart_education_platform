package com.example.smart_education_platform_backend.controller;

import com.example.smart_education_platform_backend.model.dto.LoginDTO;
import com.example.smart_education_platform_backend.model.dto.RegisterDTO;
import com.example.smart_education_platform_backend.model.vo.CaptchaVO;
import com.example.smart_education_platform_backend.model.vo.LoginVO;
import com.example.smart_education_platform_backend.result.Result;
import com.example.smart_education_platform_backend.service.CaptchaService;
import com.example.smart_education_platform_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CaptchaService captchaService;

    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.success(captchaService.generateCaptcha());
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success("注册成功");
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }
}
