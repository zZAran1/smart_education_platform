package com.example.smart_education_platform_backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("job")
public class Job {
    @TableId
    private Long id;
    private String title;
    private String city;
    private String address;
    private Integer salary_min;
    private Integer salary_max;
    private Integer headcount;
    private Long company_id;
    private Long category_id;
    private String description;
    private String requirement;
    private Integer status;
    private Integer deleted;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
