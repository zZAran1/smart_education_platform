package com.example.smart_education_platform_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smart_education_platform_backend.model.dto.CourseCommentDTO;
import com.example.smart_education_platform_backend.model.dto.CourseQueryDTO;
import com.example.smart_education_platform_backend.model.dto.CourseQuestionDTO;
import com.example.smart_education_platform_backend.model.vo.CourseCardVO;
import com.example.smart_education_platform_backend.model.vo.CourseCategoryVO;
import com.example.smart_education_platform_backend.model.vo.CourseChapterVO;
import com.example.smart_education_platform_backend.model.vo.CourseCommentVO;
import com.example.smart_education_platform_backend.model.vo.CourseDetailVO;
import com.example.smart_education_platform_backend.model.vo.CourseEnrollVO;
import com.example.smart_education_platform_backend.model.vo.CourseQuestionVO;
import com.example.smart_education_platform_backend.model.vo.MyCourseVO;
import com.example.smart_education_platform_backend.result.Result;
import com.example.smart_education_platform_backend.service.CourseService;
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
@RequestMapping("/api/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping("/categories")
    public Result<List<CourseCategoryVO>> categories(@RequestParam(required = false) Integer type) {
        return Result.success(courseService.getCategoryTree(type));
    }

    @GetMapping("/page")
    public Result<Page<CourseCardVO>> page(@Valid @ModelAttribute CourseQueryDTO dto) {
        return Result.success(courseService.pageCourses(dto));
    }

    @GetMapping("/{id}")
    public Result<CourseDetailVO> detail(@PathVariable Long id) {
        return Result.success(courseService.getCourseDetail(id));
    }

    /** 我的课程：当前登录用户的报名记录 + 学习进度 */
    @GetMapping("/my")
    public Result<Page<MyCourseVO>> my(@RequestParam(defaultValue = "1") Integer page_num,
                                       @RequestParam(defaultValue = "10") Integer page_size) {
        return Result.success(courseService.pageMyCourses(page_num, page_size));
    }

    @GetMapping("/{id}/chapters")
    public Result<List<CourseChapterVO>> chapters(@PathVariable Long id) {
        return Result.success(courseService.getChapters(id));
    }

    @PostMapping("/{id}/collect")
    public Result<Boolean> collect(@PathVariable Long id) {
        return Result.success(courseService.toggleCollect(id));
    }

    @PostMapping("/{id}/enroll")
    public Result<CourseEnrollVO> enroll(@PathVariable Long id) {
        return Result.success(courseService.enroll(id));
    }

    @GetMapping("/{id}/comments")
    public Result<Page<CourseCommentVO>> comments(@PathVariable Long id,
                                                  @RequestParam(defaultValue = "1") Integer page_num,
                                                  @RequestParam(defaultValue = "10") Integer page_size) {
        return Result.success(courseService.pageComments(id, page_num, page_size));
    }

    @PostMapping("/{id}/comments")
    public Result<Void> addComment(@PathVariable Long id, @Valid @RequestBody CourseCommentDTO dto) {
        courseService.addComment(id, dto);
        return Result.success("评论成功");
    }

    @GetMapping("/{id}/questions")
    public Result<List<CourseQuestionVO>> questions(@PathVariable Long id) {
        return Result.success(courseService.listQuestions(id));
    }

    @PostMapping("/{id}/questions")
    public Result<Void> addQuestion(@PathVariable Long id, @Valid @RequestBody CourseQuestionDTO dto) {
        courseService.addQuestion(id, dto);
        return Result.success("提问成功");
    }
}
