package com.example.smart_education_platform_backend.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学员「我的课程」视图：报名记录 + 课程展示字段 + 学习进度。
 * 课程被删除时仅保留 course_id 与报名信息，其余字段留空。
 */
@Data
public class MyCourseVO {

    /** 报名记录 ID */
    private Long id;
    private Long course_id;
    private String title;
    private String cover;
    private Integer type;
    private Integer level;
    private String teacher_name;
    private Integer is_free;
    private BigDecimal score;
    private Integer student_count;
    /** 学习进展 0~100 */
    private Integer progress;
    private Integer finish_count;
    private Integer total_count;
    /** 报名时间 */
    private LocalDateTime enroll_time;
}
