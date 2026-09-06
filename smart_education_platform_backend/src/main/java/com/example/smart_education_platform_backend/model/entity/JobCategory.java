package com.example.smart_education_platform_backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("job_category")
public class JobCategory {
    @TableId
    private Long id;
    private Long parent_id;
    private String name;
    private Integer sort;
    private Integer deleted;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
