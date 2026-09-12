package com.example.smart_education_platform_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smart_education_platform_backend.exception.OrderException;
import com.example.smart_education_platform_backend.mapper.CourseChapterMapper;
import com.example.smart_education_platform_backend.mapper.CourseEnrollmentMapper;
import com.example.smart_education_platform_backend.mapper.CourseMapper;
import com.example.smart_education_platform_backend.mapper.OrderInfoMapper;
import com.example.smart_education_platform_backend.model.entity.Course;
import com.example.smart_education_platform_backend.model.entity.CourseChapter;
import com.example.smart_education_platform_backend.model.entity.CourseEnrollment;
import com.example.smart_education_platform_backend.model.entity.OrderInfo;
import com.example.smart_education_platform_backend.model.vo.MyOrderVO;
import com.example.smart_education_platform_backend.service.OrderService;
import com.example.smart_education_platform_backend.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final int ORDER_STATUS_PENDING = 0;
    private static final int ORDER_STATUS_PAID = 1;
    /** 订单支付超时时间（分钟），与定时任务取消阈值保持一致 */
    private static final int PAY_TIMEOUT_MINUTES = 30;

    private final OrderInfoMapper orderInfoMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private final CourseMapper courseMapper;
    private final CourseChapterMapper courseChapterMapper;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "courseList", allEntries = true),
            @CacheEvict(cacheNames = "courseDetail", allEntries = true)
    })
    public void pay(String orderNo, Integer payType) {
        OrderInfo order = getOwnOrder(orderNo);
        if (order.getStatus() == null || order.getStatus() != ORDER_STATUS_PENDING) {
            throw new OrderException("订单状态异常，无法支付");
        }
        assertNotExpired(order);

        // 课程设计简化：不接入真实支付渠道，点击支付即视为支付成功。
        // 条件更新保证并发或重复点击时只有一个请求能完成入账。
        int updated = orderInfoMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
                .eq(OrderInfo::getId, order.getId())
                .eq(OrderInfo::getStatus, ORDER_STATUS_PENDING)
                .set(OrderInfo::getStatus, ORDER_STATUS_PAID)
                .set(payType != null, OrderInfo::getPay_type, payType)
                .set(OrderInfo::getPay_time, LocalDateTime.now()));
        if (updated == 0) {
            // 已被其它请求处理，幂等返回
            return;
        }

        Long userId = order.getUser_id();
        Long courseId = order.getCourse_id();
        // 清除历史残留（含退款作废的逻辑删除行），避免唯一键冲突
        courseEnrollmentMapper.deleteByUserAndCourse(userId, courseId);

        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setUser_id(userId);
        enrollment.setCourse_id(courseId);
        enrollment.setOrder_id(order.getId());
        enrollment.setProgress(0);
        enrollment.setFinish_count(0);
        // 分母按课程当前章节数初始化，否则刚开通未学习的课程会显示成 0/0
        enrollment.setTotal_count(countChapters(courseId));
        courseEnrollmentMapper.insert(enrollment);

        courseMapper.update(null, new LambdaUpdateWrapper<Course>()
                .eq(Course::getId, courseId)
                .setSql("student_count = student_count + 1"));
    }

    @Override
    public Page<MyOrderVO> pageMyOrders(Integer pageNum, Integer pageSize, Integer status) {
        Long userId = UserContext.getUserId();
        Page<OrderInfo> page = orderInfoMapper.selectPage(
                new Page<>(pageNumOr(pageNum), pageSizeOr(pageSize)),
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(OrderInfo::getUser_id, userId)
                        .eq(status != null, OrderInfo::getStatus, status)
                        .orderByDesc(OrderInfo::getId));

        Set<Long> courseIds = page.getRecords().stream()
                .map(OrderInfo::getCourse_id)
                .collect(Collectors.toSet());
        // 课程被删除时查不到，课程标题留空，但订单记录仍然返回
        Map<Long, Course> courseMap = courseIds.isEmpty() ? Map.of()
                : courseMapper.selectBatchIds(courseIds).stream()
                        .collect(Collectors.toMap(Course::getId, course -> course, (a, b) -> a));

        List<MyOrderVO> records = page.getRecords().stream().map(order -> {
            MyOrderVO vo = new MyOrderVO();
            vo.setId(order.getId());
            vo.setOrder_no(order.getOrder_no());
            vo.setCourse_id(order.getCourse_id());
            vo.setAmount(order.getAmount());
            vo.setPay_type(order.getPay_type());
            vo.setStatus(order.getStatus());
            vo.setCreated_at(order.getCreated_at());
            vo.setPay_time(order.getPay_time());

            Course course = courseMap.get(order.getCourse_id());
            if (course != null) {
                vo.setCourse_title(course.getTitle());
            }
            return vo;
        }).toList();

        Page<MyOrderVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    /** 分页参数兜底：空串参数会被绑定为 null，直接传入 Page 会因自动拆箱抛 NPE */
    private static long pageNumOr(Integer value) {
        return value == null || value < 1 ? 1L : value;
    }

    /** 同上，并限制每页上限，避免超大 page_size 造成慢查询 */
    private static long pageSizeOr(Integer value) {
        return value == null || value < 1 ? 10L : Math.min(value, 100);
    }

    /** 课程当前章节总数：作为学习进度的分母，开通课程时即初始化，避免进度显示成 0/0 */
    private int countChapters(Long courseId) {
        Long total = courseChapterMapper.selectCount(new LambdaQueryWrapper<CourseChapter>()
                .eq(CourseChapter::getCourse_id, courseId));
        return total == null ? 0 : total.intValue();
    }

    /** 查询属于当前登录用户的订单，避免越权操作他人订单 */
    private OrderInfo getOwnOrder(String orderNo) {
        Long userId = UserContext.getUserId();
        OrderInfo order = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getOrder_no, orderNo)
                .eq(OrderInfo::getUser_id, userId));
        if (order == null) {
            throw new OrderException("订单不存在");
        }
        return order;
    }

    /** 支付时效校验：超时订单不允许再支付（定时任务也会将其取消） */
    private void assertNotExpired(OrderInfo order) {
        if (order.getCreated_at() != null
                && order.getCreated_at().plusMinutes(PAY_TIMEOUT_MINUTES).isBefore(LocalDateTime.now())) {
            throw new OrderException("订单已超时，请重新下单");
        }
    }
}
