package com.example.smart_education_platform_backend.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学员「我的投递」视图：投递记录 + 职位与公司展示字段。
 * 职位被删除时仅保留 job_id 与投递信息，其余字段留空。
 */
@Data
public class MyJobApplicationVO {

    /** 投递记录 ID */
    private Long id;
    private Long job_id;
    private String job_title;
    private String city;
    private Integer salary_min;
    private Integer salary_max;
    private Long company_id;
    private String company_name;
    /** 投递状态：0待处理 1已查看 2通过 3拒绝 */
    private Integer status;
    /** 投递时间 */
    private LocalDateTime created_at;
}
