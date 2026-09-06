package com.example.smart_education_platform_backend.util;

import com.example.smart_education_platform_backend.exception.TokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:604800000}")
    private long expiration;

    private volatile SecretKey secretKey;

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
