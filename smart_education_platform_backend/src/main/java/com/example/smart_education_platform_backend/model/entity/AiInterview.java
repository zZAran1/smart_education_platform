package com.example.smart_education_platform_backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_interview")
public class AiInterview {
    @TableId
    private Long id;
    private Long user_id;
    private Long job_id;
    private Long application_id;
    private Integer status;
    private String report;
    private LocalDateTime interview_time;
    private Integer deleted;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
