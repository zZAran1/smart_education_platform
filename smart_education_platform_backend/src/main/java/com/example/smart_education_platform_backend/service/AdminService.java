package com.example.smart_education_platform_backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smart_education_platform_backend.model.dto.AdminCourseDTO;
import com.example.smart_education_platform_backend.model.dto.AdminJobDTO;
import com.example.smart_education_platform_backend.model.entity.AiInterview;
import com.example.smart_education_platform_backend.model.entity.Course;
import com.example.smart_education_platform_backend.model.entity.Job;
import com.example.smart_education_platform_backend.model.entity.JobApplication;
import com.example.smart_education_platform_backend.model.entity.OrderInfo;
import com.example.smart_education_platform_backend.model.vo.UserVO;

public interface AdminService {

    /** 管理员角色二次校验，非管理员抛 1013 */
    void checkRole();

    Page<UserVO> pageUsers(Integer pageNum, Integer pageSize, String username, Integer status);

    void updateUserStatus(Long userId, Integer status);

    Page<OrderInfo> pageOrders(Integer pageNum, Integer pageSize, Long userId, Long courseId, Integer status);

    void refundOrder(Long orderId);

    Page<Course> pageCourses(Integer pageNum, Integer pageSize, String keyword, Integer status);

    Page<Job> pageJobs(Integer pageNum, Integer pageSize, String keyword, Integer status);

    Long createCourse(AdminCourseDTO dto);

    void updateCourse(Long courseId, AdminCourseDTO dto);

    void updateCourseStatus(Long courseId, Integer status);

    Long createJob(AdminJobDTO dto);

    void updateJob(Long jobId, AdminJobDTO dto);

    void updateJobStatus(Long jobId, Integer status);

    Page<JobApplication> pageApplications(Integer pageNum, Integer pageSize, Integer status);

    void updateApplicationStatus(Long applicationId, Integer status);

    Page<AiInterview> pageInterviews(Integer pageNum, Integer pageSize, Integer status);

    void updateInterview(Long interviewId, Integer status);

    void hideComment(Long commentId);

    void answerQuestion(Long questionId, String answer);
}
