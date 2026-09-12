package com.example.smart_education_platform_backend.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学员「我的数字人面试」视图：面试记录 + 职位与公司展示字段。
 * 职位被删除时仅保留 job_id 与面试信息，其余字段留空。
 */
@Data
public class MyInterviewVO {

    /** 面试记录 ID */
    private Long id;
    private Long job_id;
    private String job_title;
    private Long company_id;
    private String company_name;
    /** 关联的投递记录 ID */
    private Long application_id;
    /** 面试状态：0待进行 1进行中 2已完成 */
    private Integer status;
    private LocalDateTime interview_time;
    /** 面试报告地址 */
    private String report;
    /** 申请时间 */
    private LocalDateTime created_at;
}
