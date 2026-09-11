package com.example.smart_education_platform_backend.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JobQueryDTO {
    private Long category_id;

    /** 搜索类型：job 按职位搜索 / company 按公司搜索 */
    private String search_type;

    @Size(max = 50, message = "搜索关键词不能超过50字")
    private String keyword;

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
