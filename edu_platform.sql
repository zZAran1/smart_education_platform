-- =============================================================
-- 智慧教育平台 数据库初始化脚本
-- 依据：《系统设计文档 V1.2》第 4 章 数据库设计
-- 数据库：MySQL 8.0+，引擎 InnoDB，字符集 utf8mb4
-- 约定：主键 id BIGINT AUTO_INCREMENT；逻辑删除 deleted TINYINT DEFAULT 0；
--       审计字段 created_at / updated_at；金额 DECIMAL(10,2)
-- =============================================================

CREATE DATABASE IF NOT EXISTS `edu_platform`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE `edu_platform`;

-- =============================================================
-- 1. 用户模块
-- =============================================================

-- 用户表
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `username`   VARCHAR(32)  NOT NULL COMMENT '登录账号',
  `password`   VARCHAR(100) NOT NULL COMMENT 'BCrypt密文',
  `nickname`   VARCHAR(32)  DEFAULT NULL COMMENT '昵称',
  `real_name`  VARCHAR(32)  DEFAULT NULL COMMENT '姓名',
  `avatar`     VARCHAR(255) DEFAULT NULL COMMENT '头像URL（/uploads/前缀）',
  `email`      VARCHAR(64)  DEFAULT NULL COMMENT '邮箱',
  `phone`      VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `role`       TINYINT      NOT NULL DEFAULT 0 COMMENT '0学员 1教师 2管理员',
  `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 2封禁',
  `deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- =============================================================
-- 2. 课程模块
-- =============================================================

-- 课程表
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
  `id`                BIGINT        NOT NULL AUTO_INCREMENT,
  `title`             VARCHAR(100)  NOT NULL COMMENT '课程名称',
  `type`              TINYINT       NOT NULL COMMENT '0理论 1实训 2认证',
  `tech_system_id`    BIGINT        DEFAULT NULL COMMENT '技术体系（一级分类）',
  `tech_direction_id` BIGINT        DEFAULT NULL COMMENT '技术方向（二级分类）',
  `level`             TINYINT       DEFAULT 0 COMMENT '0初级 1中级 2高级 3专业共建合作（实训为实验等级）',
  `cover`             VARCHAR(255)  DEFAULT NULL COMMENT '封面图',
  `teacher_id`        BIGINT        DEFAULT NULL COMMENT '授课教师ID',
  `teacher_name`      VARCHAR(32)   DEFAULT NULL COMMENT '授课教师姓名（冗余便于搜索）',
  `courseware_count`  INT           DEFAULT 0 COMMENT '课件数量（冗余统计）',
  `video_count`       INT           DEFAULT 0 COMMENT '视频数量（冗余统计）',
  `lab_count`         INT           DEFAULT 0 COMMENT '实验数量（冗余统计）',
  `is_free`           TINYINT       NOT NULL DEFAULT 1 COMMENT '0收费 1免费',
  `price`             DECIMAL(10,2) DEFAULT 0.00 COMMENT '价格（免费为0）',
  `score`             DECIMAL(2,1)  DEFAULT 0.0 COMMENT '平均评分（评论后重算）',
  `rating_count`      INT           DEFAULT 0 COMMENT '评价人次',
  `student_count`     INT           DEFAULT 0 COMMENT '报名学习人数（排序用）',
  `intro`             TEXT          COMMENT '课程简介',
  `target`            TEXT          COMMENT '课程目标',
  `status`            TINYINT       NOT NULL DEFAULT 1 COMMENT '0下架 1上架',
  `publish_time`      DATETIME      DEFAULT NULL COMMENT '上架时间（排序用）',
  `deleted`           TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_list` (`type`,`status`,`tech_system_id`,`tech_direction_id`),
  KEY `idx_teacher_name` (`teacher_name`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_student_count` (`student_count`),
  KEY `idx_score` (`score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- 课程分类表（技术体系/技术方向两级字典）
DROP TABLE IF EXISTS `course_category`;
CREATE TABLE `course_category` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT,
  `parent_id`  BIGINT      NOT NULL DEFAULT 0 COMMENT '父分类ID，0=技术体系（一级分类）',
  `name`       VARCHAR(50) NOT NULL COMMENT '分类名称',
  `type`       TINYINT     NOT NULL DEFAULT 0 COMMENT '适用课程类型 0理论 1实训 2认证',
  `sort`       INT         NOT NULL DEFAULT 0 COMMENT '排序号',
  `deleted`    TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程分类表';

-- 课程目录表（章节/资源）
DROP TABLE IF EXISTS `course_chapter`;
CREATE TABLE `course_chapter` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `course_id`     BIGINT       NOT NULL COMMENT '所属课程ID',
  `title`         VARCHAR(100) NOT NULL COMMENT '资源标题',
  `resource_type` TINYINT      NOT NULL DEFAULT 0 COMMENT '0课件 1视频 2实验',
  `duration`      INT          NOT NULL DEFAULT 0 COMMENT '时长（秒）',
  `sort`          INT          NOT NULL DEFAULT 0 COMMENT '排序号',
  `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程目录表';

-- 课程评论表
DROP TABLE IF EXISTS `course_comment`;
CREATE TABLE `course_comment` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `course_id`  BIGINT       NOT NULL COMMENT '课程ID',
  `user_id`    BIGINT       NOT NULL COMMENT '用户ID',
  `score`      TINYINT      NOT NULL COMMENT '评分 1~5',
  `content`    VARCHAR(500) DEFAULT NULL COMMENT '评论内容',
  `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '0隐藏 1正常',
  `deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_user` (`course_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程评论表';

-- 课程答疑表
DROP TABLE IF EXISTS `course_qa`;
CREATE TABLE `course_qa` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `course_id`   BIGINT       NOT NULL COMMENT '课程ID',
  `user_id`     BIGINT       NOT NULL COMMENT '提问用户ID',
  `question`    VARCHAR(500) NOT NULL COMMENT '问题内容',
  `answer`      VARCHAR(1000) DEFAULT NULL COMMENT '回复内容',
  `answerer_id` BIGINT       DEFAULT NULL COMMENT '回复人ID（教师/管理员）',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '0待回复 1已回复',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程答疑表';

-- 课程收藏表
DROP TABLE IF EXISTS `course_collection`;
CREATE TABLE `course_collection` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT   NOT NULL COMMENT '用户ID',
  `course_id`  BIGINT   NOT NULL COMMENT '课程ID',
  `deleted`    TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_course` (`user_id`,`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程收藏表';

-- 课程报名记录表
DROP TABLE IF EXISTS `course_enrollment`;
CREATE TABLE `course_enrollment` (
  `id`           BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT   NOT NULL COMMENT '用户ID',
  `course_id`    BIGINT   NOT NULL COMMENT '课程ID',
  `order_id`     BIGINT   DEFAULT NULL COMMENT '关联订单ID（免费课程为NULL）',
  `progress`     INT      NOT NULL DEFAULT 0 COMMENT '学习进展百分比 0~100',
  `finish_count` INT      NOT NULL DEFAULT 0 COMMENT '已学完资源数',
  `total_count`  INT      NOT NULL DEFAULT 0 COMMENT '课程资源总数',
  `deleted`      TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是（退款作废）',
  `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_course` (`user_id`,`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程报名记录表';

-- 学习明细表
DROP TABLE IF EXISTS `study_record`;
CREATE TABLE `study_record` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT   NOT NULL COMMENT '用户ID',
  `course_id`  BIGINT   NOT NULL COMMENT '课程ID',
  `chapter_id` BIGINT   NOT NULL COMMENT '章节/资源ID',
  `deleted`    TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_chapter` (`user_id`,`chapter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习明细表';

-- =============================================================
-- 3. 实习就业模块
-- =============================================================

-- 公司表
DROP TABLE IF EXISTS `company`;
CREATE TABLE `company` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `name`       VARCHAR(100) NOT NULL COMMENT '公司名称',
  `logo`       VARCHAR(255) DEFAULT NULL COMMENT '公司LOGO',
  `industry`   VARCHAR(50)  DEFAULT NULL COMMENT '所属行业',
  `scale`      VARCHAR(50)  DEFAULT NULL COMMENT '公司规模',
  `region`     VARCHAR(50)  DEFAULT NULL COMMENT '所在地区',
  `intro`      TEXT         COMMENT '公司简介',
  `deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司表';

-- 职位分类表（两级）
DROP TABLE IF EXISTS `job_category`;
CREATE TABLE `job_category` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT,
  `parent_id`  BIGINT      NOT NULL DEFAULT 0 COMMENT '父分类ID，0=一级分类',
  `name`       VARCHAR(50) NOT NULL COMMENT '分类名称',
  `sort`       INT         NOT NULL DEFAULT 0 COMMENT '排序号',
  `deleted`    TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位分类表';

-- 职位表
DROP TABLE IF EXISTS `job`;
CREATE TABLE `job` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `title`       VARCHAR(100) NOT NULL COMMENT '职位名称',
  `city`        VARCHAR(50)  DEFAULT NULL COMMENT '工作城市',
  `address`     VARCHAR(255) DEFAULT NULL COMMENT '详细工作地址',
  `salary_min`  INT          DEFAULT NULL COMMENT '薪资下限（K）',
  `salary_max`  INT          DEFAULT NULL COMMENT '薪资上限（K）',
  `headcount`   INT          DEFAULT 1 COMMENT '招聘人数',
  `company_id`  BIGINT       NOT NULL COMMENT '公司ID',
  `category_id` BIGINT       DEFAULT NULL COMMENT '职位分类ID',
  `description` TEXT         COMMENT '职位描述（岗位职责）',
  `requirement` TEXT         COMMENT '任职要求',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '0下架 1上架',
  `expire_time` DATETIME     DEFAULT NULL COMMENT '职位到期下架时间，NULL 表示长期有效',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status_category` (`status`,`category_id`),
  KEY `idx_status_expire` (`status`,`expire_time`),
  KEY `idx_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位表';

-- 职位收藏表
DROP TABLE IF EXISTS `job_collection`;
CREATE TABLE `job_collection` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT   NOT NULL COMMENT '用户ID',
  `job_id`     BIGINT   NOT NULL COMMENT '职位ID',
  `deleted`    TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_job` (`user_id`,`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位收藏表';

-- 职位申请表
DROP TABLE IF EXISTS `job_application`;
CREATE TABLE `job_application` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT       NOT NULL COMMENT '用户ID',
  `job_id`     BIGINT       NOT NULL COMMENT '职位ID',
  `resume_url` VARCHAR(255) DEFAULT NULL COMMENT '简历附件URL（PDF，≤5MB）',
  `status`     TINYINT      NOT NULL DEFAULT 0 COMMENT '0待处理 1已查看 2通过 3拒绝',
  `deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_job` (`user_id`,`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位申请表';

-- AI面试表
DROP TABLE IF EXISTS `ai_interview`;
CREATE TABLE `ai_interview` (
  `id`             BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`        BIGINT   NOT NULL COMMENT '用户ID',
  `job_id`         BIGINT   NOT NULL COMMENT '职位ID',
  `application_id` BIGINT   NOT NULL COMMENT '关联职位申请ID',
  `status`         TINYINT  NOT NULL DEFAULT 0 COMMENT '0待进行 1进行中 2已完成',
  `report`         TEXT     COMMENT '面试报告',
  `interview_time` DATETIME DEFAULT NULL COMMENT '面试时间',
  `deleted`        TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_job` (`user_id`,`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI面试表';

-- =============================================================
-- 4. 订单模块
-- =============================================================

-- 订单表
DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT,
  `order_no`   VARCHAR(32)   NOT NULL COMMENT '订单号',
  `user_id`    BIGINT        NOT NULL COMMENT '用户ID',
  `course_id`  BIGINT        NOT NULL COMMENT '课程ID',
  `amount`     DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '订单金额',
  `pay_type`   TINYINT       DEFAULT NULL COMMENT '支付方式 0支付宝 1微信',
  `status`     TINYINT       NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付 2已退款 3已取消',
  `pay_time`   DATETIME      DEFAULT NULL COMMENT '支付时间',
  `deleted`    TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status_created` (`status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- =============================================================
-- 5. 初始化数据
-- =============================================================

-- 默认管理员账号（用户名 admin，密码请使用 BCryptPasswordUtil 生成后替换下方密文）
-- INSERT INTO `users` (`username`, `password`, `nickname`, `role`, `status`)
-- VALUES ('admin', '<BCrypt密文>', '系统管理员', 2, 1);

-- 课程分类示例：技术体系（一级）
INSERT INTO `course_category` (`parent_id`, `name`, `type`, `sort`) VALUES
(0, '人工智能', 0, 1),
(0, '大数据',   0, 2),
(0, '云计算',   0, 3),
(0, '工业软件', 0, 4),
(0, '数通',     0, 5),
(0, '通信',     0, 6),
(0, '鸿蒙',     0, 7);

-- 课程分类示例：技术方向（二级，parent_id 对应上方技术体系，请按实际主键调整）
-- INSERT INTO `course_category` (`parent_id`, `name`, `type`, `sort`) VALUES
-- (1, '语音识别', 0, 1),
-- (1, '图像识别', 0, 2),
-- (2, '数据分析', 0, 1),
-- (2, '数据可视化', 0, 2);

-- 职位分类示例：一级分类
INSERT INTO `job_category` (`parent_id`, `name`, `sort`) VALUES
(0, '技术',           1),
(0, '产品',           2),
(0, '设计',           3),
(0, '运营',           4),
(0, '市场',           5),
(0, '人事/财务/行政', 6),
(0, '高级管理',       7);

-- 职位分类示例：二级分类（parent_id 对应上方一级分类，请按实际主键调整）
-- INSERT INTO `job_category` (`parent_id`, `name`, `sort`) VALUES
-- (1, '后端开发', 1),
-- (1, '前端开发', 2),
-- (1, '5G无线网络优化', 3),
-- (2, '产品经理', 1);
