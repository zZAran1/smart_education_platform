package com.example.smart_education_platform_backend.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CourseDetailVO {
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
    private LocalDateTime publish_time;
    private LocalDateTime created_at;

    /** 当前用户是否已收藏（登录增强字段，游客恒为 false） */
    private Boolean is_collected = false;
    /** 当前用户是否已报名（登录增强字段，游客恒为 false） */
    private Boolean is_enrolled = false;
    /** 当前用户学习进展 0~100（登录增强字段） */
    private Integer progress = 0;
}
