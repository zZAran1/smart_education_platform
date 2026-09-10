package com.example.smart_education_platform_backend.controller;

import com.example.smart_education_platform_backend.result.Result;
import com.example.smart_education_platform_backend.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyController {

    private final CourseService courseService;

    @PostMapping("/chapters/{chapterId}/finish")
    public Result<Void> finishChapter(@PathVariable Long chapterId) {
        courseService.finishChapter(chapterId);
        return Result.success("已标记学完");
    }
}
