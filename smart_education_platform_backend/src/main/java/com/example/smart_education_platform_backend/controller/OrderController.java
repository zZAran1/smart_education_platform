package com.example.smart_education_platform_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smart_education_platform_backend.model.dto.PayOrderDTO;
import com.example.smart_education_platform_backend.model.vo.MyOrderVO;
import com.example.smart_education_platform_backend.result.Result;
import com.example.smart_education_platform_backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 支付订单：直接完成支付并开通课程（课程设计简化，跳过真实支付流程） */
    @PostMapping("/{orderNo}/pay")
    public Result<Void> pay(@PathVariable String orderNo,
                            @Valid @RequestBody(required = false) PayOrderDTO dto) {
        orderService.pay(orderNo, dto == null ? null : dto.getPay_type());
        return Result.success("支付成功");
    }

    /** 我的订单：当前登录用户的订单记录 + 课程标题 */
    @GetMapping("/my")
    public Result<Page<MyOrderVO>> my(@RequestParam(defaultValue = "1") Integer page_num,
                                      @RequestParam(defaultValue = "10") Integer page_size,
                                      @RequestParam(required = false) Integer status) {
        return Result.success(orderService.pageMyOrders(page_num, page_size, status));
    }
}
