package com.example.smart_education_platform_backend.util;

import com.example.smart_education_platform_backend.exception.TokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    /** HS256 要求的最小密钥长度（字节） */
    private static final int MIN_SECRET_BYTES = 32;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:604800000}")
    private long expiration;

    private volatile SecretKey secretKey;

    /**
     * 启动期校验密钥，配置缺失或强度不足时直接快速失败。
     * 否则会在首次登录/鉴权时才抛出异常，表现为"登录失败但查不到原因"。
     */
    @PostConstruct
    public void validateSecret() {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException(
                    "未配置 jwt.secret：请通过环境变量 JWT_SECRET 注入 Base64 编码的密钥（解码后至少 32 字节）");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("jwt.secret 不是合法的 Base64 字符串");
        }
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret 解码后仅 " + keyBytes.length + " 字节，HS256 要求至少 " + MIN_SECRET_BYTES + " 字节");
        }
    }

    private SecretKey key() {
        if (secretKey == null) {
            synchronized (this) {
                if (secretKey == null) {
                    secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
                }
            }
        }
        return secretKey;
    }

    public String generateToken(String userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserId(String token) {
        try {
            return parseToken(token).getSubject();
        } catch (ExpiredJwtException e) {
            throw new TokenException("登录已过期");
        } catch (Exception e) {
            throw new TokenException("登录失效");
        }
    }

    public String getRole(String token) {
        try {
            return parseToken(token).get("role", String.class);
        } catch (ExpiredJwtException e) {
            throw new TokenException("登录已过期");
        } catch (Exception e) {
            throw new TokenException("登录失效");
        }
    }

    public long getExpiration() {
        return expiration;
    }
}
