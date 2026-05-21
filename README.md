# 在线考试系统 · 后端（Spring Boot）

> 本目录是 **API 服务**。完整业务说明见仓库根目录 [README.md](../README.md)。  
> **维护约定**：修改本目录内会影响启动、配置、接口或数据库的代码时，请同步更新本文（由 Cursor 规则 `readme-sync-backend.mdc` 自动提醒 Agent）。

---

## 快速启动

### 环境要求

| 软件 | 版本 |
|------|------|
| JDK | 8 或 11 |
| Maven | 3.x |
| MySQL | 5.7+，库名 `db_exam` |
| Redis | 任意稳定版（Windows 可用 Memurai） |

### 初始化数据库

```sql
CREATE DATABASE db_exam DEFAULT CHARACTER SET utf8mb4;
```

在 MySQL 中执行 `sql/db_exam.sql`。若从旧库升级，按需执行 `sql/` 下以 `alter_` 开头的脚本（详见根 README「试题图片」「材料题」等章节）。  
**注册邀请码**：对已有库执行一次 `sql/create_t_invite_code.sql`（教师/管理员自助注册须凭码）。  
**进入考试报 `Unknown column 'score' in 'field list'`**：对已有库执行一次 `sql/alter_t_exam_qu_answer_score.sql`（为 `t_exam_qu_answer` 增加 `score` 列）。

### 启动

```bash
cd online-exam-system-backend
mvn spring-boot:run
```

| 项目 | 默认值 |
|------|--------|
| 端口 | **8080** |
| 接口前缀 | `/api` |
| 接口文档 | http://127.0.0.1:8080/doc.html |

启动前请确认 **MySQL、Redis** 已运行。

---

## 配置说明

主配置：`src/main/resources/application.yml`（激活 `dev` / `prod`）。  
开发环境：`src/main/resources/application-dev.yml`。

### 默认连接（dev）

| 服务 | 地址 | 账号 | 密码 |
|------|------|------|------|
| MySQL | `127.0.0.1:3306/db_exam` | `root` | 见 `application-dev.yml` 中 `MYSQL_PASSWORD` 默认值 |
| Redis | `127.0.0.1:6379` | — | 无 |

**不想改 yml 文件时**：在系统环境变量中设置变量名，可覆盖默认值。变量名与说明见本目录 **[env.example](env.example)**。

### 常用环境变量

| 变量 | 用途 |
|------|------|
| `MYSQL_URL` / `MYSQL_USERNAME` / `MYSQL_PASSWORD` | 数据库连接 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis |
| `JWT_SECRET` | 登录 Token 签名（生产务必修改） |
| `EXAM_AES_KEY` / `EXAM_AES_IV` | 登录密码传输加密，**须与前端** `VUE_APP_CRYPTO_KEY` / `VUE_APP_CRYPTO_IV` **一致**（各 16 字符） |
| `STORAGE_TYPE` | `local`（默认，本地 `uploads/`）或 `minio` / `aliyun` |
| `COZE_API_TOKEN` 等 | 仅在使用 Coze/Dify/LLM 相关能力时需要 |

---

## 目录结构

```
online-exam-system-backend/
├── pom.xml
├── env.example                 # 环境变量说明（不自动加载，供人工配置）
├── sql/                        # 建库与增量脚本
│   ├── db_exam.sql
│   └── alter_*.sql
└── src/main/
    ├── java/cn/org/alan/exam/
    │   ├── ExamApplication.java    # 启动类
    │   ├── config/                 # Security、Redis、Swagger 等
    │   ├── controller/             # REST API
    │   ├── service/impl/           # 业务逻辑
    │   ├── mapper/                 # MyBatis-Plus
    │   ├── model/                  # entity / vo / form / dto
    │   ├── filter/                 # JWT 过滤器
    │   └── websocket/              # WebSocket 推送
    └── resources/
        ├── application.yml
        ├── application-dev.yml
        ├── application-prod.yml
        └── mapper/*.xml
```

---

## API 模块一览

| 模块 | 路径前缀 | Controller |
|------|----------|------------|
| 认证 | `/api/auths` | AuthController |
| 邀请码 | `/api/invite-codes` | InviteCodeController（仅管理员） |
| 用户 | `/api/user` | UserController |
| 班级 | `/api/grades` | GradeController |
| 题目 | `/api/questions` | QuestionController |
| 题库 | `/api/repo` | RepoController |
| 分类 | `/api/category` | CategoryController |
| 考试 | `/api/exams` | ExamController |
| 答卷 | `/api/answers` | AnswerController |
| 评分 | `/api/score` | ScoreController |
| 记录 | `/api/records` | RecordController |
| 练习 | `/api/exercises` | ExerciseController |
| 错题本 | `/api/userbooks` | UserBookController |
| 讨论 | `/api/discussion`、`/api/reply` | DiscussionController、ReplyController |
| 公告 | `/api/notices` | NoticeController |
| 统计 | `/api/stat` | StatController |
| 证书 | `/api/certificate` | CertificateController |
| 文件 | `/api/upload` | FileController |
| 日志 | `/api/log` | LogController |

完整参数与示例见 Knife4j：http://127.0.0.1:8080/doc.html

**近期接口补充**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/records/exam/paging` | 学生：已交卷记录（含 `whetherMark=0` 待批改） |
| GET | `/api/records/exam/detail` | 考后详情，含每题 `totalScore` / `quScore` |
| GET | `/api/answers/exam/absent` | 教师：某场考试缺考学生（未交卷；可选 `gradeId`、姓名） |
| POST | `/api/exams/random-preview` | 教师：随机组卷预览（按题库与题型数量抽题，返回题目列表，不落库） |
| POST | `/api/auths/register` | 注册：Body 含 `roleId`（1/2/3）；教师/管理员须 `inviteCode` |
| POST | `/api/invite-codes` | 管理员：生成邀请码 |
| GET | `/api/invite-codes/paging` | 管理员：邀请码分页 |
| PUT | `/api/invite-codes/{id}/disable` | 管理员：禁用邀请码 |
| DELETE | `/api/invite-codes/{ids}` | 管理员：批量删除邀请码 |

创建考试 `POST /api/exams`：随机模式（`addQuype=1`）若同时提交 `quIds` 与 `quScores`（与预览列表一致），则按确认后的题目与分值组卷，不再重新洗牌。

---

## 推荐阅读顺序（改代码前）

1. `ExamApplication.java` — 入口
2. `config/SecurityConfig.java` + `filter/VerifyTokenFilter.java` — JWT
3. `controller/AuthController.java` + `service/impl/AuthServiceImpl.java` — 登录
4. `service/impl/ExamServiceImpl.java` — 考试全流程
5. `service/impl/AutoScoringServiceImpl.java` / `ManualScoreServiceImpl.java` — 判分
6. `mapper/` + `model/entity/` — 表结构

---

## 常见问题

### 找不到 `mvn`

安装 Maven 并加入 PATH，新开终端执行 `mvn -version` 验证。

### 启动报数据库 / Redis 连接失败

- 确认 MySQL、Redis 服务已启动
- 确认已创建 `db_exam` 并执行 `sql/db_exam.sql`
- 密码与 `application-dev.yml` 或环境变量 `MYSQL_PASSWORD` 一致

### 前端登录报「用户名或密码错误」但账号正确

检查 `EXAM_AES_KEY` / `EXAM_AES_IV` 是否与前端 `VUE_APP_CRYPTO_*` 完全一致。

### Maven 依赖下载 SSL 失败

配置阿里云镜像，见根 [README.md](../README.md)「问题 3」。

---

## 技术栈

Spring Boot 2 · MyBatis-Plus · Spring Security + JWT · Redis · WebSocket · MySQL · Knife4j

---

*最后更新：2026-05-21*
