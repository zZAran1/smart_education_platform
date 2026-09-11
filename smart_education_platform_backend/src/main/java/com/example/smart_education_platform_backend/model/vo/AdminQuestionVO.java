package com.example.smart_education_platform_backend.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 后台答疑视图：附带课程标题与提问人昵称，便于管理员回复 */
@Data
public class AdminQuestionVO {

    private Long id;
    private Long course_id;
    private String course_title;
    private Long user_id;
    private String nickname;
    private String question;
    private String answer;
    /** 0待回复 1已回复 */
    private Integer status;
    private LocalDateTime created_at;
}
