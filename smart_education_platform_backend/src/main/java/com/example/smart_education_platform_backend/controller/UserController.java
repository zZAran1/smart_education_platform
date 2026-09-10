package com.example.smart_education_platform_backend.controller;

import com.example.smart_education_platform_backend.model.dto.LoginDTO;
import com.example.smart_education_platform_backend.model.dto.RegisterDTO;
import com.example.smart_education_platform_backend.model.dto.ResetCodeDTO;
import com.example.smart_education_platform_backend.model.dto.ResetPasswordDTO;
import com.example.smart_education_platform_backend.model.dto.UpdateProfileDTO;
import com.example.smart_education_platform_backend.model.vo.CaptchaVO;
import com.example.smart_education_platform_backend.model.vo.LoginVO;
import com.example.smart_education_platform_backend.model.vo.ResetCodeVO;
import com.example.smart_education_platform_backend.model.vo.UserVO;
import com.example.smart_education_platform_backend.result.Result;
import com.example.smart_education_platform_backend.service.CaptchaService;
import com.example.smart_education_platform_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private static final String BEARER_PREFIX = "Bearer ";

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

    @PostMapping("/reset-code")
    public Result<ResetCodeVO> resetCode(@Valid @RequestBody ResetCodeDTO dto) {
        return Result.success(userService.sendResetCode(dto.getTarget()));
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(dto);
        return Result.success("密码重置成功");
    }

    @GetMapping("/profile")
    public Result<UserVO> profile() {
        return Result.success(userService.getProfile());
    }

    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        return Result.success(userService.updateProfile(dto));
    }

    @PutMapping("/avatar")
    public Result<String> updateAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success("头像上传成功", userService.updateAvatar(file));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String token = authorization != null && authorization.startsWith(BEARER_PREFIX)
                ? authorization.substring(BEARER_PREFIX.length())
                : null;
        userService.logout(token);
        return Result.success("退出成功");
    }
}
