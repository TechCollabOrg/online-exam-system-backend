# 在线考试系统 · 后端（Spring Boot）

> 本目录是 **API 服务**。完整业务说明见仓库根目录 [README.md](../README.md)。  
> **维护约定**：修改本目录内会影响启动、配置、接口或数据库的代码时，请同步更新本文（由 Cursor 规则 `readme-sync-backend.mdc` 自动提醒 Agent）。

---

## 快速启动

### 环境要求

| 软件 | 版本 |
|------|------|
| JDK | **推荐 8 或 11**；若使用 17/21，见下方「Java 17+ 启动」 |
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
**考试管理报 `Unknown column 'compound_count' in 'field list'`**：对已有库执行一次 `sql/alter_t_exam_compound_type.sql`（为 `t_exam` 增加 `compound_count`、`compound_score` 列），然后重启后端。  
**组卷分值要支持小数（如 0.5、2.5 分）且旧库此前按「整分」直存**：对已有库执行一次 `sql/alter_score_storage_x100.sql`（或 `sql/upgrade_legacy_db.sql` 已包含该步骤），将历史分值统一为「库内整数 = 展示分 × 100」。新装库直接执行 `db_exam.sql` 即可；**勿重复执行**升级脚本。执行前请备份。
**学生专业字段**：对已有库执行一次 `sql/alter_t_user_major.sql`（或 `upgrade_legacy_db.sql` 已包含），为 `t_user` 增加 `major` 列。  
**按学生发布考试**：对已有库执行一次 `sql/alter_t_exam_user.sql`（或 `upgrade_legacy_db.sql` 已包含），为 `t_exam` 增加 `target_type` 并创建 `t_exam_user` 表。  
**刷题/结束刷题后记录为空，或接口报 `Unknown column 'audio' in 'field list'`**：对已有库执行一次 `sql/alter_t_question_audio.sql`（为 `t_question` 增加 `audio` 列），然后重启后端。

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
| POST | `/api/auths/register` | 注册：Body 含 `roleId`（1/2/3）；学生须 `major`；教师/管理员须 `inviteCode` |
| POST | `/api/invite-codes` | 管理员：生成邀请码 |
| GET | `/api/invite-codes/paging` | 管理员：邀请码分页 |
| PUT | `/api/invite-codes/{id}/disable` | 管理员：禁用邀请码 |
| DELETE | `/api/invite-codes/{ids}` | 管理员：批量删除邀请码 |
| PUT | `/api/user/{id}/profile` | 管理员：维护学生班级（`gradeId`，可空）与专业（`major`） |
| GET | `/api/discussion/query/page/admin` | 管理员：全站讨论分页（可选 `title`、`gradeId`） |
| GET | `/api/repo/{id}/knowledge-tree` | 教师/管理员：获取题库已保存的知识树 |
| POST | `/api/repo/{id}/knowledge-tree/generate` | 教师/管理员：AI 分析题目并生成/覆盖知识树 |
| GET | `/api/repo/{id}/knowledge-points` | 教师/管理员：知识树下拉选项（按知识点筛题） |
| GET | `/api/questions/paging` | 新增可选参数 `knowledgePointPath`（须同时传 `repoId`） |
| POST | `/api/questions/uploadAudio` | 教师/管理员：上传试题听力音频（multipart `file`，单文件 ≤ 50MB，见 `spring.servlet.multipart`） |
| POST | `/api/questions/ai-import/{repoId}` | 教师/管理员：AI 智能导入（上传 `.docx`/`.md`，AI 按 `sql/JSON_QUESTION_IMPORT_SPEC.md` 转 JSON 后入库；multipart `file`；超时建议 ≥ 300s） |

AI 试题导入使用「AI 试题导入」功能配置（`question_import`）；首次部署执行 `sql/alter_t_ai_feature_config_question_import.sql`（或已合并进 `alter_t_ai_feature_config.sql`）。

创建考试 `POST /api/exams`：随机模式（`addQuype=1`）若同时提交 `quIds` 与 `quScores`（与预览列表一致），则按确认后的题目与分值组卷，不再重新洗牌。  
发布范围：`targetType=1`（默认）按班级，传 `gradeIds`；`targetType=2` 按指定学生，传 `userIds`（逗号分隔），`gradeIds` 可由所选学生班级自动推导。

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

### 登录接口 HTTP 500（`Field 'device' doesn't have a default value`）

部分客户端（脚本、代理）不带常见 `User-Agent` 时，登录写 `t_log` 会因 `device` 为空失败。当前版本已对空设备名写入默认值「未知设备」；改代码后请 **重启后端**。

### 登录日志「登录地点」显示内网 IP、`Reserved` 或无法识别

- 登录地点格式为 **国家 省 市**（如 `中国 广东省 深圳市`）；**每次登录/登出实时查询，不做缓存**。
- 本机或局域网访问时，前端会在登录/登出前通过浏览器多源查询当前公网 IP 并传给后端（请求头 `X-Client-Public-Ip`），可反映 **VPN / 代理切换**；请同时重启前后端使改动生效。
- 查询顺序：国内接口优先（pconline JSONP、ipip.net、ip.sb 等）→ 国外接口 → 最后经同源接口 `GET /api/auths/client-public-ip` 由后端查出口 IP。
- 若浏览器与后端均无法获取公网 IP，后端会再尝试用服务端出口 IP 解析地点；仍失败才显示 `本机/内网（10.x.x.x）`。
- 公网 IP 优先用离线库 `ip2region.xdb`；识别失败时再调在线接口（pconline / ip-api）。
- 若经 Nginx 反向代理部署，请转发 `X-Real-IP` / `X-Forwarded-For`，否则后端只能看到代理机 IP。
- 开发时前端 `vue.config.js` 已配置代理转发真实客户端 IP 与 `X-Client-Public-Ip`；修改后需 **重启 `npm run dev`**。

### 登录日志「登录设备」显示 `Macintosh`、`Linux` 等不准确信息

- 设备名由后端解析 `User-Agent`，格式示例：`Windows 10/11 / Chrome`、`iPhone / iOS / Safari`、`SM-G991B / Android 13 / Chrome`。
- 学生 Electron 客户端会识别为 `Electron`；若仍显示「未知设备」，请确认客户端未屏蔽 `User-Agent` 请求头。

### 刷题/错题本报 `For input string: "question"`

- 原因：请求 `/api/exercises/question/{题目ID}` 时若 **题目 ID 为空**，Spring 会把路径 `/api/exercises/question` 误当成「题库 ID = question」，从而报类型转换错误。
- 处理：请使用 **2026-07-03 之后** 的后端与前端；重启后端后重新进入刷题/错题本。复合题（题型 5）需在刷题页显示小题作答区，旧版前端仅显示题干无答题框。
- 自查：浏览器开发者工具 → Network，确认请求形如 `GET /api/exercises/question/123`（末尾为数字 ID），而不是 `/api/exercises/question` 或 `/api/exercises/question/`。

### 获取题库/刷题等接口报 `InaccessibleObjectException`（`ClassLoader.defineClass`）

- **原因**：本机用了 **Java 17 或更高**（如 JDK 21），而项目基于 Spring Boot 2 + MyBatis-Plus Lambda 查询，在强封装模块下反射会失败，前端常显示「获取题库数据失败」。
- **推荐**：安装 **JDK 8 或 11**，设置 `JAVA_HOME` 后重启后端。
- **若必须用 JDK 17/21**：
  1. 用 `mvn spring-boot:run` 启动（项目已配置 `.mvn/jvm.config` 与插件 `--add-opens`）。
  2. 在 IDEA / Eclipse 运行主类时，VM 选项加入：
     ```
     --add-opens java.base/java.lang=ALL-UNNAMED
     --add-opens java.base/java.lang.reflect=ALL-UNNAMED
     --add-opens java.base/java.io=ALL-UNNAMED
     --add-opens java.base/java.util=ALL-UNNAMED
     ```
  3. 修改配置后 **重新编译并重启**（`mvn compile` 或 IDE Rebuild）。

### Maven 依赖下载 SSL 失败

配置阿里云镜像，见根 [README.md](../README.md)「问题 3」。

### 学生首页「在线时长」一登录就显示 24 小时

1. 确认已用当前源码重新编译并重启后端（`mvn compile` 或 `mvn spring-boot:run`），旧 `target` 里可能仍含「登录写满 86400 秒」的字节码。
2. 学生端登录后由前端每约 5 分钟调用 `POST /api/auths/track-presence`；库表按**秒**累计，图表按**分钟**展示。
3. 若当日数据已被旧版写满，重新登录学生账号会自动把当日记录归零后按心跳重算；仍异常可手动改 `t_user_daily_login_duration` 当日 `total_seconds`。

---

## 技术栈

Spring Boot 2 · MyBatis-Plus · Spring Security + JWT · Redis · WebSocket · MySQL · Knife4j

---

*最后更新：2026-07-03（Java 17+ 反射排错）*
