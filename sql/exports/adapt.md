## 对比结论

两份 SQL 都是 **36 张表**，表名完全一致。adapt 是在 full（1555 原库快照）基础上做的**适配版**，主要面向当前仓库后端可直接导入。

---

## adapt 实际做了 5 类改动

### 1. 表结构：主观题/AI 字段扩容（有实际改动）

**表：`t_exam_qu_answer`**

| 字段 | full（原库） | adapt（修复版） |
|------|-------------|----------------|
| `answer_content` | `varchar(255)` | `mediumtext` |
| `ai_reason` | `varchar(255)` | `mediumtext` |

目的：避免简答题长答案、AI 阅卷说明超过 255 字符被截断。  
**数据内容未变**，只是列类型放宽。

---

### 2. AI 平台密钥脱敏（有实际改动）

**表：`t_ai_platform_config`**

| | full | adapt |
|---|------|-------|
| `base_url` | `https://ark.cn-beijing.volces.com/api/v3` | `''`（空） |
| `api_key` | 真实密钥 | `''`（空） |
| `model_name` | `doubao-seed-2-0-lite-260215` | `''`（空） |
| `enabled` | `1` | `0` |

导入后需在管理端「API 连接配置」重新填写。

---

### 3. AI 功能码对齐代码（有实际改动）

**表：`t_ai_feature_config`**

| full（5 条） | adapt（4 条） |
|-------------|--------------|
| assistant, briefing, grading, **question_import**, question_review | assistant, briefing, grading, question_review |

删除了代码不支持的 `question_import` 功能码。

---

### 4. 表注释乱码修复（仅元数据，数据不变）

**表：`t_ai_knowledge_doc`**

full 里列注释是乱码（导出编码问题），例如：

```
COMMENT '涓婚敭'          →  '主键'
COMMENT '文档标�?...'      →  '文档标题（管理端展示）'
COMMENT 'AI 鍔╂墜鐭ヨ瘑...' →  'AI 助手知识库文档'
```

**文档正文数据（5 条 Markdown）两份完全相同**，只修了 COMMENT 文字。

---

### 5. 头部声明的「脏数据修复」——实际未做

adapt 文件头写了：

> 修复无班级学生、无效证书记录、在线时长超 24 小时等脏数据

逐表对比 INSERT 后，**这 3 项在 adapt 里与原库完全一致**：

| 问题 | full | adapt | 是否修复 |
|------|------|-------|---------|
| 无班级学生（如 id=165 李涵熠，`grade_id=NULL`） | 存在 | 仍存在 | 未修 |
| 无效证书（`t_certificate_user` id=2，`certificate_id=NULL`） | 存在 | 仍存在 | 未修 |
| 在线时长 86400 秒（4 条，含 2025-04-24、2026-05-19 等） | 4 条 | 4 条 | 未修 |

也就是说，头部第 4 条是**计划说明或文档遗留**，这份 export 里并没有真正改这些数据。

---

## 其余 25 张表：完全相同

包括与你关心的业务直接相关的：

- `t_repo`（题库）
- `t_question`（试题）
- `t_user`、`t_exam`、`t_category` 等

**INSERT 数据字节级一致**，没有任何差异。

---

## 与之前 JVM 报错的关系

adapt 的改动全部是：

- 2 个字段类型扩容
- AI 配置脱敏/裁剪
- 注释修复

**没有任何改动会影响** `InaccessibleObjectException` 或题库分页接口。  
你换 adapt 库后报错消失，原因不在 SQL，而在部署环境（JDK 8 / deploy 脚本自带 `--add-opens`）。

---

## 导入 adapt 后还需手动做的事

文件头已提醒：

1. **重新配置 AI**：`t_ai_platform_config` 密钥已清空
2. **复制上传文件**：需把原机 `uploads/exam-files/` 拷过来，否则题目图片会 404

---

## 简要对照

```
full (1555原库)                    adapt (修复版)
─────────────────────────────────────────────────
36 表，数据基本一致                36 表
t_exam_qu_answer  varchar(255)  →  mediumtext     ✅
t_ai_platform_config 含真实密钥  →  空 + disabled   ✅
t_ai_feature_config 5条          →  4条(删import)  ✅
t_ai_knowledge_doc 注释乱码       →  注释修正        ✅
脏数据(无班级/证书/86400秒)        →  未改(与full相同) ⚠️
t_repo / t_question / t_user等   →  完全相同        —
```

若你需要，我可以帮你写一份针对「脏数据」的实际修复 SQL（补班级、清无效证书、修正 86400 秒记录），单独作为 `alter/fix_dirty_data.sql` 使用。