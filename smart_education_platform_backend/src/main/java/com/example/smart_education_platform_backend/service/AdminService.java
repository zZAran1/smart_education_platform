package com.example.smart_education_platform_backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smart_education_platform_backend.model.dto.AdminChapterDTO;
import com.example.smart_education_platform_backend.model.dto.AdminCompanyDTO;
import com.example.smart_education_platform_backend.model.dto.AdminCourseDTO;
import com.example.smart_education_platform_backend.model.dto.AdminJobDTO;
import com.example.smart_education_platform_backend.model.entity.AiInterview;
import com.example.smart_education_platform_backend.model.entity.Company;
import com.example.smart_education_platform_backend.model.entity.Course;
import com.example.smart_education_platform_backend.model.entity.CourseChapter;
import com.example.smart_education_platform_backend.model.entity.Job;
import com.example.smart_education_platform_backend.model.entity.JobApplication;
import com.example.smart_education_platform_backend.model.entity.OrderInfo;
import com.example.smart_education_platform_backend.model.vo.AdminCommentVO;
import com.example.smart_education_platform_backend.model.vo.AdminQuestionVO;
import com.example.smart_education_platform_backend.model.vo.UserVO;

import java.util.List;

public interface AdminService {

    /** 管理员角色二次校验，非管理员抛 1013 */
    void checkRole();

    /* ---------- 用户 ---------- */

    Page<UserVO> pageUsers(Integer pageNum, Integer pageSize, String username, Integer status);

    void updateUserStatus(Long userId, Integer status);

    /* ---------- 订单 ---------- */

    Page<OrderInfo> pageOrders(Integer pageNum, Integer pageSize, Long userId, Long courseId, Integer status);

    void refundOrder(Long orderId);

    /* ---------- 课程 ---------- */

    Page<Course> pageCourses(Integer pageNum, Integer pageSize, String keyword, Integer status);

    Long createCourse(AdminCourseDTO dto);

    void updateCourse(Long courseId, AdminCourseDTO dto);

    void updateCourseStatus(Long courseId, Integer status);

    /* ---------- 职位 ---------- */

    Page<Job> pageJobs(Integer pageNum, Integer pageSize, String keyword, Integer status);

    Long createJob(AdminJobDTO dto);

    void updateJob(Long jobId, AdminJobDTO dto);

    void updateJobStatus(Long jobId, Integer status);

    /* ---------- 职位申请 / AI 面试 ---------- */

    Page<JobApplication> pageApplications(Integer pageNum, Integer pageSize, Integer status);

    void updateApplicationStatus(Long applicationId, Integer status);

    Page<AiInterview> pageInterviews(Integer pageNum, Integer pageSize, Integer status);

    void updateInterview(Long interviewId, Integer status);

    /* ---------- 评论 / 答疑 ---------- */

    Page<AdminCommentVO> pageComments(Integer pageNum, Integer pageSize, Integer status);

    Page<AdminQuestionVO> pageQuestions(Integer pageNum, Integer pageSize, Integer status);

    void hideComment(Long commentId);

    void answerQuestion(Long questionId, String answer);

    /* ---------- 章节目录 ---------- */

    List<CourseChapter> listChapters(Long courseId);

    Long createChapter(Long courseId, AdminChapterDTO dto);

    void updateChapter(Long chapterId, AdminChapterDTO dto);

    void deleteChapter(Long chapterId);

    /* ---------- 公司 ---------- */

    Page<Company> pageCompanies(Integer pageNum, Integer pageSize, String keyword);

    Long createCompany(AdminCompanyDTO dto);

    void updateCompany(Long companyId, AdminCompanyDTO dto);

    void deleteCompany(Long companyId);
}
