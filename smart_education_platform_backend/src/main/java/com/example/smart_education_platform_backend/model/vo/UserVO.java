package com.example.smart_education_platform_backend.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String real_name;
    private String avatar;
    private String email;
    private String phone;
    private Integer role;
    private LocalDateTime created_at;
}
