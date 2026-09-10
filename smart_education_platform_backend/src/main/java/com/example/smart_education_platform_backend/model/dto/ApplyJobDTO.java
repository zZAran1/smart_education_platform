package com.example.smart_education_platform_backend.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApplyJobDTO {
    @Size(max = 255, message = "简历地址长度不能超过255位")
    private String resume_url;
}
