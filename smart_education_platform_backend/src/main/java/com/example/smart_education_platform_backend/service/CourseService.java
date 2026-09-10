package com.example.smart_education_platform_backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.smart_education_platform_backend.model.dto.CourseCommentDTO;
import com.example.smart_education_platform_backend.model.dto.CourseQueryDTO;
import com.example.smart_education_platform_backend.model.dto.CourseQuestionDTO;
import com.example.smart_education_platform_backend.model.entity.Course;
import com.example.smart_education_platform_backend.model.vo.CourseCardVO;
import com.example.smart_education_platform_backend.model.vo.CourseCategoryVO;
import com.example.smart_education_platform_backend.model.vo.CourseChapterVO;
import com.example.smart_education_platform_backend.model.vo.CourseCommentVO;
import com.example.smart_education_platform_backend.model.vo.CourseDetailVO;
import com.example.smart_education_platform_backend.model.vo.CourseEnrollVO;
import com.example.smart_education_platform_backend.model.vo.CourseQuestionVO;

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

    /** 收藏/取消收藏（切换，幂等），返回收藏后的状态 */
    Boolean toggleCollect(Long courseId);

    /** 立即学习：免费直接报名，收费生成待支付订单 */
    CourseEnrollVO enroll(Long courseId);

    /** 评论分页（仅展示正常评论） */
    Page<CourseCommentVO> pageComments(Long courseId, Integer pageNum, Integer pageSize);

    /** 发表评论+评分（须已报名，每人每课仅一条） */
    void addComment(Long courseId, CourseCommentDTO dto);

    /** 答疑列表（按时间倒序） */
    List<CourseQuestionVO> listQuestions(Long courseId);

    /** 提交答疑问题 */
    void addQuestion(Long courseId, CourseQuestionDTO dto);

    /** 标记章节/资源学完，更新学习进展 */
    void finishChapter(Long chapterId);
}
