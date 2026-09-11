package com.example.smart_education_platform_backend.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseQueryDTO {
    @NotNull(message = "课程类型不能为空")
    private Integer type;

    private Long tech_system_id;
    private Long tech_direction_id;
    private Integer level;
    private Integer is_free;

    @Size(max = 50, message = "搜索关键词不能超过50字")
    private String keyword;

    /** 排序字段：publish_time / student_count / score，默认 publish_time 倒序 */
    private String sort_by;

    @Min(value = 1, message = "页码最小为1")
    private Integer page_num = 1;

    @Min(value = 1, message = "每页条数最小为1")
    private Integer page_size = 10;

    /** 空串参数会被绑定为 null，此处兜底避免 Page 自动拆箱抛 NPE */
    public void setPage_num(Integer page_num) {
        this.page_num = (page_num == null || page_num < 1) ? 1 : page_num;
    }

    /** 同上，并对每页条数设置上限，避免超大 page_size 造成慢查询 */
    public void setPage_size(Integer page_size) {
        this.page_size = (page_size == null || page_size < 1) ? 10 : Math.min(page_size, 100);
    }
}
