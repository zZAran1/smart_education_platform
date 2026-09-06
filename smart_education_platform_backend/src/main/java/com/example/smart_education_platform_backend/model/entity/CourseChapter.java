package com.example.smart_education_platform_backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_chapter")
public class CourseChapter {
    @TableId
    private Long id;
    private Long course_id;
    private String title;
    private Integer resource_type;
    private Integer duration;
    private Integer sort;
    private Integer deleted;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
