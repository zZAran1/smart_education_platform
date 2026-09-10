package com.example.smart_education_platform_backend.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseCommentVO {
    private Long id;
    private Long course_id;
    private Long user_id;
    private Integer score;
    private String content;
    private LocalDateTime created_at;
    /** 评论用户昵称（查询时组装） */
    private String nickname;
    /** 评论用户头像（查询时组装） */
    private String avatar;
}
