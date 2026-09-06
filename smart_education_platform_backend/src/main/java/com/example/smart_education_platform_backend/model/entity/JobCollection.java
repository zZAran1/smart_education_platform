package com.example.smart_education_platform_backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("job_collection")
public class JobCollection {
    @TableId
    private Long id;
    private Long user_id;
    private Long job_id;
    private Integer deleted;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
