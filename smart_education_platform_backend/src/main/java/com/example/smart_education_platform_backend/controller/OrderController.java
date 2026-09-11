package com.example.smart_education_platform_backend.controller;

import com.example.smart_education_platform_backend.model.dto.PayOrderDTO;
import com.example.smart_education_platform_backend.result.Result;
import com.example.smart_education_platform_backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 支付订单：直接完成支付并开通课程（课程设计简化，跳过真实支付流程） */
    @PostMapping("/{orderNo}/pay")
    public Result<Void> pay(@PathVariable String orderNo,
                            @RequestBody(required = false) PayOrderDTO dto) {
        orderService.pay(orderNo, dto == null ? null : dto.getPay_type());
        return Result.success("支付成功");
    }
}
