package com.example.smart_education_platform_backend.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class CourseCategoryVO {
    private Long id;
    private Long parent_id;
    private String name;
    private Integer type;
    private Integer sort;
    private List<CourseCategoryVO> children;
}
