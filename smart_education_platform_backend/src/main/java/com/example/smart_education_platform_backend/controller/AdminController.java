package com.example.smart_education_platform_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smart_education_platform_backend.model.dto.AdminChapterDTO;
import com.example.smart_education_platform_backend.model.dto.AdminCompanyDTO;
import com.example.smart_education_platform_backend.model.dto.AdminCourseDTO;
import com.example.smart_education_platform_backend.model.dto.AdminJobDTO;
import com.example.smart_education_platform_backend.model.dto.AnswerDTO;
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
import com.example.smart_education_platform_backend.result.Result;
import com.example.smart_education_platform_backend.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public Result<Page<UserVO>> users(@RequestParam(defaultValue = "1") Integer page_num,
                                      @RequestParam(defaultValue = "10") Integer page_size,
                                      @RequestParam(required = false) String username,
                                      @RequestParam(required = false) Integer status) {
        return Result.success(adminService.pageUsers(page_num, page_size, username, status));
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminService.updateUserStatus(id, status);
        return Result.success("操作成功");
    }

    @GetMapping("/orders")
    public Result<Page<OrderInfo>> orders(@RequestParam(defaultValue = "1") Integer page_num,
                                          @RequestParam(defaultValue = "10") Integer page_size,
                                          @RequestParam(required = false) Long user_id,
                                          @RequestParam(required = false) Long course_id,
                                          @RequestParam(required = false) Integer status) {
        return Result.success(adminService.pageOrders(page_num, page_size, user_id, course_id, status));
    }

    @PutMapping("/orders/{id}/refund")
    public Result<Void> refund(@PathVariable Long id) {
        adminService.refundOrder(id);
        return Result.success("退款成功");
    }

    @GetMapping("/courses")
    public Result<Page<Course>> courses(@RequestParam(defaultValue = "1") Integer page_num,
                                        @RequestParam(defaultValue = "10") Integer page_size,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Integer status) {
        return Result.success(adminService.pageCourses(page_num, page_size, keyword, status));
    }

    @PostMapping("/courses")
    public Result<Long> createCourse(@Valid @RequestBody AdminCourseDTO dto) {
        return Result.success("新增成功", adminService.createCourse(dto));
    }

    @PutMapping("/courses/{id}")
    public Result<Void> updateCourse(@PathVariable Long id, @Valid @RequestBody AdminCourseDTO dto) {
        adminService.updateCourse(id, dto);
        return Result.success("保存成功");
    }

    @PutMapping("/courses/{id}/status")
    public Result<Void> updateCourseStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminService.updateCourseStatus(id, status);
        return Result.success("操作成功");
    }

    @GetMapping("/jobs")
    public Result<Page<Job>> jobs(@RequestParam(defaultValue = "1") Integer page_num,
                                  @RequestParam(defaultValue = "10") Integer page_size,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Integer status) {
        return Result.success(adminService.pageJobs(page_num, page_size, keyword, status));
    }

    @PostMapping("/jobs")
    public Result<Long> createJob(@Valid @RequestBody AdminJobDTO dto) {
        return Result.success("新增成功", adminService.createJob(dto));
    }

    @PutMapping("/jobs/{id}")
    public Result<Void> updateJob(@PathVariable Long id, @Valid @RequestBody AdminJobDTO dto) {
        adminService.updateJob(id, dto);
        return Result.success("保存成功");
    }

    @PutMapping("/jobs/{id}/status")
    public Result<Void> updateJobStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminService.updateJobStatus(id, status);
        return Result.success("操作成功");
    }

    @GetMapping("/applications")
    public Result<Page<JobApplication>> applications(@RequestParam(defaultValue = "1") Integer page_num,
                                                     @RequestParam(defaultValue = "10") Integer page_size,
                                                     @RequestParam(required = false) Integer status) {
        return Result.success(adminService.pageApplications(page_num, page_size, status));
    }

    @PutMapping("/applications/{id}/status")
    public Result<Void> updateApplicationStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminService.updateApplicationStatus(id, status);
        return Result.success("操作成功");
    }

    @GetMapping("/interviews")
    public Result<Page<AiInterview>> interviews(@RequestParam(defaultValue = "1") Integer page_num,
                                                @RequestParam(defaultValue = "10") Integer page_size,
                                                @RequestParam(required = false) Integer status) {
        return Result.success(adminService.pageInterviews(page_num, page_size, status));
    }

    @PutMapping("/interviews/{id}")
    public Result<Void> updateInterview(@PathVariable Long id, @RequestParam Integer status) {
        adminService.updateInterview(id, status);
        return Result.success("操作成功");
    }

    @GetMapping("/comments")
    public Result<Page<AdminCommentVO>> comments(@RequestParam(defaultValue = "1") Integer page_num,
                                                 @RequestParam(defaultValue = "10") Integer page_size,
                                                 @RequestParam(required = false) Integer status) {
        return Result.success(adminService.pageComments(page_num, page_size, status));
    }

    @GetMapping("/questions")
    public Result<Page<AdminQuestionVO>> questions(@RequestParam(defaultValue = "1") Integer page_num,
                                                   @RequestParam(defaultValue = "10") Integer page_size,
                                                   @RequestParam(required = false) Integer status) {
        return Result.success(adminService.pageQuestions(page_num, page_size, status));
    }

    @DeleteMapping("/comments/{id}")
    public Result<Void> hideComment(@PathVariable Long id) {
        adminService.hideComment(id);
        return Result.success("评论已隐藏");
    }

    @PutMapping("/questions/{id}/answer")
    public Result<Void> answerQuestion(@PathVariable Long id, @Valid @RequestBody AnswerDTO dto) {
        adminService.answerQuestion(id, dto.getAnswer());
        return Result.success("回复成功");
    }

    /* ---------- 章节目录 ---------- */

    @GetMapping("/courses/{courseId}/chapters")
    public Result<List<CourseChapter>> chapters(@PathVariable Long courseId) {
        return Result.success(adminService.listChapters(courseId));
    }

    @PostMapping("/courses/{courseId}/chapters")
    public Result<Long> createChapter(@PathVariable Long courseId,
                                      @Valid @RequestBody AdminChapterDTO dto) {
        return Result.success("新增成功", adminService.createChapter(courseId, dto));
    }

    @PutMapping("/chapters/{id}")
    public Result<Void> updateChapter(@PathVariable Long id, @Valid @RequestBody AdminChapterDTO dto) {
        adminService.updateChapter(id, dto);
        return Result.success("保存成功");
    }

    @DeleteMapping("/chapters/{id}")
    public Result<Void> deleteChapter(@PathVariable Long id) {
        adminService.deleteChapter(id);
        return Result.success("删除成功");
    }

    /* ---------- 公司 ---------- */

    @GetMapping("/companies")
    public Result<Page<Company>> companies(@RequestParam(defaultValue = "1") Integer page_num,
                                           @RequestParam(defaultValue = "10") Integer page_size,
                                           @RequestParam(required = false) String keyword) {
        return Result.success(adminService.pageCompanies(page_num, page_size, keyword));
    }

    @PostMapping("/companies")
    public Result<Long> createCompany(@Valid @RequestBody AdminCompanyDTO dto) {
        return Result.success("新增成功", adminService.createCompany(dto));
    }

    @PutMapping("/companies/{id}")
    public Result<Void> updateCompany(@PathVariable Long id, @Valid @RequestBody AdminCompanyDTO dto) {
        adminService.updateCompany(id, dto);
        return Result.success("保存成功");
    }

    @DeleteMapping("/companies/{id}")
    public Result<Void> deleteCompany(@PathVariable Long id) {
        adminService.deleteCompany(id);
        return Result.success("删除成功");
    }
}
