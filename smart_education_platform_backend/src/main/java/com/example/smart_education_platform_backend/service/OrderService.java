package com.example.smart_education_platform_backend.service;

/**
 * 订单支付。
 * 课程设计简化实现：不接入真实支付渠道，用户点击支付即视为支付成功并开通课程。
 */
public interface OrderService {

    /** 支付订单：把待支付订单置为已支付，并开通课程、累加学习人数（幂等） */
    void pay(String orderNo, Integer payType);
}
