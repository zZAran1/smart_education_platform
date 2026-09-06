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
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_BLACKLIST_PREFIX = "edu:token:blacklist:";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** 游客可浏览但登录后需增强（收藏状态、学习进展等）的 GET 路径 */
    private static final List<String> OPTIONAL_AUTH_GET_PATTERNS = List.of(
            "/api/course/*",
            "/api/course/*/chapters",
            "/api/course/*/comments",
            "/api/course/*/questions",
            "/api/job/*"
    );

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // ① 放行 OPTIONS 预检
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)
                ? header.substring(BEARER_PREFIX.length())
                : null;

        if (token == null) {
            // 公开/登录增强接口：无 Token 直接放行（UserContext 为空）
            if (HttpMethod.GET.matches(request.getMethod()) && isOptionalAuthPath(request.getRequestURI())) {
                return true;
            }
            throw new TokenException("未登录");
        }
        // ② 黑名单校验（注销/禁用即时失效）
        if (Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + token))) {
            throw new TokenException("登录已失效");
        }
        // ③ 解析身份（过期/非法抛 TokenException）
        UserContext.set(jwtUtil.getUserId(token), jwtUtil.getRole(token));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 防止线程复用串号
        UserContext.remove();
    }

    private boolean isOptionalAuthPath(String uri) {
        return OPTIONAL_AUTH_GET_PATTERNS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, uri));
    }
}
