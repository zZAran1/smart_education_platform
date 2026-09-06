package com.example.smart_education_platform_backend.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import com.example.smart_education_platform_backend.exception.CaptchaException;
import com.example.smart_education_platform_backend.model.vo.CaptchaVO;
import com.example.smart_education_platform_backend.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private static final String CAPTCHA_KEY_PREFIX = "captcha:";
    private static final Duration CAPTCHA_TTL = Duration.ofSeconds(300);

    private final StringRedisTemplate redisTemplate;

    @Override
    public CaptchaVO generateCaptcha() {
        CircleCaptcha captcha = CaptchaUtil.createCircleCaptcha(130, 48, 4, 20);
        String uuid = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(CAPTCHA_KEY_PREFIX + uuid, captcha.getCode(), CAPTCHA_TTL);
        return new CaptchaVO(uuid, captcha.getImageBase64Data());
    }

    @Override
    public void verifyCaptcha(String uuid, String code) {
        String key = CAPTCHA_KEY_PREFIX + uuid;
        String cached = redisTemplate.opsForValue().get(key);
        if (cached == null) {
            throw new CaptchaException("验证码已过期，请重新获取");
        }
        // 一次性：取出后立即删除
        redisTemplate.delete(key);
        if (!cached.equalsIgnoreCase(code)) {
            throw new CaptchaException("验证码错误");
        }
    }
}
