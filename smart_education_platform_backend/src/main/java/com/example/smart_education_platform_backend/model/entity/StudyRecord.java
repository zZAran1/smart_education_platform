package com.example.smart_education_platform_backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("study_record")
public class StudyRecord {
    @TableId
    private Long id;
    private Long user_id;
    private Long course_id;
    private Long chapter_id;
    private Integer deleted;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
