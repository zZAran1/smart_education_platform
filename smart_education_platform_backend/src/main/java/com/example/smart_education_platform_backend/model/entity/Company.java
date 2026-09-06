package com.example.smart_education_platform_backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("company")
public class Company {
    @TableId
    private Long id;
    private String name;
    private String logo;
    private String industry;
    private String scale;
    private String region;
    private String intro;
    private Integer deleted;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
