# 在线考试系统 · 试题 JSON 批量导入规范

> **用途**：将本文档全文交给 AI，要求其按此规范生成可导入的 JSON 文件。  
> **导入接口**：`POST /api/questions/import/{题库ID}`，上传字段名 `file`，编码 **UTF-8**。  
> **参考示例**：同目录 `question_import_example.json`。

---

## 1. 文件根结构（必须二选一）

```json
[
  { "quType": 1, "content": "...", "options": [ ... ] }
]
```

或：

```json
{
  "questions": [
    { "quType": 1, "content": "...", "options": [ ... ] }
  ]
}
```

| 规则 | 说明 |
|------|------|
| **必须** | 根为数组 `[...]`，或对象且含非空数组 `questions` |
| **禁止** | 空文件、空数组、根节点为单个题目对象（无 `questions` 包裹时） |
| **禁止** | 非 UTF-8 编码 |

---

## 2. 题型代码 `quType`

| 值 | 含义 |
|----|------|
| `1` | 单选题 |
| `2` | 多选题 |
| `3` | 判断题 |
| `4` | 简答题 |
| `5` | 复合题（一道大题含多道小题） |

**全局硬性要求：**

- 每条记录 **必须** 有 `quType`，且为整数 `1`～`5`，**不能** 为 `null`、不能省略。
- **禁止** 使用 `type`、`questionType` 等其它字段名代替 `quType`。

---

## 3. 三种导入模式（每条记录只能属于一种）

### 模式 A：普通题（最常见）

- **条件**：无 `stemGroupCode`，且 `quType` 为 `1`～`4`
- **禁止**：填写 `subItems`、`sharedStemContent`

### 模式 B：单条复合题

- **条件**：`quType` 为 `5`，且有非空 `subItems`
- **禁止**：填写 `stemGroupCode`、顶层 `options`

### 模式 C：材料组（多行合并为一题）

- **条件**：填写相同的 `stemGroupCode`；组内每行 `quType` 为 `1`～`4`
- **禁止**：组内任一行 `quType` 为 `5`；**禁止** 使用 `subItems`

**互斥关系（违反即导入失败）：**

| 组合 | 是否允许 |
|------|----------|
| `quType: 5` + `stemGroupCode` | ❌ |
| `subItems` 存在 + `quType` 不是 5（且无材料组） | ❌ |
| `quType: 5` + 顶层 `options` | ❌ |
| 材料组内 + `quType: 5` | ❌ |

---

## 4. 字段说明（按模式）

### 4.1 模式 A · 普通题字段

| 字段 | 类型 | 必须 | 说明 |
|------|------|:----:|------|
| `quType` | int | ✅ | `1`～`4` |
| `content` | string | ✅ | 题干，不能为空白 |
| `options` | array | ✅* | 见第 5 节；简答至少 1 项，客观至少 2 项 |
| `analysis` | string | ❌ | 题目解析，可省略 |
| `image` | string | ❌ | 题干图片 URL，可省略 |

### 4.2 模式 B · 复合题顶层字段

| 字段 | 类型 | 必须 | 说明 |
|------|------|:----:|------|
| `quType` | int | ✅ | 固定为 `5` |
| `content` | string | ✅ | **共用材料**题干 |
| `subItems` | array | ✅ | 非空，见 4.4 |
| `analysis` | string | ❌ | 整题解析 |
| `image` | string | ❌ | 材料配图 |
| `options` | — | ❌ | **不要** 在顶层写 |

### 4.3 模式 C · 材料组每行字段

| 字段 | 类型 | 首行 | 同组后续行 | 说明 |
|------|------|:----:|:----------:|------|
| `stemGroupCode` | string | ✅ | ✅ | 相同字符串的多行合并为一题 |
| `sharedStemContent` | string | ✅ | ❌ | 仅首行：共用材料题干 |
| `sharedStemImage` | string | ❌ | ❌ | 仅首行有效 |
| `quType` | int | ✅ | ✅ | `1`～`4`，该行小题题型 |
| `content` | string | ✅ | ✅ | 该行**小题**题干（不是共用材料） |
| `options` | array | ✅ | ✅ | 该行小题的选项 |
| `analysis` | string | ❌ | ❌ | 仅首行写入整题解析 |

### 4.4 `subItems[]` 小题对象（仅模式 B）

| 字段 | 类型 | 必须 | 说明 |
|------|------|:----:|------|
| `quType` | int | ✅ | `1`～`4` |
| `content` | string | ✅ | 小题题干，须有文字或 HTML 图片 |
| `options` | array | ✅* | 规则同普通题 |
| `sort` | int | ❌ | 排序号；省略则按数组顺序 1、2、3… |

---

## 5. 选项 `options` 与 `isRight`

### 5.1 选项对象结构

```json
{
  "content": "选项文字",
  "isRight": 1,
  "image": null,
  "analysis": null
}
```

| 字段 | 必须 | 说明 |
|------|:----:|------|
| `content` | 有选项时实质必须 | **无内容或空字符串的项会被忽略**，不计入选项数 |
| `isRight` | 客观题必须 | 见下表 |
| `image` | ❌ | 可省略 |
| `analysis` | ❌ | 可省略 |

### 5.2 `isRight` 合法值

**推荐 AI 只使用数字：**

- `0` = 错误选项  
- `1` = 正确选项  

也支持（不推荐 AI 使用，易出错）：`true`/`false`、`"是"`/`"否"` 等。

**禁止：**

- `null`、省略 `isRight`（客观题且有 `content` 时）
- 字符串 `"null"`、`"undefined"`

简答题（`quType: 4`）：可不写 `isRight`，系统会自动设为 `1`。

### 5.3 各题型选项数量与正确答案数

| quType | 有效选项数（有 content） | isRight 要求 |
|--------|--------------------------|--------------|
| `1` 单选 | ≥ 2 | **恰好 1 个** `isRight: 1` |
| `2` 多选 | ≥ 2 | **至少 2 个** `isRight: 1` |
| `3` 判断 | ≥ 2 | **恰好 1 个** `isRight: 1` |
| `4` 简答 | ≥ 1 | 作为参考答案，`isRight` 可省略 |

---

## 6. 完整示例（可直接作模板）

```json
{
  "questions": [
    {
      "quType": 1,
      "content": "1+1等于几？",
      "analysis": "基础题",
      "options": [
        { "content": "2", "isRight": 1 },
        { "content": "3", "isRight": 0 }
      ]
    },
    {
      "quType": 2,
      "content": "下列哪些是偶数？",
      "options": [
        { "content": "2", "isRight": 1 },
        { "content": "4", "isRight": 1 },
        { "content": "3", "isRight": 0 }
      ]
    },
    {
      "quType": 3,
      "content": "地球是圆的。",
      "options": [
        { "content": "正确", "isRight": 1 },
        { "content": "错误", "isRight": 0 }
      ]
    },
    {
      "quType": 4,
      "content": "简述牛顿第一定律。",
      "options": [
        { "content": "物体在不受外力时保持静止或匀速直线运动。" }
      ]
    },
    {
      "quType": 5,
      "content": "阅读下列材料，回答问题。",
      "subItems": [
        {
          "sort": 1,
          "content": "(1) 本诗题材是？",
          "quType": 1,
          "options": [
            { "content": "山水", "isRight": 1 },
            { "content": "边塞", "isRight": 0 }
          ]
        },
        {
          "sort": 2,
          "content": "(2) 概括主旨。",
          "quType": 4,
          "options": [
            { "content": "参考答案要点。" }
          ]
        }
      ]
    },
    {
      "stemGroupCode": "G1",
      "sharedStemContent": "根据下文回答问题。（材料组：多行合并）",
      "quType": 2,
      "content": "第1小题题干",
      "options": [
        { "content": "A", "isRight": 1 },
        { "content": "B", "isRight": 1 },
        { "content": "C", "isRight": 0 }
      ]
    },
    {
      "stemGroupCode": "G1",
      "quType": 1,
      "content": "第2小题题干",
      "options": [
        { "content": "对", "isRight": 1 },
        { "content": "错", "isRight": 0 }
      ]
    }
  ]
}
```

---

## 7. AI 生成检查清单（输出前自检）

生成 JSON 后，请逐项确认：

- [ ] 根结构为 `[...]` 或 `{"questions":[...]}`
- [ ] 每条记录都有整数 `quType`，无 `null`
- [ ] 未使用错误字段名（只用 `quType`，不用 `type` 等）
- [ ] 客观题每个有效选项都有 `isRight: 0` 或 `1`（数字）
- [ ] 单选/判断：恰好一个 `isRight: 1`
- [ ] 多选：至少两个 `isRight: 1`
- [ ] 简答：至少一个带 `content` 的 option
- [ ] 复合题：`quType: 5` + `subItems`，无顶层 `options`，无 `stemGroupCode`
- [ ] 材料组：相同 `stemGroupCode`，首行有 `sharedStemContent`，行内 `quType` 为 1～4
- [ ] 可选字段未用 `null` 占位（不需要则直接省略）
- [ ] 输出为纯 JSON，无 Markdown 代码块包裹（除非用户要求）

---

## 8. 给 AI 的简短系统提示（可复制）

```
你是试题 JSON 生成器。严格按《JSON_QUESTION_IMPORT_SPEC.md》输出 UTF-8 JSON：
- 根节点用 {"questions":[...]}
- 每题必有整数 quType（1单选 2多选 3判断 4简答 5复合题）
- 客观题 options 每项有 content 时必须带 isRight:0 或 1（数字，禁止 null）
- 单选/判断：恰好一个 isRight:1；多选：至少两个 isRight:1
- 简答：至少一个 option 作参考答案，isRight 可省略
- 复合题：quType:5 + subItems，禁止顶层 options 和 stemGroupCode
- 材料组：多行相同 stemGroupCode，首行 sharedStemContent，行内 quType 1-4，禁止 quType:5
- 不要输出 null 占位必填字段；只输出合法 JSON，不要解释文字。
```

---

## 9. 与 Excel 导入的对应关系

| JSON 字段 | Excel 列名 |
|-----------|------------|
| `quType` | 试题类型 |
| `content` | 题干 |
| `analysis` | 解析 |
| `image` | 题干图片 |
| `options` | 选项一～六（JSON 不限个数） |
| `stemGroupCode` | 材料组编号 |
| `sharedStemContent` | 共用材料题干 |
| `sharedStemImage` | 共用材料题干图片 |

---

*文档版本：与后端 `QuestionJsonImportRow` / `QuestionImportValidators` 实现一致（2026-05）*
