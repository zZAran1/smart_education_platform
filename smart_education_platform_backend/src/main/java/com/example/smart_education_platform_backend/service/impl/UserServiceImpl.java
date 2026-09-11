package com.example.smart_education_platform_backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.smart_education_platform_backend.converter.Converter;
import com.example.smart_education_platform_backend.exception.CaptchaException;
import com.example.smart_education_platform_backend.exception.LoginException;
import com.example.smart_education_platform_backend.exception.ProfileException;
import com.example.smart_education_platform_backend.exception.RegisterException;
import com.example.smart_education_platform_backend.mapper.UserMapper;
import com.example.smart_education_platform_backend.model.dto.LoginDTO;
import com.example.smart_education_platform_backend.model.dto.RegisterDTO;
import com.example.smart_education_platform_backend.model.dto.ResetPasswordDTO;
import com.example.smart_education_platform_backend.model.dto.UpdateProfileDTO;
import com.example.smart_education_platform_backend.model.entity.Users;
import com.example.smart_education_platform_backend.model.vo.LoginVO;
import com.example.smart_education_platform_backend.model.vo.UserVO;
import com.example.smart_education_platform_backend.service.CaptchaService;
import com.example.smart_education_platform_backend.service.UserService;
import com.example.smart_education_platform_backend.util.BCryptPasswordUtil;
import com.example.smart_education_platform_backend.util.JwtUtil;
import com.example.smart_education_platform_backend.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, Users> implements UserService {

    private static final String LOGIN_FAIL_KEY_PREFIX = "edu:login:fail:";
    private static final Duration LOGIN_FAIL_TTL = Duration.ofMinutes(10);
    private static final int MAX_LOGIN_FAIL_COUNT = 5;

    private static final String RESET_CODE_KEY_PREFIX = "edu:reset:";
    private static final Duration RESET_CODE_TTL = Duration.ofMinutes(5);

    private static final String TOKEN_BLACKLIST_PREFIX = "edu:token:blacklist:";

    private final CaptchaService captchaService;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Value("${file.upload.path}")
    private String uploadPath;

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

    @Override
    public void sendResetCode(String target) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        redisTemplate.opsForValue().set(RESET_CODE_KEY_PREFIX + target, code, RESET_CODE_TTL);
        // 课程设计简化：验证码不回传响应体，只打印到后端日志便于本地调试。
        // 生产环境应改为经短信/邮件网关下发，并删除此日志输出。
        log.info("【重置密码验证码】target={}, code={}, 有效期={}分钟", target, code, RESET_CODE_TTL.toMinutes());
    }

    @Override
    public void resetPassword(ResetPasswordDTO dto) {
        if (!dto.getNew_password().equals(dto.getConfirm_password())) {
            throw new RegisterException("两次输入的密码不一致");
        }
        String key = RESET_CODE_KEY_PREFIX + dto.getTarget();
        String cached = redisTemplate.opsForValue().get(key);
        if (cached == null) {
            throw new CaptchaException("验证码已过期，请重新获取");
        }
        // 一次性：取出后立即删除
        redisTemplate.delete(key);
        if (!cached.equals(dto.getCode())) {
            throw new CaptchaException("验证码错误");
        }
        Users user = lambdaQuery()
                .and(w -> w.eq(Users::getEmail, dto.getTarget()).or().eq(Users::getPhone, dto.getTarget()))
                .one();
        if (user == null) {
            throw new RegisterException("该邮箱/手机号未注册");
        }
        user.setPassword(BCryptPasswordUtil.encode(dto.getNew_password()));
        updateById(user);
    }

    @Override
    public UserVO getProfile() {
        Users user = getById(UserContext.getUserId());
        if (user == null) {
            throw new ProfileException("用户不存在");
        }
        return Converter.INSTANCE.toUserVO(user);
    }

    @Override
    public UserVO updateProfile(UpdateProfileDTO dto) {
        boolean hasNickname = StringUtils.hasText(dto.getNickname());
        boolean hasRealName = StringUtils.hasText(dto.getReal_name());
        // 两字段都为空时会生成不带 SET 子句的 UPDATE 导致 SQL 语法错误，提前拦截
        if (!hasNickname && !hasRealName) {
            throw new ProfileException("请至少填写一项要修改的资料");
        }
        Long userId = UserContext.getUserId();
        lambdaUpdate().eq(Users::getId, userId)
                .set(hasNickname, Users::getNickname, dto.getNickname())
                .set(hasRealName, Users::getReal_name, dto.getReal_name())
                .update();
        return Converter.INSTANCE.toUserVO(getById(userId));
    }

    @Override
    public String updateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ProfileException("请选择要上传的图片");
        }
        String original = file.getOriginalFilename();
        String ext = StringUtils.hasText(original) && original.contains(".")
                ? original.substring(original.lastIndexOf('.')).toLowerCase()
                : "";
        if (!".jpg".equals(ext) && !".png".equals(ext)) {
            throw new ProfileException("头像仅支持 JPG/PNG 格式");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new ProfileException("头像大小不能超过 2MB");
        }
        try {
            Path dir = Paths.get(uploadPath);
            Files.createDirectories(dir);
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            file.transferTo(dir.resolve(filename));
            String avatarUrl = "/uploads/" + filename;
            lambdaUpdate().eq(Users::getId, UserContext.getUserId()).set(Users::getAvatar, avatarUrl).update();
            return avatarUrl;
        } catch (IOException e) {
            throw new ProfileException("头像上传失败");
        }
    }

    @Override
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        long remaining = jwtUtil.parseToken(token).getExpiration().getTime() - System.currentTimeMillis();
        if (remaining > 0) {
            redisTemplate.opsForValue().set(TOKEN_BLACKLIST_PREFIX + token, "1", Duration.ofMillis(remaining));
        }
    }
}
