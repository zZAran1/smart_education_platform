package com.example.smart_education_platform_backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminJobDTO {
    @NotBlank(message = "职位名称不能为空")
    @Size(max = 100, message = "职位名称不能超过100字")
    private String title;

    private String city;
    private String address;
    private Integer salary_min;
    private Integer salary_max;
    private Integer headcount;

    @NotNull(message = "公司不能为空")
    private Long company_id;

    private Long category_id;

    /** 职位到期下架时间，null 表示长期有效 */
    private LocalDateTime expire_time;

    private String description;
    private String requirement;
}
