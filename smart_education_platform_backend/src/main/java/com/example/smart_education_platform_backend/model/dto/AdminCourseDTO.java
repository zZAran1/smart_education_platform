package com.example.smart_education_platform_backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminCourseDTO {
    @NotBlank(message = "课程名称不能为空")
    @Size(max = 100, message = "课程名称不能超过100字")
    private String title;

    @NotNull(message = "课程类型不能为空")
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
    private String intro;
    private String target;
}
