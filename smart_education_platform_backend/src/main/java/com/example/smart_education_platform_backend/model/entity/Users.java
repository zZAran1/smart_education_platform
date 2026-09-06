package com.example.smart_education_platform_backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@TableName("users")
public class Users {
    @TableId
    private Long id;
    private String username;
    private String password;
    private String email;
    private String nickname;
    private String phone;
    private String real_name;
    private String avatar;
    private Integer role;
    private Integer status;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    private Integer deleted;
}
