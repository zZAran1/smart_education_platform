package com.example.smart_education_platform_backend.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.smart_education_platform_backend.mapper.JobMapper;
import com.example.smart_education_platform_backend.mapper.OrderInfoMapper;
import com.example.smart_education_platform_backend.model.entity.Job;
import com.example.smart_education_platform_backend.model.entity.OrderInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 平台定时任务：
 * 1. 超时未支付订单自动取消（每 60 秒扫描一次，超时阈值 30 分钟）；
 * 2. 到期职位自动下架（每日 0 点）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformScheduledTask {

    /** 订单支付超时阈值（分钟） */
    private static final int ORDER_TIMEOUT_MINUTES = 30;

    private static final int ORDER_STATUS_PENDING = 0;
    private static final int ORDER_STATUS_CANCELED = 3;
    private static final int JOB_STATUS_ON_SHELF = 1;
    private static final int JOB_STATUS_OFF_SHELF = 0;

    private final OrderInfoMapper orderInfoMapper;
    private final JobMapper jobMapper;
    private final CacheManager cacheManager;

    /** 每 60 秒扫描一次，取消超过 30 分钟未支付的订单 */
    @Scheduled(fixedDelay = 60_000)
    public void cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(ORDER_TIMEOUT_MINUTES);
        int rows = orderInfoMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
                .eq(OrderInfo::getStatus, ORDER_STATUS_PENDING)
                .lt(OrderInfo::getCreated_at, deadline)
                .set(OrderInfo::getStatus, ORDER_STATUS_CANCELED));
        if (rows > 0) {
            log.info("定时任务：取消超时未支付订单 {} 笔", rows);
        }
    }

    /** 每日 0 点，将已到期的在架职位自动下架 */
    @Scheduled(cron = "0 0 0 * * ?")
    public void offShelfExpiredJobs() {
        List<Job> expiredJobs = jobMapper.selectList(new LambdaQueryWrapper<Job>()
                .eq(Job::getStatus, JOB_STATUS_ON_SHELF)
                .isNotNull(Job::getExpire_time)
                .lt(Job::getExpire_time, LocalDateTime.now()));
        if (expiredJobs.isEmpty()) {
            return;
        }
        List<Long> ids = expiredJobs.stream().map(Job::getId).toList();
        jobMapper.update(null, new LambdaUpdateWrapper<Job>()
                .in(Job::getId, ids)
                .set(Job::getStatus, JOB_STATUS_OFF_SHELF));

        // 同步失效列表/详情缓存
        var jobListCache = cacheManager.getCache("jobList");
        if (jobListCache != null) {
            jobListCache.clear();
        }
        var jobDetailCache = cacheManager.getCache("jobDetail");
        if (jobDetailCache != null) {
            ids.forEach(jobDetailCache::evict);
        }
        log.info("定时任务：自动下架到期职位 {} 个", ids.size());
    }
}
