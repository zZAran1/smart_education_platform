package com.example.smart_education_platform_backend.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileDTO {
    @Size(max = 32, message = "昵称长度不能超过32位")
    private String nickname;

    @Size(max = 32, message = "姓名长度不能超过32位")
    private String real_name;
}
