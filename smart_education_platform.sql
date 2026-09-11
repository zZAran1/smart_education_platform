-- =============================================================
-- 智慧教育平台 数据库初始化脚本
-- 依据：《系统设计文档 V1.2》第 4 章 数据库设计
-- 数据库：MySQL 8.0+，引擎 InnoDB，字符集 utf8mb4
-- 约定：主键 id BIGINT AUTO_INCREMENT；逻辑删除 deleted TINYINT DEFAULT 0；
--       审计字段 created_at / updated_at；金额 DECIMAL(10,2)
-- =============================================================

CREATE DATABASE IF NOT EXISTS `smart_education_platform`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE `smart_education_platform`;

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
  `trade_no`   VARCHAR(64)   DEFAULT NULL COMMENT '第三方支付流水号',
  `pay_time`   DATETIME      DEFAULT NULL COMMENT '支付时间',
  `notify_time` DATETIME     DEFAULT NULL COMMENT '支付回调时间',
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

-- =============================================================
-- 6. 演示数据（可选）
--    用途：课程设计演示 + 前端列表/详情/后台效果验证
--    约定：
--      1) 全部使用 9001+ 高位主键，每节先 DELETE 再 INSERT，可重复执行；
--      2) 不影响你自己录入的数据；
--      3) 账号密码统一为 admin123456（BCrypt 已加密存储）
--         · admin      系统管理员（role=2，可进后台管理）
--         · student01  学员（role=0）
--         · student02  学员（role=0）
--    注意：请确认当前数据库与后端 application.yml 中连接的库一致。
-- =============================================================

USE `edu_platform`;

-- ---------- 6.1 课程二级分类（技术方向） ----------
DELETE FROM `course_chapter` WHERE `id` BETWEEN 9101 AND 9200;
DELETE FROM `course`         WHERE `id` BETWEEN 9001 AND 9010;
DELETE FROM `course_category` WHERE `id` BETWEEN 9001 AND 9010;

INSERT INTO `course_category` (`id`, `parent_id`, `name`, `type`, `sort`)
SELECT 9001, id, '机器学习',   0, 1 FROM `course_category` WHERE `parent_id`=0 AND `name`='人工智能' LIMIT 1;
INSERT INTO `course_category` (`id`, `parent_id`, `name`, `type`, `sort`)
SELECT 9002, id, '语音识别',   0, 2 FROM `course_category` WHERE `parent_id`=0 AND `name`='人工智能' LIMIT 1;
INSERT INTO `course_category` (`id`, `parent_id`, `name`, `type`, `sort`)
SELECT 9003, id, '数据分析',   0, 1 FROM `course_category` WHERE `parent_id`=0 AND `name`='大数据'   LIMIT 1;
INSERT INTO `course_category` (`id`, `parent_id`, `name`, `type`, `sort`)
SELECT 9004, id, '数据可视化', 0, 2 FROM `course_category` WHERE `parent_id`=0 AND `name`='大数据'   LIMIT 1;
INSERT INTO `course_category` (`id`, `parent_id`, `name`, `type`, `sort`)
SELECT 9005, id, '云原生',     0, 1 FROM `course_category` WHERE `parent_id`=0 AND `name`='云计算'   LIMIT 1;
INSERT INTO `course_category` (`id`, `parent_id`, `name`, `type`, `sort`)
SELECT 9006, id, '鸿蒙应用开发', 0, 1 FROM `course_category` WHERE `parent_id`=0 AND `name`='鸿蒙'   LIMIT 1;
INSERT INTO `course_category` (`id`, `parent_id`, `name`, `type`, `sort`)
SELECT 9007, id, '5G网络优化', 0, 1 FROM `course_category` WHERE `parent_id`=0 AND `name`='通信'     LIMIT 1;

-- ---------- 6.2 课程（覆盖理论/实训/认证，含免费与付费，便于验证筛选与排序） ----------
SET @sys_ai    = (SELECT id FROM `course_category` WHERE `parent_id`=0 AND `name`='人工智能' LIMIT 1);
SET @sys_big   = (SELECT id FROM `course_category` WHERE `parent_id`=0 AND `name`='大数据'   LIMIT 1);
SET @sys_cloud = (SELECT id FROM `course_category` WHERE `parent_id`=0 AND `name`='云计算'   LIMIT 1);
SET @sys_hm    = (SELECT id FROM `course_category` WHERE `parent_id`=0 AND `name`='鸿蒙'     LIMIT 1);
SET @sys_com   = (SELECT id FROM `course_category` WHERE `parent_id`=0 AND `name`='通信'     LIMIT 1);

INSERT INTO `course`
(`id`, `title`, `type`, `tech_system_id`, `tech_direction_id`, `level`, `teacher_id`, `teacher_name`,
 `courseware_count`, `video_count`, `lab_count`, `is_free`, `price`, `score`, `rating_count`, `student_count`,
 `intro`, `target`, `status`, `publish_time`) VALUES
(9001, '人工智能导论', 0, @sys_ai, 9001, 0, NULL, '李文博', 12, 24, 0, 1, 0.00, 4.8, 126, 1280,
 '从零开始理解人工智能的核心概念、发展脉络与典型应用场景，建立完整的知识框架，为后续深入学习打下基础。',
 '掌握人工智能基本概念与主要技术分支，能够判断实际问题适用的 AI 方法。', 1, NOW() - INTERVAL 30 DAY),
(9002, '深度学习与神经网络实战', 0, @sys_ai, 9001, 2, NULL, '李文博', 18, 36, 4, 0, 199.00, 4.9, 88, 860,
 '围绕卷积网络、循环网络与注意力机制展开，配套真实数据集实战，讲解模型训练、调参与部署的完整链路。',
 '能够独立完成图像分类与文本分类任务，理解主流网络结构的设计动机。', 1, NOW() - INTERVAL 20 DAY),
(9003, '大数据分析基础', 0, @sys_big, 9003, 0, NULL, '张若曦', 14, 20, 2, 1, 0.00, 4.5, 210, 2100,
 '讲解数据采集、清洗、统计与建模的基本方法，使用典型业务数据集贯穿全流程。',
 '掌握数据分析基本流程，能够使用常用工具完成描述性统计与简单建模。', 1, NOW() - INTERVAL 60 DAY),
(9004, '数据可视化设计', 0, @sys_big, 9004, 1, NULL, '王思远', 10, 16, 3, 0, 129.00, 4.6, 54, 430,
 '从信息层级、图表选型到交互细节，系统讲解如何把数据讲成一个清晰可信的故事。',
 '能够针对不同业务场景选择恰当图表，产出规范、易读的可视化作品。', 1, NOW() - INTERVAL 15 DAY),
(9005, '鸿蒙应用开发实训', 1, @sys_hm, 9006, 1, NULL, '陈宇', 6, 8, 12, 1, 0.00, 4.7, 96, 950,
 '基于鸿蒙开发套件完成一个完整应用，覆盖声明式 UI、状态管理与设备能力调用。',
 '能够独立搭建鸿蒙应用工程，完成页面开发与基础设备能力集成。', 1, NOW() - INTERVAL 10 DAY),
(9006, '云原生容器化实验', 1, @sys_cloud, 9005, 2, NULL, '赵倩', 8, 10, 14, 0, 259.00, 4.4, 32, 320,
 '通过一组递进式实验掌握容器镜像构建、编排部署、服务发现与弹性扩缩容。',
 '掌握容器化交付全流程，能够独立完成微服务的部署与运维。', 1, NOW() - INTERVAL 8 DAY),
(9007, 'HCIA-AI 认证冲刺', 2, @sys_ai, 9001, 3, NULL, '李文博', 20, 15, 2, 0, 399.00, 4.8, 61, 610,
 '对照认证考纲梳理核心考点，配合真题演练与错题精讲，帮助学员高效通过考试。',
 '系统掌握考纲要求的知识点与解题方法，具备通过认证考试的能力。', 1, NOW() - INTERVAL 5 DAY),
(9008, '通信工程认证基础', 2, @sys_com, 9007, 0, NULL, '孙浩然', 16, 12, 1, 1, 0.00, 4.2, 18, 180,
 '面向通信工程认证入门，讲解网络架构、协议基础与典型工程场景。',
 '理解通信网络基本架构与关键协议，具备认证考试的基础知识储备。', 1, NOW() - INTERVAL 3 DAY);

-- ---------- 6.3 课程目录（章节/资源） ----------
INSERT INTO `course_chapter` (`id`, `course_id`, `title`, `resource_type`, `duration`, `sort`) VALUES
(9101, 9001, '第1讲 人工智能的起源与三次浪潮', 1, 1620, 1),
(9102, 9001, '第2讲 机器学习、深度学习与强化学习的关系', 1, 1860, 2),
(9103, 9001, '第3讲 典型行业应用案例解析', 0, 0, 3),
(9111, 9002, '第1讲 神经网络基础与前向传播', 1, 2100, 1),
(9112, 9002, '第2讲 卷积神经网络与图像分类实战', 1, 2880, 2),
(9113, 9002, '实验一 手写数字识别模型训练', 2, 3600, 3),
(9114, 9002, '第3讲 注意力机制与 Transformer', 1, 2640, 4),
(9121, 9003, '第1讲 数据分析流程与常见误区', 1, 1500, 1),
(9122, 9003, '第2讲 数据清洗与特征工程', 1, 2040, 2),
(9123, 9003, '实验一 电商订单数据分析', 2, 2700, 3),
(9131, 9004, '第1讲 信息层级与视觉编码', 1, 1680, 1),
(9132, 9004, '第2讲 图表选型与配色规范', 0, 0, 2),
(9133, 9004, '实验一 销售看板设计', 2, 3000, 3),
(9141, 9005, '第1讲 鸿蒙应用工程结构', 1, 1440, 1),
(9142, 9005, '实验一 声明式 UI 页面开发', 2, 3300, 2),
(9143, 9005, '实验二 设备能力调用与状态管理', 2, 3600, 3),
(9151, 9006, '第1讲 容器镜像与分层原理', 1, 1560, 1),
(9152, 9006, '实验一 镜像构建与仓库推送', 2, 3000, 2),
(9153, 9006, '实验二 服务编排与弹性扩缩容', 2, 4200, 3),
(9161, 9007, '第1讲 考纲解读与学习路径', 1, 1200, 1),
(9162, 9007, '第2讲 核心考点精讲', 1, 3600, 2),
(9163, 9007, '第3讲 真题演练与错题分析', 0, 0, 3),
(9171, 9008, '第1讲 通信网络整体架构', 1, 1800, 1),
(9172, 9008, '第2讲 关键协议与工程场景', 1, 2100, 2);

-- ---------- 6.4 公司 ----------
DELETE FROM `company` WHERE `id` BETWEEN 9001 AND 9010;

INSERT INTO `company` (`id`, `name`, `industry`, `scale`, `region`, `intro`) VALUES
(9001, '星辰智能科技有限公司', '人工智能', '500-999人', '深圳',
 '专注计算机视觉与语音识别的 AI 解决方案提供商，服务金融、安防、教育等行业客户。'),
(9002, '云梯云计算有限公司', '云计算', '1000-4999人', '杭州',
 '提供云原生基础设施与容器编排平台，服务互联网与制造业客户的数字化转型。'),
(9003, '鸿蒙生态技术有限公司', '智能终端', '100-499人', '珠海',
 '聚焦鸿蒙生态应用开发与智能硬件互联方案，团队来自头部终端厂商。'),
(9004, '数聚数据服务有限公司', '大数据', '300-499人', '成都',
 '面向零售与政务客户的数据中台与可视化分析服务商。');

-- ---------- 6.5 职位（含二级分类与到期时间，便于验证筛选、分页与自动下架） ----------
DELETE FROM `job_category` WHERE `id` BETWEEN 9001 AND 9010;

INSERT INTO `job_category` (`id`, `parent_id`, `name`, `sort`)
SELECT 9001, id, '后端开发',        1 FROM `job_category` WHERE `parent_id`=0 AND `name`='技术' LIMIT 1;
INSERT INTO `job_category` (`id`, `parent_id`, `name`, `sort`)
SELECT 9002, id, '前端开发',        2 FROM `job_category` WHERE `parent_id`=0 AND `name`='技术' LIMIT 1;
INSERT INTO `job_category` (`id`, `parent_id`, `name`, `sort`)
SELECT 9003, id, '5G无线网络优化',  3 FROM `job_category` WHERE `parent_id`=0 AND `name`='技术' LIMIT 1;
INSERT INTO `job_category` (`id`, `parent_id`, `name`, `sort`)
SELECT 9004, id, '产品经理',        1 FROM `job_category` WHERE `parent_id`=0 AND `name`='产品' LIMIT 1;
INSERT INTO `job_category` (`id`, `parent_id`, `name`, `sort`)
SELECT 9005, id, '视觉设计',        1 FROM `job_category` WHERE `parent_id`=0 AND `name`='设计' LIMIT 1;
INSERT INTO `job_category` (`id`, `parent_id`, `name`, `sort`)
SELECT 9006, id, '内容运营',        1 FROM `job_category` WHERE `parent_id`=0 AND `name`='运营' LIMIT 1;

DELETE FROM `job` WHERE `id` BETWEEN 9001 AND 9010;

INSERT INTO `job`
(`id`, `title`, `city`, `address`, `salary_min`, `salary_max`, `headcount`, `company_id`, `category_id`,
 `description`, `requirement`, `status`, `expire_time`) VALUES
(9001, '算法工程师（计算机视觉）', '深圳', '南山区科技园南区 A 座 12 层', 25, 40, 3, 9001, 9001,
 '负责计算机视觉算法的设计、训练与优化，参与核心产品从算法原型到工程落地的全过程。\n岗位职责：\n1. 目标检测/分割模型的训练与调优；\n2. 模型推理性能优化与工程化部署；\n3. 与产品、后端协作完成需求落地。',
 '任职要求：\n1. 本科及以上学历，计算机相关专业；\n2. 熟悉 PyTorch 或 TensorFlow；\n3. 有视觉项目落地经验者优先。', 1, NOW() + INTERVAL 60 DAY),
(9002, 'Java 后端开发工程师', '杭州', '余杭区文一西路 969 号', 20, 35, 5, 9002, 9001,
 '参与云原生平台后端服务的设计与开发，负责核心模块的架构演进与性能优化。\n岗位职责：\n1. 后端服务设计与编码；\n2. 接口性能与稳定性优化；\n3. 参与技术方案评审。',
 '任职要求：\n1. 熟练掌握 Java 与 Spring 生态；\n2. 熟悉 MySQL、Redis 与消息队列；\n3. 有分布式系统经验者优先。', 1, NOW() + INTERVAL 45 DAY),
(9003, '鸿蒙应用开发工程师', '珠海', '高新区唐家湾镇软件园 3 栋', 18, 30, 2, 9003, 9002,
 '负责鸿蒙生态应用的开发与维护，参与新特性预研与跨设备适配。\n岗位职责：\n1. 应用页面与业务逻辑开发；\n2. 设备能力集成与性能调优；\n3. 参与技术文档沉淀。',
 '任职要求：\n1. 熟悉 ArkTS / 声明式 UI；\n2. 有移动端开发经验；\n3. 对鸿蒙生态有热情。', 1, NOW() + INTERVAL 30 DAY),
(9004, '数据产品经理', '成都', '高新区天府大道中段 1268 号', 15, 25, 1, 9004, 9004,
 '负责数据产品的需求调研、方案设计与落地推进，串联业务与研发团队。',
 '任职要求：\n1. 有数据类产品设计经验；\n2. 具备较强的逻辑与沟通能力；\n3. 熟悉 SQL 与常用分析工具。', 1, NOW() + INTERVAL 50 DAY),
(9005, '视觉设计师', '深圳', '南山区科技园南区 A 座 9 层', 12, 20, 2, 9001, 9005,
 '负责产品界面的视觉设计与设计规范维护，参与品牌视觉表达。',
 '任职要求：\n1. 具备扎实的平面与界面设计功底；\n2. 熟练使用主流设计工具；\n3. 有 B 端产品设计经验者优先。', 1, NOW() + INTERVAL 25 DAY),
(9006, '5G 无线网络优化工程师', '广州', '黄埔区科学城科学大道 162 号', 14, 22, 4, 9002, 9003,
 '负责 5G 无线网络的覆盖优化与性能提升，输出优化方案并跟进实施。',
 '任职要求：\n1. 熟悉 5G 网络架构与关键指标；\n2. 能适应阶段性出差；\n3. 有运营商项目经验者优先。', 1, NOW() - INTERVAL 1 DAY);

-- ---------- 6.6 用户（密码统一为 admin123456） ----------
DELETE FROM `users` WHERE `username` IN ('admin', 'student01', 'student02');

INSERT INTO `users` (`username`, `password`, `nickname`, `real_name`, `email`, `phone`, `role`, `status`) VALUES
('admin',     '$2a$10$gvBTaAaY95/nOWKo8F4qv.SOgpsXL1rmDfqDv9jCUf5KIrk25xd26', '系统管理员', '管理员', 'admin@edu.com',     '13800000000', 2, 1),
('student01', '$2a$10$gvBTaAaY95/nOWKo8F4qv.SOgpsXL1rmDfqDv9jCUf5KIrk25xd26', '小明同学',   '李明',   'student01@edu.com', '13800000001', 0, 1),
('student02', '$2a$10$gvBTaAaY95/nOWKo8F4qv.SOgpsXL1rmDfqDv9jCUf5KIrk25xd26', '小美',       '王美',   'student02@edu.com', '13800000002', 0, 1);

