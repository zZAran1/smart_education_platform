package com.example.smart_education_platform_backend.util;

import com.example.smart_education_platform_backend.exception.TokenException;

public class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE_HOLDER = new ThreadLocal<>();

    public static void set(String userId, String role) {
        USER_ID_HOLDER.set(Long.valueOf(userId));
        ROLE_HOLDER.set(role);
    }

    public static Long getUserId() {
        Long userId = USER_ID_HOLDER.get();
        if (userId == null) {
            throw new TokenException("未登录");
        }
        return userId;
    }

    public static Long getUserIdOrNull() {
        return USER_ID_HOLDER.get();
    }

    public static String getRole() {
        String role = ROLE_HOLDER.get();
        if (role == null) {
            throw new TokenException("未登录");
        }
        return role;
    }

    public static String getRoleOrNull() {
        return ROLE_HOLDER.get();
    }

    public static void remove() {
        USER_ID_HOLDER.remove();
        ROLE_HOLDER.remove();
    }
}
