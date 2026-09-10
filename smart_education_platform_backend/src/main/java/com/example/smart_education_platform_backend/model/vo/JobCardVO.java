package com.example.smart_education_platform_backend.model.vo;

import lombok.Data;

@Data
public class JobCardVO {
    private Long id;
    private String title;
    private String city;
    private String address;
    private Integer salary_min;
    private Integer salary_max;
    private Integer headcount;
    private Long company_id;
    private Long category_id;
    /** 公司信息（查询时组装） */
    private String company_name;
    private String company_logo;
    private String company_industry;
    private String company_scale;
    private String company_region;
}
