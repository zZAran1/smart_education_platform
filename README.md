# 智慧教育平台 SmartEducationPlatform

> 面向「学 · 练 · 证 · 就」一体化的在线教育 + 实习就业平台
> 后端：Spring Boot 3 + MyBatis-Plus + MySQL + Redis ｜ 前端：Vue 3 + Vite + TypeScript

---

## 一、项目简介

本项目是一个**在线学习 + 实习就业**双主线的教育平台。学员可以在平台上浏览与报名理论/实训/认证三类课程、按章节学习并查看学习进度、评价课程、发起答疑；也可以在实习就业板块按技术方向筛选职位、投递简历、申请数字人面试。管理员在后台统一维护用户、课程与章节目录、职位与公司、订单、评论与答疑等内容。

平台区分三种角色（`role` 字段）：

| 值 | 角色 | 说明 |
| --- | --- | --- |
| 0 | 学员 | 前台使用者：学习、求职、查看个人记录 |
| 1 | 教师 | 预留角色，当前权限同普通登录用户 |
| 2 | 管理员 | 可进入 `/admin` 后台管理 |

**两条核心业务闭环：**

1. **课程学习闭环**：课程中心筛选/搜索 → 课程详情（简介 / 目录 / 评论 / 答疑）→ 报名（免费直接开通，收费下单支付）→ 逐章标记学完 → 学习进度更新 → 个人中心「我的课程 / 我的订单」可复查
2. **求职就业闭环**：实习就业按分类筛选 → 职位详情 → 收藏 / 投递 → 申请数字人面试 → 个人中心「我的投递 / 我的面试」可复查 → 管理员在后台「申请管理 / 面试管理」审阅与流转状态

---

## 二、技术栈

### 后端

| 分类 | 选型 | 版本 |
| --- | --- | --- |
| 语言 / 运行时 | Java | 21 |
| 框架 | Spring Boot | 3.4.4 |
| ORM | MyBatis-Plus（分页插件 + 逻辑删除） | 3.5.10 |
| 数据库 | MySQL（InnoDB / utf8mb4） | 8.0+ |
| 缓存 | Redis（Spring Cache 注解缓存 + StringRedisTemplate） | — |
| 认证 | JWT（jjwt） | 0.12.6 |
| 密码加密 | BCrypt（jbcrypt + spring-security-crypto） | 0.4 |
| 图形验证码 | Hutool CircleCaptcha | 5.8.25 |
| 对象转换 | MapStruct | 1.6.3 |
| 其它 | Lombok、spring-boot-devtools、spring-boot-starter-validation | — |

### 前端

| 分类 | 选型 | 版本 |
| --- | --- | --- |
| 框架 | Vue 3（`<script setup>` 组合式 API） | 3.5 |
| 构建 | Vite | 8 |
| 语言 | TypeScript（`vue-tsc` 类型检查） | 6 |
| 路由 | vue-router（全局前置守卫做登录/角色拦截） | 4 |
| 请求 | axios（统一封装 `Result` 拆包、Token 注入、1002 自动跳登录） | 1.20 |
| 状态管理 | 无第三方库，使用 `reactive` 模块单例 + localStorage 持久化 | — |
| 测试 | vitest + @vue/test-utils + jsdom | 5 |

---

## 三、功能模块

### 用户与认证

- 图形验证码登录（验证码一次性，存 Redis `captcha:{uuid}`，TTL 300s）
- 注册、重置密码（邮箱/手机号 + 验证码，课程设计场景下验证码仅输出到后端日志）
- 个人资料查看与修改（昵称/姓名）、头像上传（JPG/PNG，≤2MB）
- 退出登录（Token 加入黑名单即时失效）

### 课程模块

- 两级分类树（技术体系 / 技术方向），按课程类型过滤
- 课程中心分页：类型 + 分类 + 等级 + 免费筛选、关键词搜索（课程名/讲师名）、排序（最新/最热/好评）
- 课程详情：资源构成（课件/视频/实验）、评分与评价人次、学习人数
- 章节目录、收藏、报名（免费直接开通 / 收费生成待支付订单）、学习进度
- 课程评论（须已报名，每人每课一条，评分实时重算课程平均分）、课程答疑

### 支付与订单

- 收费课程下单 → 点击支付即成功（课程设计简化，不接入真实支付渠道）
- 订单状态机：待支付 / 已支付 / 已退款 / 已取消
- 支付幂等（同一订单重复支付被拦截）、归属校验（不能支付他人订单）、30 分钟超时校验
- 订单超时未支付由定时任务自动取消；管理员可对已支付订单退款

### 实习就业

- 两级职位分类，**一级分类筛选等价于「该一级 + 其下所有二级」**
- 职位分页（分类 + 职位名/公司名搜索）、职位详情（含公司信息）
- 职位收藏、投递（同一职位仅可投递一次）、数字人面试申请（幂等）
- 职位到期由定时任务自动下架

### 个人中心

「我的课程 / 我的订单 / 我的投递 / 我的面试」四个列表页，数据一律以 Token 中的身份为准（不接受前端传 `user_id`）。

### 后台管理

用户管理、课程管理（含章节目录 CRUD）、职位管理、订单管理（退款）、申请管理、面试管理、公司管理、评论管理、答疑管理，共 9 个模块。

---

## 四、目录结构

```
SmartEducationPlatform/
├── smart_education_platform.sql          # 数据库初始化脚本（建库建表 + 可选演示数据）
├── smart_education_platform_backend/     # 后端
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/example/smart_education_platform_backend/
│       │   │   ├── config/        # MyBatis-Plus / Redis / WebMvc 配置
│       │   │   ├── controller/    # User / Course / Study / Job / Order / Admin（6 个）
│       │   │   ├── service/       # 业务接口与 impl 实现
│       │   │   ├── mapper/        # MyBatis-Plus Mapper
│       │   │   ├── model/
│       │   │   │   ├── entity/    # 16 个数据库实体
│       │   │   │   ├── dto/       # 入参对象（含 @Valid 校验注解）
│       │   │   │   └── vo/        # 出参视图对象
│       │   │   ├── converter/     # MapStruct 对象转换
│       │   │   ├── exception/     # 分模块业务异常（对应不同业务码）
│       │   │   ├── handler/       # 全局异常处理
│       │   │   ├── interceptor/   # AuthInterceptor 统一鉴权
│       │   │   ├── task/          # 定时任务
│       │   │   ├── result/        # 统一响应 Result<T>
│       │   │   └── util/          # JWT / BCrypt / UserContext
│       │   └── resources/application.yml
│       └── test/
│           ├── e2e/               # Node.js 端到端脚本（真实 HTTP）
│           └── java/.../          # JUnit 集成测试
└── smart_education_platform_frontend/    # 前端
    ├── vite.config.ts                    # 开发代理：/api 与 /uploads → 8080
    ├── vitest.config.ts
    └── src/
        ├── api/           # request 封装 + 各模块接口（user/course/job/order/admin）
        ├── assets/        # base.css 设计令牌 + main.css 公共类
        ├── components/    # AppHeader / CourseCard / JobCard / Pagination / SideNav 等
        ├── composables/   # toast
        ├── router/        # 路由与守卫
        ├── stores/        # auth（登录态单例）
        ├── types/         # 与后端逐字段对齐的 DTO/VO 类型与常量
        └── views/         # 9 个页面
```

---

## 五、数据库设计

数据库：`smart_education_platform`，共 16 张表。统一约定：主键 `id BIGINT AUTO_INCREMENT`、逻辑删除 `deleted TINYINT DEFAULT 0`、审计字段 `created_at / updated_at`、金额 `DECIMAL(10,2)`。

| 模块 | 表 | 说明 |
| --- | --- | --- |
| 用户 | `users` | 用户与角色 |
| 课程 | `course` | 课程主表（类型/等级/免费/价格/评分/学习人数等） |
| 课程 | `course_category` | 两级课程分类（技术体系 / 技术方向） |
| 课程 | `course_chapter` | 章节目录（资源类型：课件/视频/实验，含时长） |
| 课程 | `course_comment` | 课程评论与评分 |
| 课程 | `course_qa` | 课程答疑（提问 / 回复） |
| 课程 | `course_collection` | 课程收藏 |
| 课程 | `course_enrollment` | 报名记录与学习进度 |
| 课程 | `study_record` | 章节学习记录 |
| 就业 | `company` | 招聘公司 |
| 就业 | `job_category` | 两级职位分类 |
| 就业 | `job` | 职位（含到期时间 `expire_time`） |
| 就业 | `job_collection` | 职位收藏（感兴趣） |
| 就业 | `job_application` | 职位投递记录 |
| 就业 | `ai_interview` | 数字人面试记录 |
| 订单 | `order_info` | 课程订单 |

> 脚本位于仓库根目录 `smart_education_platform.sql`，第 6 节为可选演示数据（使用 9001+ 高位主键，可重复执行）。**涉及表结构或脚本的改动请先说明原因再执行。**

---

## 六、接口一览

所有接口统一前缀 `/api`，除下表标注「匿名」外**均需携带 `Authorization: Bearer <token>`**。

### 用户 `/api/user`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/captcha` | 图形验证码（匿名） |
| POST | `/register` | 注册（匿名） |
| POST | `/login` | 登录（匿名） |
| POST | `/reset-code` | 下发重置密码验证码（匿名，验证码只打后端日志） |
| POST | `/reset-password` | 提交新密码（匿名） |
| GET | `/profile` | 当前用户资料 |
| PUT | `/profile` | 修改昵称/姓名 |
| PUT | `/avatar` | 上传头像（multipart，字段名 `file`） |
| POST | `/logout` | 退出登录（Token 进黑名单） |

### 课程 `/api/course`、学习 `/api/study`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/course/categories` | 课程分类树 |
| GET | `/course/page` | 课程分页（筛选 + 搜索 + 排序） |
| GET | `/course/{id}` | 课程详情 |
| GET | `/course/my` | 我的课程（含学习进度） |
| GET | `/course/{id}/chapters` | 章节目录 |
| POST | `/course/{id}/collect` | 收藏切换 |
| POST | `/course/{id}/enroll` | 报名 / 生成订单 |
| GET / POST | `/course/{id}/comments` | 评论分页 / 发表评论 |
| GET / POST | `/course/{id}/questions` | 答疑列表 / 提问 |
| POST | `/study/chapters/{chapterId}/finish` | 标记章节学完 |

### 职位 `/api/job`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/job/categories` | 职位分类树 |
| GET | `/job/page` | 职位分页（一级分类命中其全部子类） |
| GET | `/job/{id}` | 职位详情 |
| POST | `/job/{id}/collect` | 感兴趣切换 |
| POST | `/job/{id}/apply` | 投递职位 |
| POST | `/job/{id}/ai-interview` | 申请数字人面试 |
| GET | `/job/my-applications` | 我的投递 |
| GET | `/job/my-interviews` | 我的数字人面试 |

### 订单 `/api/order`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/order/{orderNo}/pay` | 支付订单 |
| GET | `/order/my` | 我的订单 |

### 后台 `/api/admin`（仅 role=2）

| 分组 | 接口 |
| --- | --- |
| 用户 | `GET /users`、`PUT /users/{id}/status` |
| 课程 | `GET /courses`、`POST /courses`、`PUT /courses/{id}`、`PUT /courses/{id}/status` |
| 章节 | `GET|POST /courses/{courseId}/chapters`、`PUT /chapters/{id}`、`DELETE /chapters/{id}` |
| 职位 | `GET /jobs`、`POST /jobs`、`PUT /jobs/{id}`、`PUT /jobs/{id}/status` |
| 公司 | `GET /companies`、`POST /companies`、`PUT /companies/{id}`、`DELETE /companies/{id}` |
| 订单 | `GET /orders`、`PUT /orders/{id}/refund` |
| 申请 | `GET /applications`、`PUT /applications/{id}/status` |
| 面试 | `GET /interviews`、`PUT /interviews/{id}` |
| 评论 | `GET /comments`、`DELETE /comments/{id}` |
| 答疑 | `GET /questions`、`PUT /questions/{id}/answer` |

---

## 七、统一响应与状态码

所有接口返回统一结构：

```json
{ "code": 200, "msg": null, "data": {} }
```

- `code = 200` 表示业务成功，`data` 为业务数据
- 业务失败通过 `code + msg` 区分，HTTP 状态码统一为 200（除框架层异常）
- 前端在响应拦截器里统一拆包：`200` 直接返回 `data`；`1002` 清除登录态并跳转登录页；其余弹出错误提示

| code | 含义 |
| --- | --- |
| 200 | 成功 |
| 400 | 参数校验失败 / 请求格式不正确 |
| 405 | 请求方法不支持 |
| 500 | 系统异常（「系统繁忙，请稍后重试」） |
| 1000 | 注册相关异常 |
| 1001 | 登录失败（账号或密码错误） |
| 1002 | 未登录 / Token 失效 / 账号被封禁 |
| 1003 | 验证码错误 |
| 1004 | 个人资料 / 头像相关异常 |
| 1010 | 课程业务异常（如重复报名） |
| 1011 | 职位业务异常（如重复投递） |
| 1012 | 订单业务异常（如订单状态不允许支付） |
| 1013 | 后台权限 / 后台业务异常 |

---

## 八、关键实现说明

### 1. 统一鉴权（AuthInterceptor）

- 拦截 `/api/**`，白名单仅 5 个认证接口 + `/error`
- 校验顺序：`Bearer` 格式 → Token 黑名单（登出/封禁即时失效）→ JWT 签名与有效期 → 角色合法性 → 用户封禁标记
- 通过后把 `userId / role` 写入 `UserContext`（ThreadLocal），业务层通过 `UserContext.getUserId()` 取当前用户，**请求结束后清理，避免线程复用串号**
- 后台接口在业务层二次校验 `role == 2`

### 2. 缓存策略

- Redis 注解缓存：`courseList` / `courseDetail` / `jobList` / `jobDetail` / `courseCategory` / `jobCategory`，全局 TTL 10 分钟，key 前缀 `edu:`
- 所有写操作通过 `@CacheEvict` 精确失效（列表 `allEntries`，详情按 id）
- 用户维度的数据（收藏、报名、订单、我的列表）**不缓存**

### 3. 定时任务

| 任务 | 触发 | 行为 |
| --- | --- | --- |
| 超时订单取消 | 每 60 秒 | 把创建超过 30 分钟仍未支付的订单置为「已取消」 |
| 到期职位下架 | 每日 0 点（`cron = 0 0 0 * * ?`） | 将 `expire_time` 已过期的在架职位下架，并清除对应缓存 |

### 4. 幂等与并发控制

- 收藏：切换语义（存在即取消），返回操作后的状态
- 报名 / 投递 / 面试申请：数据库唯一键为最终防线；投递额外加 Redis 短锁（30 秒）拦截短时间重复提交
- 支付：条件更新（`WHERE id = ? AND status = 0`）保证并发或重复点击时只有一次入账，其余请求幂等返回
- 评论：每人每课仅一条，评分与评价人次用单条 SQL 原子更新，避免并发丢更新

### 5. 学习进度

- `course_enrollment` 记录 `progress / finish_count / total_count`
- **报名或支付开通课程时**即按课程当前章节数初始化分母 `total_count`（否则会显示成 `0/0`）
- 学员标记章节学完后重算；管理员增删改章节后同步重算该课全部报名记录
- 进度只统计「现存章节」的学习记录，避免章节删除后残留记录导致进度超过 100%

### 6. 验证码

- 登录图形验证码：Hutool 生成，答案存 Redis `captcha:{uuid}`，TTL 300 秒，登录后即失效（一次性）
- 重置密码验证码：课程设计场景下**不返回给前端、不回传响应体**，仅打印到后端日志

---

## 九、本地运行

### 前置依赖

- JDK 21、Maven
- Node.js 22+（前端 `engines` 要求 `^22.18.0 || >=24.12.0`）
- MySQL 8.0+、Redis 6+

### 1. 准备数据库

```bash
mysql -uroot -p < smart_education_platform.sql
```

脚本会创建 `smart_education_platform` 库、16 张表，以及可选的演示数据（账号见脚本第 6 节注释）。

### 2. 启动后端

后端配置全部走环境变量（`application.yml` 中不写死敏感信息），**至少需要注入 `DB_PASSWORD` 与 `JWT_SECRET`**：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/smart_education_platform?...` | 数据库连接串 |
| `DB_USERNAME` | `root` | 数据库账号 |
| `DB_PASSWORD` | 无（必填） | 数据库密码 |
| `JWT_SECRET` | 无（必填） | Base64 编码密钥，建议 ≥32 字节 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | `localhost` / `6379` / 空 | Redis 连接 |
| `FILE_UPLOAD_PATH` | `/uploads` | 头像等上传文件存储目录 |
| `SERVER_PORT` | `8080` | 服务端口 |
| `SPRING_PROFILES_ACTIVE` | `local` | 激活的 profile |

```bash
# Windows PowerShell 示例
$env:DB_PASSWORD='<你的数据库密码>'
$env:JWT_SECRET='<Base64 密钥>'
mvn spring-boot:run
```

启动后访问 <http://localhost:8080/api/user/captcha> 能拿到验证码即表示后端正常。

### 3. 启动前端

```bash
cd smart_education_platform_frontend
npm install
npm run dev
```

访问 <http://localhost:5173>。开发服务器会把 `/api` 与 `/uploads` 一并代理到后端 8080（`vite.config.ts`）。

> **注意**：头像等上传文件由后端 `/uploads/**` 提供，如果只代理 `/api`，浏览器请求 `/uploads/...` 会命中 Vite 的 SPA 回退拿到 `index.html`，导致图片无法显示。生产部署（nginx 等）同样需要把 `/uploads/**` 路由到后端。

### 4. 演示账号

见 `smart_education_platform.sql` 第 6 节，初始密码统一为 `admin123456`：

| 账号 | 角色 |
| --- | --- |
| `admin` | 管理员（可进后台） |
| `student01` | 学员 |
| `student02` | 学员 |

> 若已通过「忘记密码」修改过某个账号的密码，请以实际密码为准。

---

## 十、测试

项目已建立三层测试资产，均针对真实运行环境（真实 MySQL / Redis / HTTP），不是 Mock。

### 1. 后端端到端测试（真实 HTTP，主回归手段）

```bash
cd smart_education_platform_backend
node src/test/e2e/api-flow-test.mjs
```

- 覆盖：鉴权与越权、课程学习闭环、职位求职闭环、后台管理闭环、学员「我的」记录、职位分类筛选语义，共 **76 项断言**
- 通过 `redis-cli` 读取 Redis 中的图形验证码答案来绕过验证码，实现自动化登录
- 用例设计为**可重复执行**：报名/评论/投递等会重复的操作采用「首次成功或已存在的幂等断言」

### 2. 后端 JUnit 集成测试

`src/test/java/.../AuthIntegrationTest.java`（7 项）：白名单匿名可访问、无 Token / 伪造 Token 被拒、学员访问后台被拒、管理员可访问。

- 运行前需注入 `DB_PASSWORD`、`JWT_SECRET`
- 若 Maven 本地仓库不可写导致 surefire 插件缺失，可用「导出测试 classpath + JUnit Platform Launcher」的方式离线运行（详见项目记忆中的记录）

### 3. 前端单元测试与构建

```bash
cd smart_education_platform_frontend
npx vitest run     # 20 项：数值兜底、时长/薪资格式化、业务状态常量映射、登录态持久化
npm run build      # vue-tsc 类型检查 + 生产构建
```

### 4. 辅助工具

`src/test/e2e/get-token.mjs`：通过「验证码 → 读 Redis 答案 → 登录」输出 Token，供浏览器端到端测试注入登录态使用。

```bash
node src/test/e2e/get-token.mjs student01          # 输出 Token
node src/test/e2e/get-token.mjs admin --json       # 输出含用户信息的整行 JSON
```

> 注意：测试脚本中的账号密码为硬编码（默认 `admin123456`）。若演示账号密码被修改，需同步调整脚本或改为环境变量注入。

---

## 十一、开发约定

- **修改数据库表结构或 `smart_education_platform.sql` 前先说明原因**，不擅自执行 DDL / DML
- 后端分层固定为 `Controller（只做参数校验与结果包装）→ Service → Mapper`，业务异常按模块拆分，对应不同业务码
- DTO 使用 `jakarta.validation` 注解做入参校验，Controller 上必须加 `@Valid` 才会生效
- 用户身份一律从 `UserContext` 取，**接口不接收前端传入的 `user_id`**，避免越权
- 前端不使用第三方状态管理库，跨页面共享状态用 `reactive` 模块单例（见 `stores/auth.ts`）
- 前端所有类型定义与后端 VO/DTO 逐字段对齐，统一放在 `src/types/api.ts`，避免字段漂移
