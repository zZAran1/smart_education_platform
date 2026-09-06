package com.example.smart_education_platform_backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course {
    @TableId
    private Long id;
    private String title;
    private Integer type;
    private Long tech_system_id;
    private Long tech_direction_id;
    private Integer level;
    private String cover;
    private Long teacher_id;
    private String teacher_name;
    private Integer courseware_count;
    private Integer video_count;
    private Integer lab_count;
    private Integer is_free;
    private BigDecimal price;
    private BigDecimal score;
    private Integer rating_count;
    private Integer student_count;
    private String intro;
    private String target;
    private Integer status;
    private LocalDateTime publish_time;
    private Integer deleted;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
