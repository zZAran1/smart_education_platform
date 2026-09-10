package com.example.smart_education_platform_backend.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class JobCategoryVO {
    private Long id;
    private Long parent_id;
    private String name;
    private Integer sort;
    private List<JobCategoryVO> children;
}
