package com.example.smart_education_platform_backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.smart_education_platform_backend.model.dto.CourseQueryDTO;
import com.example.smart_education_platform_backend.model.entity.Course;
import com.example.smart_education_platform_backend.model.vo.CourseCardVO;
import com.example.smart_education_platform_backend.model.vo.CourseCategoryVO;
import com.example.smart_education_platform_backend.model.vo.CourseChapterVO;
import com.example.smart_education_platform_backend.model.vo.CourseDetailVO;

import java.util.List;

public interface CourseService extends IService<Course> {

    /** 课程分类两级字典（技术体系/技术方向），可按适用课程类型过滤，缓存 24h */
    List<CourseCategoryVO> getCategoryTree(Integer type);

    /** 课程中心分页：类型/分类/等级/免费筛选 + 关键词搜索（课程名/讲师名） + 排序，缓存 5min */
    Page<CourseCardVO> pageCourses(CourseQueryDTO dto);

    /** 课程详情：基础信息（缓存） + 登录增强（收藏状态/是否报名/学习进展） */
    CourseDetailVO getCourseDetail(Long id);

    /** 课程详情基础信息（@Cacheable courseDetail，不含用户相关字段） */
    CourseDetailVO getCourseBase(Long id);

    /** 课程目录（按排序号升序） */
    List<CourseChapterVO> getChapters(Long courseId);
}
