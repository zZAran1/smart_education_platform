package com.example.smart_education_platform_backend.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminChapterDTO {

    @NotBlank(message = "资源标题不能为空")
    @Size(max = 100, message = "资源标题不能超过100字")
    private String title;

    /** 0 课件 1 视频 2 实验 */
    @NotNull(message = "资源类型不能为空")
    @Min(value = 0, message = "资源类型不合法")
    @Max(value = 2, message = "资源类型不合法")
    private Integer resource_type;

    /** 时长（秒） */
    @Min(value = 0, message = "时长不能为负")
    private Integer duration = 0;

    @Min(value = 0, message = "排序号不能为负")
    private Integer sort = 0;
}
