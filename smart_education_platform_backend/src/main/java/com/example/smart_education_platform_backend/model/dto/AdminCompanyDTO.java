package com.example.smart_education_platform_backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminCompanyDTO {

    @NotBlank(message = "公司名称不能为空")
    @Size(max = 100, message = "公司名称不能超过100字")
    private String name;

    @Size(max = 255, message = "LOGO 地址不能超过255位")
    private String logo;

    @Size(max = 50, message = "所属行业不能超过50字")
    private String industry;

    @Size(max = 50, message = "公司规模不能超过50字")
    private String scale;

    @Size(max = 50, message = "所在地区不能超过50字")
    private String region;

    private String intro;
}
