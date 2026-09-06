package com.example.smart_education_platform_backend.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseQueryDTO {
    @NotNull(message = "课程类型不能为空")
    private Integer type;

    private Long tech_system_id;
    private Long tech_direction_id;
    private Integer level;
    private Integer is_free;

    @Size(max = 50, message = "搜索关键词不能超过50字")
    private String keyword;

    /** 排序字段：publish_time / student_count / score，默认 publish_time 倒序 */
    private String sort_by;

    @Min(value = 1, message = "页码最小为1")
    private Integer page_num = 1;

    @Min(value = 1, message = "每页条数最小为1")
    private Integer page_size = 10;
}
