package com.example.smart_education_platform_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smart_education_platform_backend.converter.Converter;
import com.example.smart_education_platform_backend.exception.AdminException;
import com.example.smart_education_platform_backend.exception.OrderException;
import com.example.smart_education_platform_backend.mapper.AiInterviewMapper;
import com.example.smart_education_platform_backend.mapper.CourseCommentMapper;
import com.example.smart_education_platform_backend.mapper.CourseEnrollmentMapper;
import com.example.smart_education_platform_backend.mapper.CourseMapper;
import com.example.smart_education_platform_backend.mapper.CourseQaMapper;
import com.example.smart_education_platform_backend.mapper.JobApplicationMapper;
import com.example.smart_education_platform_backend.mapper.JobMapper;
import com.example.smart_education_platform_backend.mapper.OrderInfoMapper;
import com.example.smart_education_platform_backend.mapper.UserMapper;
import com.example.smart_education_platform_backend.model.dto.AdminCourseDTO;
import com.example.smart_education_platform_backend.model.dto.AdminJobDTO;
import com.example.smart_education_platform_backend.model.entity.AiInterview;
import com.example.smart_education_platform_backend.model.entity.Course;
import com.example.smart_education_platform_backend.model.entity.CourseComment;
import com.example.smart_education_platform_backend.model.entity.CourseEnrollment;
import com.example.smart_education_platform_backend.model.entity.CourseQa;
import com.example.smart_education_platform_backend.model.entity.Job;
import com.example.smart_education_platform_backend.model.entity.JobApplication;
import com.example.smart_education_platform_backend.model.entity.OrderInfo;
import com.example.smart_education_platform_backend.model.entity.Users;
import com.example.smart_education_platform_backend.model.vo.UserVO;
import com.example.smart_education_platform_backend.service.AdminService;
import com.example.smart_education_platform_backend.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final String ROLE_ADMIN = "2";
    private static final int USER_STATUS_BANNED = 2;
    private static final int COMMENT_STATUS_HIDDEN = 0;
    private static final int ORDER_STATUS_PAID = 1;
    private static final int ORDER_STATUS_REFUNDED = 2;
    private static final String BANNED_USER_KEY_PREFIX = "edu:user:banned:";

    private final UserMapper userMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final CourseMapper courseMapper;
    private final JobMapper jobMapper;
    private final JobApplicationMapper jobApplicationMapper;
    private final AiInterviewMapper aiInterviewMapper;
    private final CourseCommentMapper courseCommentMapper;
    private final CourseQaMapper courseQaMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void checkRole() {
        if (!ROLE_ADMIN.equals(UserContext.getRoleOrNull())) {
            throw new AdminException("无权限访问");
        }
    }

    @Override
    public Page<UserVO> pageUsers(Integer pageNum, Integer pageSize, String username, Integer status) {
        checkRole();
        Page<Users> page = userMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Users>()
                        .like(StringUtils.hasText(username), Users::getUsername, username)
                        .eq(status != null, Users::getStatus, status)
                        .orderByDesc(Users::getId));
        Page<UserVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(Converter.INSTANCE.toUserVOList(page.getRecords()));
        return voPage;
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        checkRole();
        Users user = userMapper.selectById(userId);
        if (user == null) {
            throw new AdminException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);

        // 封禁即时失效：写/删黑名单标记，拦截器据此拒绝已签发 Token
        if (status != null && status == USER_STATUS_BANNED) {
            redisTemplate.opsForValue().set(BANNED_USER_KEY_PREFIX + userId, "1");
        } else {
            redisTemplate.delete(BANNED_USER_KEY_PREFIX + userId);
        }
    }

    @Override
    public Page<OrderInfo> pageOrders(Integer pageNum, Integer pageSize, Long userId, Long courseId, Integer status) {
        checkRole();
        return orderInfoMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(userId != null, OrderInfo::getUser_id, userId)
                        .eq(courseId != null, OrderInfo::getCourse_id, courseId)
                        .eq(status != null, OrderInfo::getStatus, status)
                        .orderByDesc(OrderInfo::getId));
    }

    @Override
    @Transactional
    public void refundOrder(Long orderId) {
        checkRole();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new OrderException("订单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != ORDER_STATUS_PAID) {
            throw new OrderException("仅已支付订单可退款");
        }
        order.setStatus(ORDER_STATUS_REFUNDED);
        orderInfoMapper.updateById(order);

        // 退款作废报名记录并扣减学习人数
        CourseEnrollment enrollment = courseEnrollmentMapper.selectOne(new LambdaQueryWrapper<CourseEnrollment>()
                .eq(CourseEnrollment::getUser_id, order.getUser_id())
                .eq(CourseEnrollment::getCourse_id, order.getCourse_id()));
        if (enrollment != null) {
            courseEnrollmentMapper.deleteById(enrollment.getId());
            courseMapper.update(null, new LambdaUpdateWrapper<Course>()
                    .eq(Course::getId, order.getCourse_id())
                    .setSql("student_count = student_count - 1"));
        }
    }

    @Override
    public Page<Course> pageCourses(Integer pageNum, Integer pageSize, String keyword, Integer status) {
        checkRole();
        return courseMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Course>()
                        .like(StringUtils.hasText(keyword), Course::getTitle, keyword)
                        .eq(status != null, Course::getStatus, status)
                        .orderByDesc(Course::getId));
    }

    @Override
    public Page<Job> pageJobs(Integer pageNum, Integer pageSize, String keyword, Integer status) {
        checkRole();
        return jobMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Job>()
                        .like(StringUtils.hasText(keyword), Job::getTitle, keyword)
                        .eq(status != null, Job::getStatus, status)
                        .orderByDesc(Job::getId));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "courseList", allEntries = true)
    public Long createCourse(AdminCourseDTO dto) {
        checkRole();
        Course course = Converter.INSTANCE.toCourse(dto);
        course.setStatus(1);
        course.setPublish_time(LocalDateTime.now());
        if (course.getIs_free() == null) course.setIs_free(1);
        if (course.getPrice() == null) course.setPrice(BigDecimal.ZERO);
        if (course.getCourseware_count() == null) course.setCourseware_count(0);
        if (course.getVideo_count() == null) course.setVideo_count(0);
        if (course.getLab_count() == null) course.setLab_count(0);
        if (course.getScore() == null) course.setScore(BigDecimal.ZERO);
        if (course.getRating_count() == null) course.setRating_count(0);
        if (course.getStudent_count() == null) course.setStudent_count(0);
        courseMapper.insert(course);
        return course.getId();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "courseList", allEntries = true),
            @CacheEvict(cacheNames = "courseDetail", key = "#courseId")
    })
    public void updateCourse(Long courseId, AdminCourseDTO dto) {
        checkRole();
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new AdminException("课程不存在");
        }
        Course updated = Converter.INSTANCE.toCourse(dto);
        updated.setId(courseId);
        courseMapper.updateById(updated);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "courseList", allEntries = true),
            @CacheEvict(cacheNames = "courseDetail", key = "#courseId")
    })
    public void updateCourseStatus(Long courseId, Integer status) {
        checkRole();
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new AdminException("课程不存在");
        }
        course.setStatus(status);
        if (status != null && status == 1 && course.getPublish_time() == null) {
            course.setPublish_time(LocalDateTime.now());
        }
        courseMapper.updateById(course);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "jobList", allEntries = true)
    public Long createJob(AdminJobDTO dto) {
        checkRole();
        Job job = Converter.INSTANCE.toJob(dto);
        job.setStatus(1);
        if (job.getHeadcount() == null) job.setHeadcount(1);
        jobMapper.insert(job);
        return job.getId();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "jobList", allEntries = true),
            @CacheEvict(cacheNames = "jobDetail", key = "#jobId")
    })
    public void updateJob(Long jobId, AdminJobDTO dto) {
        checkRole();
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new AdminException("职位不存在");
        }
        Job updated = Converter.INSTANCE.toJob(dto);
        updated.setId(jobId);
        jobMapper.updateById(updated);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "jobList", allEntries = true),
            @CacheEvict(cacheNames = "jobDetail", key = "#jobId")
    })
    public void updateJobStatus(Long jobId, Integer status) {
        checkRole();
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new AdminException("职位不存在");
        }
        job.setStatus(status);
        jobMapper.updateById(job);
    }

    @Override
    public Page<JobApplication> pageApplications(Integer pageNum, Integer pageSize, Integer status) {
        checkRole();
        return jobApplicationMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<JobApplication>()
                        .eq(status != null, JobApplication::getStatus, status)
                        .orderByDesc(JobApplication::getId));
    }

    @Override
    public void updateApplicationStatus(Long applicationId, Integer status) {
        checkRole();
        JobApplication application = jobApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new AdminException("申请记录不存在");
        }
        application.setStatus(status);
        jobApplicationMapper.updateById(application);
    }

    @Override
    public Page<AiInterview> pageInterviews(Integer pageNum, Integer pageSize, Integer status) {
        checkRole();
        return aiInterviewMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<AiInterview>()
                        .eq(status != null, AiInterview::getStatus, status)
                        .orderByDesc(AiInterview::getId));
    }

    @Override
    public void updateInterview(Long interviewId, Integer status) {
        checkRole();
        AiInterview interview = aiInterviewMapper.selectById(interviewId);
        if (interview == null) {
            throw new AdminException("面试记录不存在");
        }
        interview.setStatus(status);
        aiInterviewMapper.updateById(interview);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "courseList", allEntries = true),
            @CacheEvict(cacheNames = "courseDetail", allEntries = true)
    })
    public void hideComment(Long commentId) {
        checkRole();
        CourseComment comment = courseCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new AdminException("评论不存在");
        }
        comment.setStatus(COMMENT_STATUS_HIDDEN);
        courseCommentMapper.updateById(comment);
        recalcCourseScore(comment.getCourse_id());
    }

    @Override
    public void answerQuestion(Long questionId, String answer) {
        checkRole();
        CourseQa qa = courseQaMapper.selectById(questionId);
        if (qa == null) {
            throw new AdminException("问题不存在");
        }
        qa.setAnswer(answer);
        qa.setAnswerer_id(UserContext.getUserId());
        qa.setStatus(1);
        courseQaMapper.updateById(qa);
    }

    private void recalcCourseScore(Long courseId) {
        List<CourseComment> comments = courseCommentMapper.selectList(new LambdaQueryWrapper<CourseComment>()
                .eq(CourseComment::getCourse_id, courseId)
                .eq(CourseComment::getStatus, 1));
        int count = comments.size();
        BigDecimal score = BigDecimal.ZERO;
        if (count > 0) {
            BigDecimal sum = comments.stream()
                    .map(CourseComment::getScore)
                    .map(BigDecimal::valueOf)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            score = sum.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
        }
        courseMapper.update(null, new LambdaUpdateWrapper<Course>()
                .eq(Course::getId, courseId)
                .set(Course::getScore, score)
                .set(Course::getRating_count, count));
    }
}
