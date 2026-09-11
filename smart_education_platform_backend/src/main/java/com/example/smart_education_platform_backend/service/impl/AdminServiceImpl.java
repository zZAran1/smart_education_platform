package com.example.smart_education_platform_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smart_education_platform_backend.converter.Converter;
import com.example.smart_education_platform_backend.exception.AdminException;
import com.example.smart_education_platform_backend.exception.OrderException;
import com.example.smart_education_platform_backend.mapper.AiInterviewMapper;
import com.example.smart_education_platform_backend.mapper.CompanyMapper;
import com.example.smart_education_platform_backend.mapper.CourseChapterMapper;
import com.example.smart_education_platform_backend.mapper.CourseCommentMapper;
import com.example.smart_education_platform_backend.mapper.CourseEnrollmentMapper;
import com.example.smart_education_platform_backend.mapper.CourseMapper;
import com.example.smart_education_platform_backend.mapper.CourseQaMapper;
import com.example.smart_education_platform_backend.mapper.JobApplicationMapper;
import com.example.smart_education_platform_backend.mapper.JobMapper;
import com.example.smart_education_platform_backend.mapper.OrderInfoMapper;
import com.example.smart_education_platform_backend.mapper.StudyRecordMapper;
import com.example.smart_education_platform_backend.mapper.UserMapper;
import com.example.smart_education_platform_backend.model.dto.AdminChapterDTO;
import com.example.smart_education_platform_backend.model.dto.AdminCompanyDTO;
import com.example.smart_education_platform_backend.model.dto.AdminCourseDTO;
import com.example.smart_education_platform_backend.model.dto.AdminJobDTO;
import com.example.smart_education_platform_backend.model.entity.AiInterview;
import com.example.smart_education_platform_backend.model.entity.Company;
import com.example.smart_education_platform_backend.model.entity.Course;
import com.example.smart_education_platform_backend.model.entity.CourseChapter;
import com.example.smart_education_platform_backend.model.entity.CourseComment;
import com.example.smart_education_platform_backend.model.entity.CourseEnrollment;
import com.example.smart_education_platform_backend.model.entity.CourseQa;
import com.example.smart_education_platform_backend.model.entity.Job;
import com.example.smart_education_platform_backend.model.entity.JobApplication;
import com.example.smart_education_platform_backend.model.entity.OrderInfo;
import com.example.smart_education_platform_backend.model.entity.StudyRecord;
import com.example.smart_education_platform_backend.model.entity.Users;
import com.example.smart_education_platform_backend.model.vo.AdminCommentVO;
import com.example.smart_education_platform_backend.model.vo.AdminQuestionVO;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final String ROLE_ADMIN = "2";
    private static final int USER_STATUS_BANNED = 2;
    /** 封禁标记保留时长：避免无过期 key 永久残留（DB 的 status 才是权威判据） */
    private static final Duration BANNED_USER_TTL = Duration.ofDays(7);
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
    private final CourseChapterMapper courseChapterMapper;
    private final CompanyMapper companyMapper;
    private final StudyRecordMapper studyRecordMapper;
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
        Page<Users> page = userMapper.selectPage(new Page<>(pageNumOr(pageNum), pageSizeOr(pageSize)),
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
            redisTemplate.opsForValue().set(BANNED_USER_KEY_PREFIX + userId, "1", BANNED_USER_TTL);
        } else {
            redisTemplate.delete(BANNED_USER_KEY_PREFIX + userId);
        }
    }

    @Override
    public Page<OrderInfo> pageOrders(Integer pageNum, Integer pageSize, Long userId, Long courseId, Integer status) {
        checkRole();
        return orderInfoMapper.selectPage(new Page<>(pageNumOr(pageNum), pageSizeOr(pageSize)),
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(userId != null, OrderInfo::getUser_id, userId)
                        .eq(courseId != null, OrderInfo::getCourse_id, courseId)
                        .eq(status != null, OrderInfo::getStatus, status)
                        .orderByDesc(OrderInfo::getId));
    }

    @Override
    @Transactional
    @Caching(evict = {
            // 退款会扣减 student_count 并作废报名记录，需同步失效课程缓存
            @CacheEvict(cacheNames = "courseList", allEntries = true),
            @CacheEvict(cacheNames = "courseDetail", allEntries = true)
    })
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
        return courseMapper.selectPage(new Page<>(pageNumOr(pageNum), pageSizeOr(pageSize)),
                new LambdaQueryWrapper<Course>()
                        .like(StringUtils.hasText(keyword), Course::getTitle, keyword)
                        .eq(status != null, Course::getStatus, status)
                        .orderByDesc(Course::getId));
    }

    @Override
    public Page<Job> pageJobs(Integer pageNum, Integer pageSize, String keyword, Integer status) {
        checkRole();
        return jobMapper.selectPage(new Page<>(pageNumOr(pageNum), pageSizeOr(pageSize)),
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
        requireCompany(dto.getCompany_id());
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
        requireCompany(dto.getCompany_id());
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
        return jobApplicationMapper.selectPage(new Page<>(pageNumOr(pageNum), pageSizeOr(pageSize)),
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
        return aiInterviewMapper.selectPage(new Page<>(pageNumOr(pageNum), pageSizeOr(pageSize)),
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
    public Page<AdminCommentVO> pageComments(Integer pageNum, Integer pageSize, Integer status) {
        checkRole();
        Page<CourseComment> page = courseCommentMapper.selectPage(new Page<>(pageNumOr(pageNum), pageSizeOr(pageSize)),
                new LambdaQueryWrapper<CourseComment>()
                        .eq(status != null, CourseComment::getStatus, status)
                        .orderByDesc(CourseComment::getId));
        List<AdminCommentVO> records = Converter.INSTANCE.toAdminCommentVOList(page.getRecords());
        fillCommentExtra(records);
        Page<AdminCommentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public Page<AdminQuestionVO> pageQuestions(Integer pageNum, Integer pageSize, Integer status) {
        checkRole();
        Page<CourseQa> page = courseQaMapper.selectPage(new Page<>(pageNumOr(pageNum), pageSizeOr(pageSize)),
                new LambdaQueryWrapper<CourseQa>()
                        .eq(status != null, CourseQa::getStatus, status)
                        .orderByDesc(CourseQa::getId));
        List<AdminQuestionVO> records = Converter.INSTANCE.toAdminQuestionVOList(page.getRecords());
        fillQuestionExtra(records);
        Page<AdminQuestionVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return voPage;
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

    /* ---------- 章节目录 ---------- */

    @Override
    public List<CourseChapter> listChapters(Long courseId) {
        checkRole();
        requireCourse(courseId);
        return courseChapterMapper.selectList(new LambdaQueryWrapper<CourseChapter>()
                .eq(CourseChapter::getCourse_id, courseId)
                .orderByAsc(CourseChapter::getSort));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "courseList", allEntries = true),
            @CacheEvict(cacheNames = "courseDetail", allEntries = true)
    })
    public Long createChapter(Long courseId, AdminChapterDTO dto) {
        checkRole();
        requireCourse(courseId);
        CourseChapter chapter = new CourseChapter();
        chapter.setCourse_id(courseId);
        applyChapter(chapter, dto);
        courseChapterMapper.insert(chapter);
        recalcCourseCounts(courseId);
        recalcEnrollmentProgress(courseId);
        return chapter.getId();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "courseList", allEntries = true),
            @CacheEvict(cacheNames = "courseDetail", allEntries = true)
    })
    public void updateChapter(Long chapterId, AdminChapterDTO dto) {
        checkRole();
        CourseChapter chapter = courseChapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new AdminException("章节不存在");
        }
        applyChapter(chapter, dto);
        courseChapterMapper.updateById(chapter);
        recalcCourseCounts(chapter.getCourse_id());
        recalcEnrollmentProgress(chapter.getCourse_id());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "courseList", allEntries = true),
            @CacheEvict(cacheNames = "courseDetail", allEntries = true)
    })
    public void deleteChapter(Long chapterId) {
        checkRole();
        CourseChapter chapter = courseChapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new AdminException("章节不存在");
        }
        courseChapterMapper.deleteById(chapterId);
        recalcCourseCounts(chapter.getCourse_id());
        recalcEnrollmentProgress(chapter.getCourse_id());
    }

    /* ---------- 公司 ---------- */

    @Override
    public Page<Company> pageCompanies(Integer pageNum, Integer pageSize, String keyword) {
        checkRole();
        return companyMapper.selectPage(new Page<>(pageNumOr(pageNum), pageSizeOr(pageSize)),
                new LambdaQueryWrapper<Company>()
                        .like(StringUtils.hasText(keyword), Company::getName, keyword)
                        .orderByDesc(Company::getId));
    }

    @Override
    public Long createCompany(AdminCompanyDTO dto) {
        checkRole();
        Company company = new Company();
        applyCompany(company, dto);
        companyMapper.insert(company);
        return company.getId();
    }

    @Override
    @Caching(evict = {
            // 公司信息内嵌在职位列表/详情缓存中，变更后必须同步失效
            @CacheEvict(cacheNames = "jobList", allEntries = true),
            @CacheEvict(cacheNames = "jobDetail", allEntries = true)
    })
    public void updateCompany(Long companyId, AdminCompanyDTO dto) {
        checkRole();
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new AdminException("公司不存在");
        }
        applyCompany(company, dto);
        companyMapper.updateById(company);
    }

    @Override
    public void deleteCompany(Long companyId) {
        checkRole();
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new AdminException("公司不存在");
        }
        // 公司被职位引用时不允许删除，避免出现孤儿职位
        Long used = jobMapper.selectCount(new LambdaQueryWrapper<Job>()
                .eq(Job::getCompany_id, companyId));
        if (used != null && used > 0) {
            throw new AdminException("该公司下仍有职位，请先处理相关职位");
        }
        companyMapper.deleteById(companyId);
    }

    /* ---------- 内部辅助方法 ---------- */

    private void requireCourse(Long courseId) {
        if (courseMapper.selectById(courseId) == null) {
            throw new AdminException("课程不存在");
        }
    }

    private void requireCompany(Long companyId) {
        if (companyId == null || companyMapper.selectById(companyId) == null) {
            throw new AdminException("所选公司不存在");
        }
    }

    /** 分页参数兜底：空串参数会被绑定为 null，直接传入 Page 会因自动拆箱抛 NPE */
    private static long pageNumOr(Integer value) {
        return value == null || value < 1 ? 1L : value;
    }

    /** 同上，并限制每页上限，避免超大 page_size 造成慢查询 */
    private static long pageSizeOr(Integer value) {
        return value == null || value < 1 ? 10L : Math.min(value, 100);
    }

    private void applyChapter(CourseChapter chapter, AdminChapterDTO dto) {
        chapter.setTitle(dto.getTitle());
        chapter.setResource_type(dto.getResource_type());
        chapter.setDuration(dto.getDuration() == null ? 0 : dto.getDuration());
        chapter.setSort(dto.getSort() == null ? 0 : dto.getSort());
    }

    private void applyCompany(Company company, AdminCompanyDTO dto) {
        company.setName(dto.getName());
        company.setLogo(dto.getLogo());
        company.setIndustry(dto.getIndustry());
        company.setScale(dto.getScale());
        company.setRegion(dto.getRegion());
        company.setIntro(dto.getIntro());
    }

    /** 章节变更后重算课程的课件/视频/实验数量，保持列表卡片统计一致 */
    private void recalcCourseCounts(Long courseId) {
        courseMapper.update(null, new LambdaUpdateWrapper<Course>()
                .eq(Course::getId, courseId)
                .set(Course::getCourseware_count, countChapterByType(courseId, 0))
                .set(Course::getVideo_count, countChapterByType(courseId, 1))
                .set(Course::getLab_count, countChapterByType(courseId, 2)));
    }

    private int countChapterByType(Long courseId, int resourceType) {
        Long count = courseChapterMapper.selectCount(new LambdaQueryWrapper<CourseChapter>()
                .eq(CourseChapter::getCourse_id, courseId)
                .eq(CourseChapter::getResource_type, resourceType));
        return count == null ? 0 : count.intValue();
    }

    /**
     * 章节变更后重算该课程所有报名记录的学习进度。
     * 只统计「现存章节」对应的学习记录，避免章节被删除后残留的 study_record
     * 导致 finish_count 偏大、progress 超过 100%。
     */
    private void recalcEnrollmentProgress(Long courseId) {
        List<Long> chapterIds = courseChapterMapper.selectList(new LambdaQueryWrapper<CourseChapter>()
                        .eq(CourseChapter::getCourse_id, courseId))
                .stream()
                .map(CourseChapter::getId)
                .toList();
        int total = chapterIds.size();

        List<CourseEnrollment> enrollments = courseEnrollmentMapper.selectList(
                new LambdaQueryWrapper<CourseEnrollment>().eq(CourseEnrollment::getCourse_id, courseId));
        if (enrollments.isEmpty()) {
            return;
        }

        Map<Long, Long> finishMap = chapterIds.isEmpty()
                ? Map.of()
                : studyRecordMapper.selectList(new LambdaQueryWrapper<StudyRecord>()
                                .eq(StudyRecord::getCourse_id, courseId)
                                .in(StudyRecord::getChapter_id, chapterIds))
                        .stream()
                        .collect(Collectors.groupingBy(StudyRecord::getUser_id, Collectors.counting()));

        for (CourseEnrollment enrollment : enrollments) {
            int finish = finishMap.getOrDefault(enrollment.getUser_id(), 0L).intValue();
            int progress = total == 0 ? 0 : (int) Math.round((double) finish / total * 100);
            courseEnrollmentMapper.update(null, new LambdaUpdateWrapper<CourseEnrollment>()
                    .eq(CourseEnrollment::getId, enrollment.getId())
                    .set(CourseEnrollment::getFinish_count, finish)
                    .set(CourseEnrollment::getTotal_count, total)
                    .set(CourseEnrollment::getProgress, progress));
        }
    }

    private void fillCommentExtra(List<AdminCommentVO> records) {
        if (records.isEmpty()) {
            return;
        }
        Map<Long, String> titles = courseTitleMap(records.stream()
                .map(AdminCommentVO::getCourse_id).collect(Collectors.toSet()));
        Map<Long, String> nicknames = nicknameMap(records.stream()
                .map(AdminCommentVO::getUser_id).collect(Collectors.toSet()));
        records.forEach(vo -> {
            vo.setCourse_title(titles.get(vo.getCourse_id()));
            vo.setNickname(nicknames.get(vo.getUser_id()));
        });
    }

    private void fillQuestionExtra(List<AdminQuestionVO> records) {
        if (records.isEmpty()) {
            return;
        }
        Map<Long, String> titles = courseTitleMap(records.stream()
                .map(AdminQuestionVO::getCourse_id).collect(Collectors.toSet()));
        Map<Long, String> nicknames = nicknameMap(records.stream()
                .map(AdminQuestionVO::getUser_id).collect(Collectors.toSet()));
        records.forEach(vo -> {
            vo.setCourse_title(titles.get(vo.getCourse_id()));
            vo.setNickname(nicknames.get(vo.getUser_id()));
        });
    }

    private Map<Long, String> courseTitleMap(Set<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        return courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Course::getTitle, (a, b) -> a));
    }

    private Map<Long, String> nicknameMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(Users::getId,
                        u -> u.getNickname() == null ? "" : u.getNickname(), (a, b) -> a));
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
