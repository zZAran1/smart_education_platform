package com.example.smart_education_platform_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smart_education_platform_backend.model.dto.CourseQueryDTO;
import com.example.smart_education_platform_backend.model.vo.CourseCardVO;
import com.example.smart_education_platform_backend.model.vo.CourseCategoryVO;
import com.example.smart_education_platform_backend.model.vo.CourseChapterVO;
import com.example.smart_education_platform_backend.model.vo.CourseDetailVO;
import com.example.smart_education_platform_backend.result.Result;
import com.example.smart_education_platform_backend.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/{id}/chapters")
    public Result<List<CourseChapterVO>> chapters(@PathVariable Long id) {
        return Result.success(courseService.getChapters(id));
    }
}
