package com.example.smart_education_platform_backend.interceptor;

import com.example.smart_education_platform_backend.exception.TokenException;
import com.example.smart_education_platform_backend.util.JwtUtil;
import com.example.smart_education_platform_backend.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * 统一鉴权拦截器：除 WebMvcConfig 白名单（登录/注册/验证码/重置密码）外，
 * 所有 /api/** 接口都必须携带有效 Token，并从中解析出 userId 与 role 注入上下文。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_BLACKLIST_PREFIX = "edu:token:blacklist:";
    private static final String BANNED_USER_PREFIX = "edu:user:banned:";

    /** 合法角色：0学员 1教师 2管理员 */
    private static final Set<String> VALID_ROLES = Set.of("0", "1", "2");

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // ① 放行 OPTIONS 预检（不承载业务数据）
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        // ② 无 Token 或格式不正确一律拒绝
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            throw new TokenException("未登录");
        }
        String token = header.substring(BEARER_PREFIX.length());

        // ③ 黑名单校验（注销 / 封禁即时失效）
        if (Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + token))) {
            throw new TokenException("登录已失效");
        }

        // ④ 解析身份：签名、有效期、userId、role 全部校验
        String userId = jwtUtil.getUserId(token);
        String role = jwtUtil.getRole(token);
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(role)) {
            throw new TokenException("登录失效");
        }
        if (!VALID_ROLES.contains(role)) {
            throw new TokenException("登录失效");
        }

        // ⑤ 封禁即时失效：命中封禁标记则拒绝已签发 Token
        if (Boolean.TRUE.equals(redisTemplate.hasKey(BANNED_USER_PREFIX + userId))) {
            throw new TokenException("账号已被封禁");
        }

        UserContext.set(userId, role);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 防止线程复用串号
        UserContext.remove();
    }
}
