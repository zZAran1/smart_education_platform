package com.example.smart_education_platform_backend.service;

import com.example.smart_education_platform_backend.model.vo.CaptchaVO;

public interface CaptchaService {

    /** 生成图形验证码：Hutool CircleCaptcha，文本存 Redis captcha:{uuid}（300s） */
    CaptchaVO generateCaptcha();

    /** 校验图形验证码（一次性，验证后立即删除） */
    void verifyCaptcha(String uuid, String code);
}
