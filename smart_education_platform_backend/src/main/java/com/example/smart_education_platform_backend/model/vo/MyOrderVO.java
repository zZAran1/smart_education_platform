package com.example.smart_education_platform_backend.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学员「我的订单」视图：订单记录 + 课程标题。
 * 课程被删除时仅保留 course_id，课程标题留空。
 */
@Data
public class MyOrderVO {

    private Long id;
    private String order_no;
    private Long course_id;
    private String course_title;
    private BigDecimal amount;
    /** 支付方式：0支付宝 1微信（仅作记录） */
    private Integer pay_type;
    /** 订单状态：0待支付 1已支付 2已退款 3已取消 */
    private Integer status;
    /** 下单时间 */
    private LocalDateTime created_at;
    private LocalDateTime pay_time;
}
