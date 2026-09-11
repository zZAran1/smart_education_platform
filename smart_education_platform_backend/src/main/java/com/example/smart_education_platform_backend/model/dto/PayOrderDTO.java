package com.example.smart_education_platform_backend.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PayOrderDTO {

    /** 支付方式：0 支付宝 1 微信（仅作记录，不参与真实支付） */
    @Min(value = 0, message = "支付方式不合法")
    @Max(value = 1, message = "支付方式不合法")
    private Integer pay_type;
}
