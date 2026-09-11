package com.example.smart_education_platform_backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_info")
public class OrderInfo {
    @TableId
    private Long id;
    private String order_no;
    private Long user_id;
    private Long course_id;
    private BigDecimal amount;
    private Integer pay_type;
    private Integer status;
    private String trade_no;
    private LocalDateTime pay_time;
    private LocalDateTime notify_time;
    private Integer deleted;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
