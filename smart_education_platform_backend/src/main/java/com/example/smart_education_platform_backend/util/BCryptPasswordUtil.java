package com.example.smart_education_platform_backend.util;

import org.mindrot.jbcrypt.BCrypt;

public class BCryptPasswordUtil {

    public static String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        return BCrypt.checkpw(rawPassword, encodedPassword);
    }
}
