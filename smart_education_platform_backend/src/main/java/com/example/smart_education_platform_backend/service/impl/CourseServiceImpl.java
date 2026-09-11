package com.example.smart_education_platform_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.smart_education_platform_backend.converter.Converter;
import com.example.smart_education_platform_backend.exception.CourseException;
import com.example.smart_education_platform_backend.mapper.CourseCategoryMapper;
import com.example.smart_education_platform_backend.mapper.CourseChapterMapper;
import com.example.smart_education_platform_backend.mapper.CourseCollectionMapper;
import com.example.smart_education_platform_backend.mapper.CourseCommentMapper;
import com.example.smart_education_platform_backend.mapper.CourseEnrollmentMapper;
import com.example.smart_education_platform_backend.mapper.CourseMapper;
import com.example.smart_education_platform_backend.mapper.CourseQaMapper;
import com.example.smart_education_platform_backend.mapper.OrderInfoMapper;
import com.example.smart_education_platform_backend.mapper.StudyRecordMapper;
import com.example.smart_education_platform_backend.mapper.UserMapper;
import com.example.smart_education_platform_backend.model.dto.CourseCommentDTO;
import com.example.smart_education_platform_backend.model.dto.CourseQueryDTO;
import com.example.smart_education_platform_backend.model.dto.CourseQuestionDTO;
import com.example.smart_education_platform_backend.model.entity.Course;
import com.example.smart_education_platform_backend.model.entity.CourseCategory;
import com.example.smart_education_platform_backend.model.entity.CourseChapter;
import com.example.smart_education_platform_backend.model.entity.CourseCollection;
import com.example.smart_education_platform_backend.model.entity.CourseComment;
import com.example.smart_education_platform_backend.model.entity.CourseEnrollment;
import com.example.smart_education_platform_backend.model.entity.CourseQa;
import com.example.smart_education_platform_backend.model.entity.OrderInfo;
import com.example.smart_education_platform_backend.model.entity.StudyRecord;
import com.example.smart_education_platform_backend.model.entity.Users;
import com.example.smart_education_platform_backend.model.vo.CourseCardVO;
import com.example.smart_education_platform_backend.model.vo.CourseCategoryVO;
import com.example.smart_education_platform_backend.model.vo.CourseChapterVO;
import com.example.smart_education_platform_backend.model.vo.CourseCommentVO;
import com.example.smart_education_platform_backend.model.vo.CourseDetailVO;
import com.example.smart_education_platform_backend.model.vo.CourseEnrollVO;
import com.example.smart_education_platform_backend.model.vo.CourseQuestionVO;
import com.example.smart_education_platform_backend.service.CourseService;
import com.example.smart_education_platform_backend.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private static final int STATUS_ON_SHELF = 1;
    private static final int COMMENT_STATUS_NORMAL = 1;

    private final CourseCategoryMapper courseCategoryMapper;
    private final CourseChapterMapper courseChapterMapper;
    private final CourseCollectionMapper courseCollectionMapper;
    private final CourseCommentMapper courseCommentMapper;
    private final CourseQaMapper courseQaMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private final StudyRecordMapper studyRecordMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final UserMapper userMapper;

    /** 自注入代理，保证 getCourseDetail 内部调用走 @Cacheable 缓存 */
    @Lazy
    @Autowired
    private CourseService self;

    @Override
    @Cacheable(cacheNames = "courseCategory", key = "#type == null ? 'all' : #type.toString()")
    public List<CourseCategoryVO> getCategoryTree(Integer type) {
        List<CourseCategory> categories = courseCategoryMapper.selectList(
                new LambdaQueryWrapper<CourseCategory>()
                        .eq(type != null, CourseCategory::getType, type)
                        .orderByAsc(CourseCategory::getSort));
        List<CourseCategoryVO> all = Converter.INSTANCE.toCourseCategoryVOList(categories);

        Map<Long, List<CourseCategoryVO>> childrenMap = all.stream()
                .filter(c -> c.getParent_id() != null && c.getParent_id() != 0)
                .collect(Collectors.groupingBy(CourseCategoryVO::getParent_id));
        List<CourseCategoryVO> roots = all.stream()
                .filter(c -> c.getParent_id() == null || c.getParent_id() == 0)
                .collect(Collectors.toList());
        roots.forEach(root -> root.setChildren(childrenMap.getOrDefault(root.getId(), List.of())));
        return roots;
    }

    @Override
    @Cacheable(cacheNames = "courseList", key = "#dto.toString()")
    public Page<CourseCardVO> pageCourses(CourseQueryDTO dto) {
        var query = lambdaQuery()
                .eq(Course::getType, dto.getType())
                .eq(Course::getStatus, STATUS_ON_SHELF)
                .eq(dto.getTech_system_id() != null, Course::getTech_system_id, dto.getTech_system_id())
                .eq(dto.getTech_direction_id() != null, Course::getTech_direction_id, dto.getTech_direction_id())
                .eq(dto.getLevel() != null, Course::getLevel, dto.getLevel())
                .eq(dto.getIs_free() != null, Course::getIs_free, dto.getIs_free())
                // 搜索：课程名 / 讲师名模糊匹配
                .and(StringUtils.hasText(dto.getKeyword()), wrapper -> wrapper
                        .like(Course::getTitle, dto.getKeyword())
                        .or()
                        .like(Course::getTeacher_name, dto.getKeyword()));

        // 排序：白名单字段，默认上架时间倒序
        String sortBy = dto.getSort_by() == null ? "" : dto.getSort_by();
        switch (sortBy) {
            case "student_count" -> query.orderByDesc(Course::getStudent_count);
            case "score" -> query.orderByDesc(Course::getScore);
            default -> query.orderByDesc(Course::getPublish_time);
        }

        Page<Course> page = query.page(new Page<>(dto.getPage_num(), dto.getPage_size()));
        Page<CourseCardVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(Converter.INSTANCE.toCourseCardVOList(page.getRecords()));
        return voPage;
    }

    @Override
    public CourseDetailVO getCourseDetail(Long id) {
        CourseDetailVO detail = self.getCourseBase(id);
        Long userId = UserContext.getUserIdOrNull();
        if (userId == null) {
            detail.setIs_collected(false);
            detail.setIs_enrolled(false);
            detail.setProgress(0);
            return detail;
        }
        Long collected = courseCollectionMapper.selectCount(new LambdaQueryWrapper<CourseCollection>()
                .eq(CourseCollection::getUser_id, userId)
                .eq(CourseCollection::getCourse_id, id));
        detail.setIs_collected(collected != null && collected > 0);

        CourseEnrollment enrollment = courseEnrollmentMapper.selectOne(new LambdaQueryWrapper<CourseEnrollment>()
                .eq(CourseEnrollment::getUser_id, userId)
                .eq(CourseEnrollment::getCourse_id, id));
        detail.setIs_enrolled(enrollment != null);
        detail.setProgress(enrollment == null ? 0 : enrollment.getProgress());
        return detail;
    }

    @Override
    @Cacheable(cacheNames = "courseDetail", key = "#id")
    public CourseDetailVO getCourseBase(Long id) {
        Course course = getById(id);
        if (course == null || course.getStatus() == null || course.getStatus() != STATUS_ON_SHELF) {
            throw new CourseException("课程不存在或已下架");
        }
        return Converter.INSTANCE.toCourseDetailVO(course);
    }

    @Override
    public List<CourseChapterVO> getChapters(Long courseId) {
        // 校验课程存在且在架
        self.getCourseBase(courseId);
        List<CourseChapter> chapters = courseChapterMapper.selectList(
                new LambdaQueryWrapper<CourseChapter>()
                        .eq(CourseChapter::getCourse_id, courseId)
                        .orderByAsc(CourseChapter::getSort));
        return Converter.INSTANCE.toCourseChapterVOList(chapters);
    }

    @Override
    public Boolean toggleCollect(Long courseId) {
        self.getCourseBase(courseId);
        Long userId = UserContext.getUserId();
        Long existed = courseCollectionMapper.selectCount(new LambdaQueryWrapper<CourseCollection>()
                .eq(CourseCollection::getUser_id, userId)
                .eq(CourseCollection::getCourse_id, courseId));
        if (existed != null && existed > 0) {
            courseCollectionMapper.deleteByUserAndCourse(userId, courseId);
            return false;
        }
        CourseCollection collection = new CourseCollection();
        collection.setUser_id(userId);
        collection.setCourse_id(courseId);
        courseCollectionMapper.insert(collection);
        return true;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "courseList", allEntries = true),
            @CacheEvict(cacheNames = "courseDetail", key = "#courseId")
    })
    public CourseEnrollVO enroll(Long courseId) {
        Course course = getById(courseId);
        if (course == null || course.getStatus() == null || course.getStatus() != STATUS_ON_SHELF) {
            throw new CourseException("课程不存在或已下架");
        }
        Long userId = UserContext.getUserId();

        Long enrolled = courseEnrollmentMapper.selectCount(new LambdaQueryWrapper<CourseEnrollment>()
                .eq(CourseEnrollment::getUser_id, userId)
                .eq(CourseEnrollment::getCourse_id, courseId));
        if (enrolled != null && enrolled > 0) {
            throw new CourseException("请勿重复报名");
        }

        CourseEnrollVO vo = new CourseEnrollVO();
        if (course.getIs_free() != null && course.getIs_free() == 1) {
            CourseEnrollment enrollment = new CourseEnrollment();
            enrollment.setUser_id(userId);
            enrollment.setCourse_id(courseId);
            enrollment.setProgress(0);
            enrollment.setFinish_count(0);
            enrollment.setTotal_count(0);
            courseEnrollmentMapper.insert(enrollment);
            lambdaUpdate().eq(Course::getId, courseId)
                    .setSql("student_count = student_count + 1")
                    .update();
            vo.setEnrolled(true);
            return vo;
        }

        // 收费课程：已有待支付订单则直接复用，否则生成新订单
        OrderInfo existing = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getUser_id, userId)
                .eq(OrderInfo::getCourse_id, courseId)
                .eq(OrderInfo::getStatus, 0)
                .orderByDesc(OrderInfo::getId)
                .last("LIMIT 1"));
        if (existing != null) {
            vo.setEnrolled(false);
            vo.setOrder_no(existing.getOrder_no());
            return vo;
        }
        OrderInfo order = new OrderInfo();
        order.setOrder_no(generateOrderNo());
        order.setUser_id(userId);
        order.setCourse_id(courseId);
        order.setAmount(course.getPrice());
        order.setStatus(0);
        orderInfoMapper.insert(order);
        vo.setEnrolled(false);
        vo.setOrder_no(order.getOrder_no());
        return vo;
    }

    @Override
    public Page<CourseCommentVO> pageComments(Long courseId, Integer pageNum, Integer pageSize) {
        self.getCourseBase(courseId);
        Page<CourseComment> page = courseCommentMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<CourseComment>()
                        .eq(CourseComment::getCourse_id, courseId)
                        .eq(CourseComment::getStatus, COMMENT_STATUS_NORMAL)
                        .orderByDesc(CourseComment::getCreated_at));
        List<CourseCommentVO> records = Converter.INSTANCE.toCourseCommentVOList(page.getRecords());
        fillCommentUsers(records);
        Page<CourseCommentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "courseList", allEntries = true),
            @CacheEvict(cacheNames = "courseDetail", key = "#courseId")
    })
    public void addComment(Long courseId, CourseCommentDTO dto) {
        self.getCourseBase(courseId);
        Long userId = UserContext.getUserId();

        Long enrolled = courseEnrollmentMapper.selectCount(new LambdaQueryWrapper<CourseEnrollment>()
                .eq(CourseEnrollment::getUser_id, userId)
                .eq(CourseEnrollment::getCourse_id, courseId));
        if (enrolled == null || enrolled == 0) {
            throw new CourseException("报名后才能评论");
        }
        Long existed = courseCommentMapper.selectCount(new LambdaQueryWrapper<CourseComment>()
                .eq(CourseComment::getUser_id, userId)
                .eq(CourseComment::getCourse_id, courseId));
        if (existed != null && existed > 0) {
            throw new CourseException("您已评价过该课程");
        }

        CourseComment comment = new CourseComment();
        comment.setCourse_id(courseId);
        comment.setUser_id(userId);
        comment.setScore(dto.getScore());
        comment.setContent(dto.getContent());
        comment.setStatus(COMMENT_STATUS_NORMAL);
        courseCommentMapper.insert(comment);

        // 评论提交后实时重算平均分与评价人次。
        // 使用单条 SQL 原子更新，避免并发评论时「读-改-写」造成统计丢更新
        // （同一 UPDATE 内计算 score 时 rating_count 仍是更新前的值）
        int newScore = dto.getScore();
        lambdaUpdate().eq(Course::getId, courseId)
                .setSql("score = ROUND((score * rating_count + " + newScore + ") / (rating_count + 1), 1)")
                .setSql("rating_count = rating_count + 1")
                .update();
    }

    @Override
    public List<CourseQuestionVO> listQuestions(Long courseId) {
        self.getCourseBase(courseId);
        List<CourseQa> qas = courseQaMapper.selectList(new LambdaQueryWrapper<CourseQa>()
                .eq(CourseQa::getCourse_id, courseId)
                .orderByDesc(CourseQa::getCreated_at));
        List<CourseQuestionVO> records = Converter.INSTANCE.toCourseQuestionVOList(qas);
        fillQuestionUsers(records);
        return records;
    }

    @Override
    public void addQuestion(Long courseId, CourseQuestionDTO dto) {
        self.getCourseBase(courseId);
        Long userId = UserContext.getUserId();
        CourseQa qa = new CourseQa();
        qa.setCourse_id(courseId);
        qa.setUser_id(userId);
        qa.setQuestion(dto.getQuestion());
        qa.setStatus(0);
        courseQaMapper.insert(qa);
    }

    @Override
    @Transactional
    public void finishChapter(Long chapterId) {
        CourseChapter chapter = courseChapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new CourseException("章节不存在");
        }
        Long courseId = chapter.getCourse_id();
        Long userId = UserContext.getUserId();

        CourseEnrollment enrollment = courseEnrollmentMapper.selectOne(new LambdaQueryWrapper<CourseEnrollment>()
                .eq(CourseEnrollment::getUser_id, userId)
                .eq(CourseEnrollment::getCourse_id, courseId));
        if (enrollment == null) {
            throw new CourseException("报名后才能学习");
        }

        Long existed = studyRecordMapper.selectCount(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUser_id, userId)
                .eq(StudyRecord::getChapter_id, chapterId));
        if (existed == null || existed == 0) {
            StudyRecord record = new StudyRecord();
            record.setUser_id(userId);
            record.setCourse_id(courseId);
            record.setChapter_id(chapterId);
            studyRecordMapper.insert(record);
        }

        // 只统计「现存章节」对应的学习记录，避免章节被删除后残留记录导致进度超过 100%
        List<Long> chapterIds = courseChapterMapper.selectList(new LambdaQueryWrapper<CourseChapter>()
                        .eq(CourseChapter::getCourse_id, courseId))
                .stream()
                .map(CourseChapter::getId)
                .toList();
        int total = chapterIds.size();
        long finishCount = chapterIds.isEmpty() ? 0L
                : studyRecordMapper.selectCount(new LambdaQueryWrapper<StudyRecord>()
                        .eq(StudyRecord::getUser_id, userId)
                        .eq(StudyRecord::getCourse_id, courseId)
                        .in(StudyRecord::getChapter_id, chapterIds));
        int progress = total == 0 ? 0 : (int) Math.round((double) finishCount / total * 100);

        enrollment.setFinish_count((int) finishCount);
        enrollment.setTotal_count(total);
        enrollment.setProgress(progress);
        courseEnrollmentMapper.updateById(enrollment);
    }

    private String generateOrderNo() {
        return System.currentTimeMillis() + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private void fillCommentUsers(List<CourseCommentVO> records) {
        if (records.isEmpty()) {
            return;
        }
        Set<Long> userIds = records.stream().map(CourseCommentVO::getUser_id).collect(Collectors.toSet());
        Map<Long, Users> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(Users::getId, u -> u, (a, b) -> a));
        records.forEach(c -> {
            Users user = userMap.get(c.getUser_id());
            if (user != null) {
                c.setNickname(user.getNickname());
                c.setAvatar(user.getAvatar());
            }
        });
    }

    private void fillQuestionUsers(List<CourseQuestionVO> records) {
        if (records.isEmpty()) {
            return;
        }
        Set<Long> userIds = records.stream().map(CourseQuestionVO::getUser_id).collect(Collectors.toSet());
        Map<Long, Users> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(Users::getId, u -> u, (a, b) -> a));
        records.forEach(q -> {
            Users user = userMap.get(q.getUser_id());
            if (user != null) {
                q.setNickname(user.getNickname());
            }
        });
    }
}
