package com.example.smart_education_platform_backend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 重置密码验证码下发结果。
 * 注：当前未接入短信/邮件网关，code 直接回传用于开发联调，生产环境应改为真实下发渠道。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetCodeVO {
    private String code;
}
