package com.example.smart_education_platform_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.smart_education_platform_backend.converter.Converter;
import com.example.smart_education_platform_backend.exception.CourseException;
import com.example.smart_education_platform_backend.mapper.CourseCategoryMapper;
import com.example.smart_education_platform_backend.mapper.CourseChapterMapper;
import com.example.smart_education_platform_backend.mapper.CourseCollectionMapper;
import com.example.smart_education_platform_backend.mapper.CourseEnrollmentMapper;
import com.example.smart_education_platform_backend.mapper.CourseMapper;
import com.example.smart_education_platform_backend.model.dto.CourseQueryDTO;
import com.example.smart_education_platform_backend.model.entity.Course;
import com.example.smart_education_platform_backend.model.entity.CourseCategory;
import com.example.smart_education_platform_backend.model.entity.CourseChapter;
import com.example.smart_education_platform_backend.model.entity.CourseCollection;
import com.example.smart_education_platform_backend.model.entity.CourseEnrollment;
import com.example.smart_education_platform_backend.model.vo.CourseCardVO;
import com.example.smart_education_platform_backend.model.vo.CourseCategoryVO;
import com.example.smart_education_platform_backend.model.vo.CourseChapterVO;
import com.example.smart_education_platform_backend.model.vo.CourseDetailVO;
import com.example.smart_education_platform_backend.service.CourseService;
import com.example.smart_education_platform_backend.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private static final int STATUS_ON_SHELF = 1;

    private final CourseCategoryMapper courseCategoryMapper;
    private final CourseChapterMapper courseChapterMapper;
    private final CourseCollectionMapper courseCollectionMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;

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
}
