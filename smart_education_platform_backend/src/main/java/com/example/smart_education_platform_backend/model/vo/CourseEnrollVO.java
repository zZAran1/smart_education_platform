package com.example.smart_education_platform_backend.model.vo;

import lombok.Data;

@Data
public class CourseEnrollVO {
    /** 免费课程是否已直接报名成功 */
    private Boolean enrolled;
    /** 收费课程生成的待支付订单号（免费课程为 null） */
    private String order_no;
}
