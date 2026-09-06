package com.example.smart_education_platform_backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.smart_education_platform_backend.converter.Converter;
import com.example.smart_education_platform_backend.exception.LoginException;
import com.example.smart_education_platform_backend.exception.RegisterException;
import com.example.smart_education_platform_backend.mapper.UserMapper;
import com.example.smart_education_platform_backend.model.dto.LoginDTO;
import com.example.smart_education_platform_backend.model.dto.RegisterDTO;
import com.example.smart_education_platform_backend.model.entity.Users;
import com.example.smart_education_platform_backend.model.vo.LoginVO;
import com.example.smart_education_platform_backend.service.CaptchaService;
import com.example.smart_education_platform_backend.service.UserService;
import com.example.smart_education_platform_backend.util.BCryptPasswordUtil;
import com.example.smart_education_platform_backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, Users> implements UserService {

    private static final String LOGIN_FAIL_KEY_PREFIX = "edu:login:fail:";
    private static final Duration LOGIN_FAIL_TTL = Duration.ofMinutes(10);
    private static final int MAX_LOGIN_FAIL_COUNT = 5;

    private final CaptchaService captchaService;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void register(RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirm_password())) {
            throw new RegisterException("两次输入的密码不一致");
        }
        long count = lambdaQuery().eq(Users::getUsername, dto.getUsername()).count();
        if (count > 0) {
            throw new RegisterException("用户名已存在");
        }
        Users user = new Users();
        user.setUsername(dto.getUsername());
        user.setPassword(BCryptPasswordUtil.encode(dto.getPassword()));
        user.setNickname(dto.getUsername());
        user.setEmail(StringUtils.hasText(dto.getEmail()) ? dto.getEmail() : null);
        user.setPhone(StringUtils.hasText(dto.getPhone()) ? dto.getPhone() : null);
        save(user);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // ① 图形验证码（一次性）
        captchaService.verifyCaptcha(dto.getUuid(), dto.getCaptcha());

        // ② 连续失败限制：同一账号 10 分钟内失败 5 次锁定
        String failKey = LOGIN_FAIL_KEY_PREFIX + dto.getUsername();
        String failCount = redisTemplate.opsForValue().get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= MAX_LOGIN_FAIL_COUNT) {
            throw new LoginException("登录失败次数过多，请稍后重试");
        }

        Users user = lambdaQuery().eq(Users::getUsername, dto.getUsername()).one();
        if (user == null || !BCryptPasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, LOGIN_FAIL_TTL);
            throw new LoginException("账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 2) {
            throw new LoginException("账号已被封禁，请联系管理员");
        }
        redisTemplate.delete(failKey);

        String token = jwtUtil.generateToken(String.valueOf(user.getId()), String.valueOf(user.getRole()));
        return new LoginVO(token, Converter.INSTANCE.toUserVO(user));
    }
}
