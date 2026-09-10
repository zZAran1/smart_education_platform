package com.example.smart_education_platform_backend.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseQuestionVO {
    private Long id;
    private Long course_id;
    private Long user_id;
    private String question;
    private String answer;
    private Long answerer_id;
    private Integer status;
    private LocalDateTime created_at;
    /** 提问用户昵称（查询时组装） */
    private String nickname;
}
