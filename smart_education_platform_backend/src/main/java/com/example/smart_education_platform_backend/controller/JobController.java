package com.example.smart_education_platform_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smart_education_platform_backend.model.dto.ApplyJobDTO;
import com.example.smart_education_platform_backend.model.dto.JobQueryDTO;
import com.example.smart_education_platform_backend.model.vo.JobCardVO;
import com.example.smart_education_platform_backend.model.vo.JobCategoryVO;
import com.example.smart_education_platform_backend.model.vo.JobDetailVO;
import com.example.smart_education_platform_backend.model.vo.MyInterviewVO;
import com.example.smart_education_platform_backend.model.vo.MyJobApplicationVO;
import com.example.smart_education_platform_backend.result.Result;
import com.example.smart_education_platform_backend.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @GetMapping("/categories")
    public Result<List<JobCategoryVO>> categories() {
        return Result.success(jobService.getCategoryTree());
    }

    @GetMapping("/page")
    public Result<Page<JobCardVO>> page(@Valid @ModelAttribute JobQueryDTO dto) {
        return Result.success(jobService.pageJobs(dto));
    }

    @GetMapping("/{id}")
    public Result<JobDetailVO> detail(@PathVariable Long id) {
        return Result.success(jobService.getJobDetail(id));
    }

    @PostMapping("/{id}/collect")
    public Result<Boolean> collect(@PathVariable Long id) {
        return Result.success(jobService.toggleCollect(id));
    }

    @PostMapping("/{id}/apply")
    public Result<Void> apply(@PathVariable Long id, @Valid @RequestBody ApplyJobDTO dto) {
        jobService.apply(id, dto);
        return Result.success("投递成功");
    }

    @PostMapping("/{id}/ai-interview")
    public Result<Long> aiInterview(@PathVariable Long id) {
        return Result.success(jobService.applyAiInterview(id));
    }

    /** 我的投递：当前登录用户的投递记录 + 职位/公司信息 */
    @GetMapping("/my-applications")
    public Result<Page<MyJobApplicationVO>> myApplications(@RequestParam(defaultValue = "1") Integer page_num,
                                                           @RequestParam(defaultValue = "10") Integer page_size,
                                                           @RequestParam(required = false) Integer status) {
        return Result.success(jobService.pageMyApplications(page_num, page_size, status));
    }

    /** 我的数字人面试：当前登录用户的面试记录 + 职位/公司信息 */
    @GetMapping("/my-interviews")
    public Result<Page<MyInterviewVO>> myInterviews(@RequestParam(defaultValue = "1") Integer page_num,
                                                    @RequestParam(defaultValue = "10") Integer page_size,
                                                    @RequestParam(required = false) Integer status) {
        return Result.success(jobService.pageMyInterviews(page_num, page_size, status));
    }
}
