package com.example.smart_education_platform_backend.model.vo;

import lombok.Data;

@Data
public class CourseChapterVO {
    private Long id;
    private Long course_id;
    private String title;
    private Integer resource_type;
    private Integer duration;
    private Integer sort;
}
