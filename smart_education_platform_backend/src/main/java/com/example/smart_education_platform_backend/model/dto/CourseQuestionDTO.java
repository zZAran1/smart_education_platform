package com.example.smart_education_platform_backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseQuestionDTO {
    @NotBlank(message = "问题内容不能为空")
    @Size(max = 500, message = "问题内容不能超过500字")
    private String question;
}
