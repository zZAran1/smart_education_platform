package com.example.smart_education_platform_backend.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 后台评论视图：附带课程标题与评论人昵称，便于管理员识别 */
@Data
public class AdminCommentVO {

    private Long id;
    private Long course_id;
    private String course_title;
    private Long user_id;
    private String nickname;
    private Integer score;
    private String content;
    /** 0隐藏 1正常 */
    private Integer status;
    private LocalDateTime created_at;
}
