package com.example.smart_education_platform_backend.model.vo;

import lombok.Data;

@Data
public class JobDetailVO {
    private Long id;
    private String title;
    private String city;
    private String address;
    private Integer salary_min;
    private Integer salary_max;
    private Integer headcount;
    private Long company_id;
    private Long category_id;
    private String description;
    private String requirement;
    /** 公司基本信息（查询时组装） */
    private String company_name;
    private String company_logo;
    private String company_industry;
    private String company_scale;
    private String company_region;
    private String company_intro;
    /** 当前用户是否已收藏（登录增强字段，游客恒为 false） */
    private Boolean is_collected = false;
    /** 当前用户是否已投递（登录增强字段，游客恒为 false） */
    private Boolean is_applied = false;
}
