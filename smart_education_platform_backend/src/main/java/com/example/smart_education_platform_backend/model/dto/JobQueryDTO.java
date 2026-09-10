package com.example.smart_education_platform_backend.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JobQueryDTO {
    private Long category_id;

    /** 搜索类型：job 按职位搜索 / company 按公司搜索 */
    private String search_type;

    @Size(max = 50, message = "搜索关键词不能超过50字")
    private String keyword;

    @Min(value = 1, message = "页码最小为1")
    private Integer page_num = 1;

    @Min(value = 1, message = "每页条数最小为1")
    private Integer page_size = 10;
}
