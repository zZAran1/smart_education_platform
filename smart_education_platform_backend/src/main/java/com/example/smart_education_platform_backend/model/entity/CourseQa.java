package com.example.smart_education_platform_backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_qa")
public class CourseQa {
    @TableId
    private Long id;
    private Long course_id;
    private Long user_id;
    private String question;
    private String answer;
    private Long answerer_id;
    private Integer status;
    private Integer deleted;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
