# 共享待办清单 V2-A - 产品需求文档 (PRD)

**文档版本**: 2.0-A
**创建日期**: 2026-02-18
**项目类型**: 功能增强版本
**产品负责人**: PM
**文档状态**: Scope Freeze

---

## 1. 版本概述

### 1.1 V2-A 目标

在 V1 MVP 基础上，增强清单的核心管理能力，但不引入协作复杂度（用户系统、权限控制等）。

**核心原则**：
- 保持简单可运行
- 不破坏 V1 API
- 串行执行，Scope Freeze 后不接受变更

### 1.2 增强功能总览

| 功能模块 | 优先级 | 说明 |
|---------|--------|------|
| 清单删除 | P0 | 允许删除清单及所有关联事项 |
| 清单标题编辑 | P0 | 清单可设置标题（默认标题） |
| Todo 优先级 | P0 | 高/中/低三级优先级 |
| Todo 截止日期 | P0 | 可设置事项截止日期 |

### 1.3 与 V1 的关系

**V1 功能保留**：
- 清单创建与分享
- 待办事项的基础管理（添加、编辑、删除、完成状态）
- Token 机制
- 所有 V1 API 继续可用

**V1 禁止功能保持禁止**：
- 用户系统
- 权限控制
- WebSocket
- Chat
- 邀请机制
- 实时同步

---

## 2. 功能范围定义

### 2.1 清单删除 (P0)

**用户故事**: 作为一名用户，我想删除不再需要的清单，以便清理数据。

**功能描述**：
- 删除清单时，级联删除该清单下的所有待办事项
- 删除前需要用户确认
- 删除后重定向到首页

**用户交互流程**：
```
用户在清单详情页点击"删除清单"按钮
  ↓
弹出确认对话框（使用原生 confirm）
  ↓
用户确认
  ↓
调用 DELETE /api/lists/{token}
  ↓
后端删除清单及所有关联事项（数据库级联删除）
  ↓
重定向到首页 (/)
```

**API 设计**：
```
DELETE /api/lists/{token}
Response: 204 No Content
```

**验收标准**：
- [ ] 可以删除清单
- [ ] 删除清单时，所有关联事项被删除
- [ ] 删除前有确认提示
- [ ] 删除后重定向到首页
- [ ] 删除不存在的 token 返回 404
- [ ] V1 的其他 API 不受影响

---

### 2.2 清单标题编辑 (P0)

**用户故事**: 作为一名用户，我想为清单设置一个描述性的标题，以便更好地识别和管理。

**功能描述**：
- 清单创建时自动生成默认标题（如："我的清单 2026-02-18"）
- 用户可以编辑清单标题
- 标题显示在清单详情页顶部
- 标题显示在浏览器标签页

**数据字段**：
```
TodoList {
  id: Long
  token: String (8位)
  title: String (100字符，新增字段)
  createdAt: LocalDateTime
  updatedAt: LocalDateTime
}
```

**默认标题规则**：
- 格式: "我的清单 yyyy-MM-dd"
- 示例: "我的清单 2026-02-18"

**用户交互流程**：
```
清单详情页显示标题
  ↓
用户点击标题旁的"编辑"按钮
  ↓
标题变为输入框，显示当前标题
  ↓
用户修改标题，按回车或点击"保存"
  ↓
调用 PATCH /api/lists/{token}
Request body: { "title": "新标题" }
  ↓
后端更新数据库
  ↓
前端更新显示
```

**API 设计**：
```
PATCH /api/lists/{token}
Request: { "title": "新标题" }
Response: 200 OK
{
  "id": 1,
  "token": "aB3xK9mP",
  "title": "我的清单 2026-02-18",
  "createdAt": "2026-02-18T10:00:00"
}
```

**向后兼容性**：
- GET /api/lists/{token} 新增返回 title 字段
- V1 客户端忽略新字段，不影响现有功能
- 创建清单时自动生成默认标题

**验收标准**：
- [ ] 创建清单时自动生成默认标题
- [ ] 可以编辑清单标题
- [ ] 标题显示在清单详情页
- [ ] 标题显示在浏览器标签页
- [ ] 标题为空时使用默认标题
- [ ] 标题长度不超过 100 字符
- [ ] API 保持向后兼容

---

### 2.3 Todo 优先级 (P0)

**用户故事**: 作为一名用户，我想标记事项的优先级，以便先处理重要任务。

**功能描述**：
- 每个待办事项可以设置优先级：高/中/低
- 默认优先级为"中"
- 优先级在列表中以视觉区分（颜色或图标）
- 可以在创建或编辑事项时设置优先级

**数据字段**：
```
TodoItem {
  id: Long
  listId: Long
  title: String (200字符)
  priority: String (枚举: HIGH/MEDIUM/LOW，新增字段)
  dueDate: LocalDate (可为空，新增字段)
  completed: Boolean
  createdAt: LocalDateTime
  updatedAt: LocalDateTime
}
```

**优先级定义**：
| 值 | 说明 | 视觉表现 |
|----|------|----------|
| HIGH | 高优先级 | 红色标记 🔴 |
| MEDIUM | 中优先级 | 黄色标记 🟡 |
| LOW | 低优先级 | 绿色标记 🟢 |

**用户交互流程**：

**流程 1: 创建事项时设置优先级**
```
用户输入标题
  ↓
选择优先级（下拉框，默认"中"）
  ↓
按回车或点击"添加"
  ↓
调用 POST /api/lists/{token}/items
Request: { "title": "买牛奶", "priority": "HIGH" }
  ↓
后端保存到数据库
  ↓
前端渲染事项，显示优先级标记
```

**流程 2: 编辑事项优先级**
```
用户点击事项的"编辑"按钮
  ↓
显示编辑表单（标题 + 优先级 + 截止日期）
  ↓
用户修改优先级
  ↓
点击"保存"
  ↓
调用 PATCH /api/items/{id}
Request: { "priority": "LOW" }
  ↓
后端更新数据库
  ↓
前端更新显示
```

**API 设计**：

**创建事项（扩展）**：
```
POST /api/lists/{token}/items
Request: {
  "title": "买牛奶",
  "priority": "HIGH"  // 新增字段，可选，默认 MEDIUM
}
Response: 201 Created
{
  "id": 1,
  "title": "买牛奶",
  "completed": false,
  "priority": "HIGH",
  "dueDate": null,
  "createdAt": "2026-02-18T10:01:00",
  "updatedAt": "2026-02-18T10:01:00"
}
```

**更新事项（扩展）**：
```
PATCH /api/items/{id}
Request: {
  "priority": "LOW"  // 新增字段，可选
}
Response: 200 OK
```

**向后兼容性**：
- 创建事项时不传 priority，默认为 "MEDIUM"
- V1 客户端忽略新字段，不影响现有功能

**验收标准**：
- [ ] 创建事项时可以设置优先级
- [ ] 不设置优先级时默认为"中"
- [ ] 可以编辑事项的优先级
- [ ] 优先级在列表中有视觉区分
- [ ] API 保持向后兼容
- [ ] 高优先级事项显示红色标记
- [ ] 中优先级事项显示黄色标记
- [ ] 低优先级事项显示绿色标记

---

### 2.4 Todo 截止日期 (P0)

**用户故事**: 作为一名用户，我想为事项设置截止日期，以便跟踪任务进度。

**功能描述**：
- 每个待办事项可以设置截止日期（可选）
- 截止日期在列表中显示
- 过期事项有视觉提示（红色文字或标记）
- 可以在创建或编辑事项时设置截止日期

**截止日期格式**：
- 前端显示: "2026-02-18" (yyyy-MM-dd)
- API 传输: "2026-02-18" (ISO 8601 日期格式)
- 数据库存储: DATE 类型

**用户交互流程**：

**流程 1: 创建事项时设置截止日期**
```
用户输入标题
  ↓
选择优先级（可选）
  ↓
选择截止日期（日期选择器，可选）
  ↓
按回车或点击"添加"
  ↓
调用 POST /api/lists/{token}/items
Request: {
  "title": "买牛奶",
  "priority": "HIGH",
  "dueDate": "2026-02-20"
}
  ↓
后端保存到数据库
  ↓
前端渲染事项，显示截止日期
```

**流程 2: 编辑事项截止日期**
```
用户点击事项的"编辑"按钮
  ↓
显示编辑表单
  ↓
用户修改截止日期
  ↓
点击"保存"
  ↓
调用 PATCH /api/items/{id}
Request: { "dueDate": "2026-02-25" }
  ↓
后端更新数据库
  ↓
前端更新显示
```

**视觉提示规则**：
| 场景 | 视觉表现 |
|------|----------|
| 未过期 | 正常显示，格式: "截止: 2026-02-20" |
| 今天到期 | 蓝色标记，格式: "今天到期" |
| 已过期 | 红色标记 + ⚠️ 图标，格式: "已过期: 2026-02-18" |
| 无截止日期 | 不显示截止日期 |

**API 设计**：

**创建事项（扩展）**：
```
POST /api/lists/{token}/items
Request: {
  "title": "买牛奶",
  "priority": "HIGH",
  "dueDate": "2026-02-20"  // 新增字段，可选，格式: yyyy-MM-dd
}
Response: 201 Created
{
  "id": 1,
  "title": "买牛奶",
  "completed": false,
  "priority": "HIGH",
  "dueDate": "2026-02-20",
  "createdAt": "2026-02-18T10:01:00",
  "updatedAt": "2026-02-18T10:01:00"
}
```

**更新事项（扩展）**：
```
PATCH /api/items/{id}
Request: {
  "dueDate": "2026-02-25"  // 新增字段，可选，格式: yyyy-MM-dd
}
Response: 200 OK
```

**清除截止日期**：
```
PATCH /api/items/{id}
Request: {
  "dueDate": null  // 设置为 null 表示清除截止日期
}
Response: 200 OK
```

**向后兼容性**：
- 创建事项时不传 dueDate，默认为 null
- V1 客户端忽略新字段，不影响现有功能

**验收标准**：
- [ ] 创建事项时可以设置截止日期
- [ ] 不设置截止日期时为空
- [ ] 可以编辑事项的截止日期
- [ ] 可以清除截止日期（设置为 null）
- [ ] 截止日期在列表中显示
- [ ] 过期事项有红色视觉提示
- [ ] 今天到期的事项有蓝色视觉提示
- [ ] API 保持向后兼容
- [ ] 日期格式验证（yyyy-MM-dd）
- [ ] 不接受无效日期（如 2026-02-30）

---

## 3. 数据库设计变更

### 3.1 todo_list 表变更

**新增字段**：
```sql
ALTER TABLE todo_list ADD COLUMN title VARCHAR(100) DEFAULT NULL;

-- 为现有数据设置默认标题（可选）
UPDATE todo_list
SET title = CONCAT('我的清单 ', DATE_FORMAT(created_at, '%Y-%m-%d'))
WHERE title IS NULL;

-- 设置为必填字段（可选，取决于业务需求）
ALTER TABLE todo_list MODIFY COLUMN title VARCHAR(100) NOT NULL;
```

**完整表结构**：
```sql
CREATE TABLE todo_list (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  token VARCHAR(8) NOT NULL UNIQUE COMMENT '访问令牌(8位随机字符)',
  title VARCHAR(100) NOT NULL COMMENT '清单标题',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_token (token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='待办清单表';
```

**字段说明**：
| 字段名 | 类型 | 约束 | 说明 | 变更类型 |
|--------|------|------|------|----------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 主键 | 不变 |
| token | VARCHAR(8) | NOT NULL, UNIQUE | 访问令牌 | 不变 |
| title | VARCHAR(100) | NOT NULL | 清单标题 | **新增** |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 | 不变 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 | 不变 |

---

### 3.2 todo_item 表变更

**新增字段**：
```sql
ALTER TABLE todo_item
ADD COLUMN priority VARCHAR(6) DEFAULT 'MEDIUM',
ADD COLUMN due_date DATE DEFAULT NULL;

-- 添加约束检查
ALTER TABLE todo_item
ADD CONSTRAINT chk_priority CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW'));
```

**完整表结构**：
```sql
CREATE TABLE todo_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  list_id BIGINT NOT NULL COMMENT '所属清单ID(外键)',
  title VARCHAR(200) NOT NULL COMMENT '事项标题',
  priority VARCHAR(6) NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级(HIGH/MEDIUM/LOW)',
  due_date DATE DEFAULT NULL COMMENT '截止日期',
  completed BOOLEAN DEFAULT FALSE COMMENT '是否完成',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (list_id) REFERENCES todo_list(id) ON DELETE CASCADE,
  INDEX idx_list_id (list_id),
  CONSTRAINT chk_priority CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='待办事项表';
```

**字段说明**：
| 字段名 | 类型 | 约束 | 说明 | 变更类型 |
|--------|------|------|------|----------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 主键 | 不变 |
| list_id | BIGINT | NOT NULL, FOREIGN KEY | 所属清单ID | 不变 |
| title | VARCHAR(200) | NOT NULL | 事项标题 | 不变 |
| priority | VARCHAR(6) | NOT NULL, DEFAULT 'MEDIUM', CHECK | 优先级 | **新增** |
| due_date | DATE | DEFAULT NULL | 截止日期 | **新增** |
| completed | BOOLEAN | DEFAULT FALSE | 是否完成 | 不变 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 | 不变 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 | 不变 |

---

### 3.3 数据迁移脚本

**为现有 todo_list 数据生成默认标题**：
```sql
-- 方案 1: 根据创建时间生成
UPDATE todo_list
SET title = CONCAT('我的清单 ', DATE_FORMAT(created_at, '%Y-%m-%d'))
WHERE title IS NULL;

-- 方案 2: 生成更友好的标题
UPDATE todo_list
SET title = CONCAT('清单 ', token)
WHERE title IS NULL;
```

**为现有 todo_item 数据设置默认优先级**：
```sql
-- 已有数据默认为 MEDIUM（通过 DEFAULT 约束自动处理）
-- 如果需要手动更新：
UPDATE todo_item
SET priority = 'MEDIUM'
WHERE priority IS NULL;
```

---

## 4. API 设计建议

### 4.1 新增 API 端点

#### API 1: 删除清单

**端点**: `DELETE /api/lists/{token}`

**功能**: 删除指定 token 的清单及其所有关联事项

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| token | String | 是 | 清单的唯一访问令牌 |

**请求示例**:
```http
DELETE /api/lists/abc123xyz456 HTTP/1.1
```

**成功响应** (204 No Content):
```
(无响应体)
```

**错误响应** (404 Not Found):
```json
{
  "error": "List not found",
  "message": "清单不存在"
}
```

**业务逻辑**:
1. 根据 token 查询清单
2. 如果不存在，返回 404
3. 删除清单（数据库级联删除所有事项）
4. 返回 204

---

#### API 2: 更新清单标题

**端点**: `PATCH /api/lists/{token}`

**功能**: 更新清单标题

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| token | String | 是 | 清单的唯一访问令牌 |

**请求示例**:
```http
PATCH /api/lists/abc123xyz456 HTTP/1.1
Content-Type: application/json

{
  "title": "家庭购物清单"
}
```

**请求体** (UpdateListRequest):
| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| title | String | 是 | 1-100 字符 | 清单标题 |

**成功响应** (200 OK):
```json
{
  "id": 1,
  "token": "abc123xyz456",
  "title": "家庭购物清单",
  "createdAt": "2026-02-18T10:00:00",
  "updatedAt": "2026-02-18T12:00:00"
}
```

**错误响应** (400 Bad Request):
```json
{
  "error": "Invalid title",
  "message": "标题长度不能超过 100 个字符"
}
```

**错误响应** (404 Not Found):
```json
{
  "error": "List not found",
  "message": "清单不存在"
}
```

---

### 4.2 修改现有 API 端点（保持向后兼容）

#### API 3: 创建清单（扩展响应）

**端点**: `POST /api/lists`

**变更**: 响应中新增 `title` 字段

**成功响应** (201 Created):
```json
{
  "id": 1,
  "token": "abc123xyz456",
  "title": "我的清单 2026-02-18",
  "createdAt": "2026-02-18T10:00:00"
}
```

**向后兼容性**:
- V1 客户端忽略 `title` 字段，不影响现有功能

---

#### API 4: 获取清单详情（扩展响应）

**端点**: `GET /api/lists/{token}`

**变更**: 响应中新增 `title` 字段，items 中新增 `priority` 和 `dueDate` 字段

**成功响应** (200 OK):
```json
{
  "id": 1,
  "token": "abc123xyz456",
  "title": "家庭购物清单",
  "createdAt": "2026-02-18T10:00:00",
  "items": [
    {
      "id": 1,
      "title": "买牛奶",
      "completed": false,
      "priority": "HIGH",
      "dueDate": "2026-02-20",
      "createdAt": "2026-02-18T10:01:00",
      "updatedAt": "2026-02-18T10:01:00"
    },
    {
      "id": 2,
      "title": "买鸡蛋",
      "completed": true,
      "priority": "MEDIUM",
      "dueDate": null,
      "createdAt": "2026-02-18T10:02:00",
      "updatedAt": "2026-02-18T10:05:00"
    }
  ]
}
```

**向后兼容性**:
- V1 客户端忽略新增字段，不影响现有功能

---

#### API 5: 获取待办事项列表（扩展响应）

**端点**: `GET /api/lists/{token}/items`

**变更**: 响应中新增 `priority` 和 `dueDate` 字段

**成功响应** (200 OK):
```json
[
  {
    "id": 1,
    "title": "买牛奶",
    "completed": false,
    "priority": "HIGH",
    "dueDate": "2026-02-20",
    "createdAt": "2026-02-18T10:01:00",
    "updatedAt": "2026-02-18T10:01:00"
  }
]
```

**向后兼容性**:
- V1 客户端忽略新增字段，不影响现有功能

---

#### API 6: 添加待办事项（扩展请求和响应）

**端点**: `POST /api/lists/{token}/items`

**变更**: 请求中新增 `priority` 和 `dueDate` 可选字段，响应中新增对应字段

**请求示例**:
```http
POST /api/lists/abc123xyz456/items HTTP/1.1
Content-Type: application/json

{
  "title": "买牛奶",
  "priority": "HIGH",
  "dueDate": "2026-02-20"
}
```

**请求体** (CreateItemRequest):
| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| title | String | 是 | 非空 | 待办事项的标题 |
| priority | String | 否 | HIGH/MEDIUM/LOW | 优先级（默认 MEDIUM） |
| dueDate | String | 否 | yyyy-MM-dd | 截止日期（可选） |

**成功响应** (201 Created):
```json
{
  "id": 1,
  "title": "买牛奶",
  "completed": false,
  "priority": "HIGH",
  "dueDate": "2026-02-20",
  "createdAt": "2026-02-18T10:01:00",
  "updatedAt": "2026-02-18T10:01:00"
}
```

**向后兼容性**:
- V1 客户端不传 `priority` 和 `dueDate`，服务端设置默认值
- V1 客户端忽略响应中的新增字段

---

#### API 7: 更新待办事项（扩展请求和响应）

**端点**: `PATCH /api/items/{id}`

**变更**: 请求中新增 `priority` 和 `dueDate` 可选字段，响应中新增对应字段

**请求示例**:
```http
PATCH /api/items/1 HTTP/1.1
Content-Type: application/json

{
  "priority": "LOW",
  "dueDate": "2026-02-25"
}
```

**请求体** (UpdateItemRequest):
| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| title | String | 否 | 非空（如果提供） | 新的待办事项标题 |
| completed | Boolean | 否 | - | 完成状态 |
| priority | String | 否 | HIGH/MEDIUM/LOW | 优先级 |
| dueDate | String | 否 | yyyy-MM-dd 或 null | 截止日期 |

**注意**: title、completed、priority、dueDate 至少需要提供一个。

**成功响应** (200 OK):
```json
{
  "id": 1,
  "title": "买牛奶",
  "completed": false,
  "priority": "LOW",
  "dueDate": "2026-02-25",
  "updatedAt": "2026-02-18T12:00:00"
}
```

**清除截止日期**:
```json
{
  "dueDate": null
}
```

**向后兼容性**:
- V1 客户端不传新增字段，不影响现有功能
- V1 客户端忽略响应中的新增字段

---

### 4.3 HTTP 状态码规范（沿用 V1）

| 状态码 | 场景 |
|--------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 204 | 删除成功 |
| 400 | 参数错误 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

---

### 4.4 错误响应格式规范（沿用 V1）

**统一格式**:
```json
{
  "error": "ERROR_CODE",
  "message": "用户友好的错误提示"
}
```

**新增错误码**:
| 错误码 | HTTP 状态码 | 说明 |
|--------|-------------|------|
| Invalid priority | 400 | 优先级值无效（不是 HIGH/MEDIUM/LOW） |
| Invalid dueDate | 400 | 截止日期格式错误或无效 |

---

## 5. 非目标确认

### 5.1 明确不做（V2-A 禁止实现）

| 功能 | 说明 | 何时考虑 |
|------|------|----------|
| 用户注册/登录 | 无需账号系统 | V2-B 或后续 |
| 权限控制 | 所有人可编辑所有清单 | V2-B 或后续 |
| 实时同步 | 无 WebSocket，需刷新更新 | V2-C 或后续 |
| 清单列表页 | 无"我的清单"功能 | V2-B 或后续 |
| 事项分类/标签 | 不支持 | V3 或后续 |
| 拖拽排序 | 不支持 | V3 或后续 |
| 操作历史/撤销 | 不支持 | V3 或后续 |
| 搜索/过滤 | 不支持 | V3 或后续 |
| 清单描述 | 仅支持标题，不支持长描述 | V3 或后续 |
| 清单模板 | 不支持 | V3 或后续 |
| 子任务 | 不支持 | V3 或后续 |
| 文件附件 | 不支持 | V3 或后续 |
| 评论功能 | 不支持 | V3 或后续 |
| 提醒通知 | 不支持 | V3 或后续 |
| 数据导出 | 不支持 | V3 或后续 |

### 5.2 技术约束（V2-A 禁止）

- ❌ 不使用前端框架 (React/Vue/Angular)
- ❌ 不使用 WebSocket 或 SSE
- ❌ 不使用 Redis 等缓存
- ❌ 不使用消息队列
- ❌ 不使用微服务架构
- ❌ 不实现复杂的权限系统
- ❌ 不实现 OAuth 或第三方登录
- ❌ 不使用复杂的前端状态管理
- ❌ 不实现自动化测试(可手动测试)

### 5.3 明确不做（延续 V1）

- 不做 QR 码生成
- 不做深色模式
- 不做移动端适配优化 (响应式即可)
- 不做 SEO 优化
- 不做日志分析
- 不做监控告警

---

## 6. 验收标准

### 6.1 功能验收清单

#### 必须完成 (P0)
- [ ] 可以删除清单
- [ ] 删除清单时所有关联事项被删除
- [ ] 创建清单时自动生成默认标题
- [ ] 可以编辑清单标题
- [ ] 标题显示在清单详情页和浏览器标签页
- [ ] 创建事项时可以设置优先级
- [ ] 优先级默认为"中"
- [ ] 可以编辑事项的优先级
- [ ] 优先级有视觉区分（高红/中黄/低绿）
- [ ] 创建事项时可以设置截止日期
- [ ] 可以编辑事项的截止日期
- [ ] 可以清除截止日期
- [ ] 截止日期显示在列表中
- [ ] 过期事项有红色视觉提示
- [ ] 今天到期的事项有蓝色视觉提示
- [ ] 数据持久化正常工作
- [ ] 刷新页面后数据保留
- [ ] V1 API 继续可用（向后兼容）

#### 应该完成 (P1)
- [ ] 前端输入验证（标题长度、优先级值、日期格式）
- [ ] 后端参数验证（优先级枚举、日期格式）
- [ ] 网络错误提示
- [ ] 删除清单确认对话框
- [ ] 日期选择器用户体验良好
- [ ] 优先级选择器用户体验良好

### 6.2 API 验收清单

#### 必须完成 (P0)
- [ ] DELETE /api/lists/{token} 正常工作
- [ ] PATCH /api/lists/{token} 正常工作
- [ ] POST /api/lists/{token}/items 支持 priority 和 dueDate
- [ ] PATCH /api/items/{id} 支持 priority 和 dueDate
- [ ] GET /api/lists/{token} 返回 title、priority、dueDate
- [ ] GET /api/lists/{token}/items 返回 priority 和 dueDate
- [ ] 所有 API 保持向后兼容

#### 应该完成 (P1)
- [ ] 优先级枚举值验证（只接受 HIGH/MEDIUM/LOW）
- [ ] 截止日期格式验证（yyyy-MM-dd）
- [ ] 截止日期有效性验证（如拒绝 2026-02-30）
- [ ] 标题长度验证（1-100 字符）
- [ ] 错误响应符合统一格式

### 6.3 数据库验收清单

#### 必须完成 (P0)
- [ ] todo_list 表成功添加 title 字段
- [ ] todo_item 表成功添加 priority 字段
- [ ] todo_item 表成功添加 due_date 字段
- [ ] priority 字段有 CHECK 约束
- [ ] 现有数据迁移成功
- [ ] 级联删除正常工作

#### 应该完成 (P1)
- [ ] 索引优化（如需要）
- [ ] 数据库迁移脚本可重复执行

### 6.4 可用性标准

| 指标 | 标准 |
|------|------|
| 页面加载时间 | < 1 秒(本地环境) |
| API 响应时间 | < 200ms (本地环境) |
| 并发支持 | 10 人同时操作不崩溃 |
| 浏览器兼容 | Chrome/Firefox/Edge 最新版 |
| 移动端可用 | 基本功能在手机浏览器可正常使用 |

### 6.5 代码质量

- [ ] 代码可编译运行
- [ ] 无明显 bug
- [ ] API 设计符合 REST 规范
- [ ] 数据库表结构合理
- [ ] 前端代码有基本注释
- [ ] 后端代码有基本注释
- [ ] 向后兼容性验证通过

---

## 7. 测试场景

### 场景 1: 删除清单

1. 创建清单并添加 3 个事项
2. 点击"删除清单"按钮
3. 确认删除对话框
4. 验证: 重定向到首页
5. 尝试访问原 token
6. 验证: 显示 404 页面

### 场景 2: 编辑清单标题

1. 创建清单
2. 验证: 自动生成默认标题（如"我的清单 2026-02-18"）
3. 点击"编辑标题"按钮
4. 修改标题为"家庭购物清单"
5. 保存
6. 验证: 标题更新成功
7. 刷新页面
8. 验证: 标题保留

### 场景 3: 设置事项优先级

1. 创建清单
2. 添加高优先级事项（"紧急任务"）
3. 添加中优先级事项（"普通任务"）
4. 添加低优先级事项（"可选任务"）
5. 验证: 三个事项有不同颜色标记
6. 编辑"普通任务"优先级为"高"
7. 验证: 颜色标记更新为红色

### 场景 4: 设置事项截止日期

1. 创建清单
2. 添加事项，设置截止日期为明天
3. 验证: 显示截止日期
4. 添加事项，设置截止日期为今天
5. 验证: 显示蓝色"今天到期"标记
6. 添加事项，设置截止日期为昨天
7. 验证: 显示红色"已过期"标记
8. 编辑事项，清除截止日期
9. 验证: 截止日期不再显示

### 场景 5: 完整流程

1. 创建清单，标题为"周末活动筹备"
2. 添加高优先级事项"订场地"，截止日期本周五
3. 添加中优先级事项"买饮料"，截止日期本周六
4. 添加低优先级事项"准备游戏"，无截止日期
5. 刷新页面
6. 验证: 所有数据保留
7. 分享链接给其他人
8. 验证: 其他人可以看到标题、优先级和截止日期

### 场景 6: 向后兼容性

1. 使用 V1 API 创建清单（POST /api/lists）
2. 验证: 响应包含 title 字段（默认值）
3. 使用 V1 API 添加事项（不传 priority 和 dueDate）
4. 验证: 响应包含 priority=MEDIUM 和 dueDate=null
5. 使用 V1 API 更新事项（只传 completed）
6. 验证: 功能正常，priority 和 dueDate 保持不变

### 场景 7: 错误处理

1. 创建事项，priority 传入无效值（如 "INVALID"）
2. 验证: 返回 400 错误
3. 创建事项，dueDate 传入无效格式（如 "2026/02/18"）
4. 验证: 返回 400 错误
5. 创建事项，dueDate 传入无效日期（如 "2026-02-30"）
6. 验证: 返回 400 错误
7. 编辑清单标题，传入空字符串
8. 验证: 返回 400 错误

---

## 8. 实现建议

### 8.1 数据库迁移策略

**方案 1: 自动迁移（推荐）**
- 使用 Spring Boot 的 `spring.jpa.hibernate.ddl-auto=update`
- Hibernate 自动添加新字段
- 手动执行数据更新 SQL（设置默认标题）

**方案 2: 手动迁移脚本**
- 创建 SQL 迁移脚本
- 使用 Flyway 或 Liquibase 管理
- 更适合生产环境

**推荐步骤**：
1. 开发环境使用 `ddl-auto=update`
2. 创建手动 SQL 脚本设置默认值
3. 生产环境使用 Flyway 管理

### 8.2 实体类更新

**TodoList 实体**：
```java
@Entity
@Table(name = "todo_list")
public class TodoList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 8)
    private String token;

    @Column(nullable = false, length = 100)
    private String title;  // 新增字段

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "list", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TodoItem> items = new ArrayList<>();

    // 构造方法
    public TodoList() {}

    public TodoList(String token) {
        this.token = token;
        this.title = generateDefaultTitle();  // 自动生成默认标题
    }

    // 默认标题生成
    private String generateDefaultTitle() {
        return "我的清单 " + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    // Getter 和 Setter...
}
```

**TodoItem 实体**：
```java
@Entity
@Table(name = "todo_item")
public class TodoItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    private TodoList list;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private Priority priority = Priority.MEDIUM;  // 新增字段，默认 MEDIUM

    @Column(name = "due_date")
    private LocalDate dueDate;  // 新增字段，可选

    @Column(nullable = false)
    private Boolean completed = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Getter 和 Setter...
}

// 优先级枚举
public enum Priority {
    HIGH,
    MEDIUM,
    LOW
}
```

### 8.3 DTO 更新

**ListResponse**：
```java
public class ListResponse {
    private Long id;
    private String token;
    private String title;  // 新增字段
    private String createdAt;
    private List<ItemResponse> items;

    public ListResponse(TodoList list) {
        this.id = list.getId();
        this.token = list.getToken();
        this.title = list.getTitle();  // 新增
        this.createdAt = formatDateTime(list.getCreatedAt());
        this.items = list.getItems().stream()
            .map(ItemResponse::new)
            .collect(Collectors.toList());
    }

    // Getter...
}
```

**ItemResponse**：
```java
public class ItemResponse {
    private Long id;
    private String title;
    private Boolean completed;
    private String priority;  // 新增字段
    private String dueDate;   // 新增字段
    private String createdAt;
    private String updatedAt;

    public ItemResponse(TodoItem item) {
        this.id = item.getId();
        this.title = item.getTitle();
        this.completed = item.getCompleted();
        this.priority = item.getPriority().name();  // 新增
        this.dueDate = item.getDueDate() != null
            ? item.getDueDate().toString()
            : null;  // 新增
        this.createdAt = formatDateTime(item.getCreatedAt());
        this.updatedAt = formatDateTime(item.getUpdatedAt());
    }

    // Getter...
}
```

**CreateItemRequest**：
```java
public class CreateItemRequest {
    @NotBlank(message = "标题不能为空")
    @Size(min = 1, max = 200, message = "标题长度必须在 1-200 个字符之间")
    private String title;

    private Priority priority = Priority.MEDIUM;  // 新增字段，默认 MEDIUM

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为 yyyy-MM-dd")
    private String dueDate;  // 新增字段，可选

    // Getter 和 Setter...
}
```

**UpdateItemRequest**：
```java
public class UpdateItemRequest {
    @Size(min = 1, max = 200, message = "标题长度必须在 1-200 个字符之间")
    private String title;

    private Boolean completed;

    private Priority priority;  // 新增字段

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为 yyyy-MM-dd")
    private String dueDate;  // 新增字段

    // Getter 和 Setter...

    @AssertTrue(message = "请求体不能为空")
    public boolean isValid() {
        return title != null || completed != null || priority != null || dueDate != null;
    }
}
```

**UpdateListRequest（新增）**：
```java
public class UpdateListRequest {
    @NotBlank(message = "标题不能为空")
    @Size(min = 1, max = 100, message = "标题长度必须在 1-100 个字符之间")
    private String title;

    // Getter 和 Setter...
}
```

### 8.4 Service 层更新

**ListService（新增方法）**：
```java
@Service
@Transactional
public class ListService {

    @Autowired
    private TodoListRepository listRepository;

    // 现有方法...

    public TodoList updateListTitle(String token, String title) {
        TodoList list = listRepository.findByToken(token)
            .orElseThrow(() -> new NotFoundException("List not found"));

        // 验证标题
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        title = title.trim();
        if (title.length() > 100) {
            throw new IllegalArgumentException("标题长度不能超过 100 个字符");
        }

        list.setTitle(title);
        return listRepository.save(list);
    }

    public void deleteList(String token) {
        TodoList list = listRepository.findByToken(token)
            .orElseThrow(() -> new NotFoundException("List not found"));
        listRepository.delete(list);
        // 级联删除由 JPA 自动处理
    }
}
```

**ItemService（更新方法）**：
```java
@Service
@Transactional
public class ItemService {

    // 现有 addItem 方法更新
    public TodoItem addItem(String token, String title, Priority priority, LocalDate dueDate) {
        // 验证标题...
        // 查找清单...

        // 创建事项
        TodoItem item = new TodoItem(list, title);
        item.setPriority(priority != null ? priority : Priority.MEDIUM);
        item.setDueDate(dueDate);
        return itemRepository.save(item);
    }

    // 现有 updateItem 方法更新
    public TodoItem updateItem(Long id, String title, Boolean completed, Priority priority, String dueDateStr) {
        TodoItem item = itemRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Item not found"));

        if (title != null) {
            // 验证并更新标题...
            item.setTitle(title);
        }

        if (completed != null) {
            item.setCompleted(completed);
        }

        if (priority != null) {
            item.setPriority(priority);
        }

        if (dueDateStr != null) {
            if (dueDateStr.isEmpty()) {
                item.setDueDate(null);
            } else {
                LocalDate dueDate = parseDate(dueDateStr);
                item.setDueDate(dueDate);
            }
        }

        return itemRepository.save(item);
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日期格式错误，必须为 yyyy-MM-dd");
        }
    }
}
```

### 8.5 Controller 层更新

**ListController（新增端点）**：
```java
@RestController
@RequestMapping("/api/lists")
public class ListController {

    @Autowired
    private ListService listService;

    // 现有方法...

    @PatchMapping("/{token}")
    public ResponseEntity<ListResponse> updateList(
        @PathVariable String token,
        @RequestBody @Valid UpdateListRequest request
    ) {
        TodoList list = listService.updateListTitle(token, request.getTitle());
        return ResponseEntity.ok(new ListResponse(list));
    }

    @DeleteMapping("/{token}")
    public ResponseEntity<Void> deleteList(@PathVariable String token) {
        listService.deleteList(token);
        return ResponseEntity.noContent().build();
    }
}
```

### 8.6 前端实现建议

**清单标题编辑**：
```javascript
// 编辑标题
async function updateListTitle(token, newTitle) {
  const response = await fetch(`/api/lists/${token}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title: newTitle })
  });

  if (response.ok) {
    const list = await response.json();
    document.title = list.title;  // 更新浏览器标签页
    document.getElementById('list-title').textContent = list.title;
  } else {
    alert('更新标题失败');
  }
}
```

**优先级选择器**：
```javascript
// 优先级配置
const PRIORITY_CONFIG = {
  'HIGH': { label: '高', color: 'red', icon: '🔴' },
  'MEDIUM': { label: '中', color: 'yellow', icon: '🟡' },
  'LOW': { label: '低', color: 'green', icon: '🟢' }
};

// 渲染优先级标记
function renderPriorityBadge(priority) {
  const config = PRIORITY_CONFIG[priority];
  return `<span class="priority-badge" style="background: ${config.color}">
    ${config.icon} ${config.label}
  </span>`;
}
```

**截止日期显示**：
```javascript
// 格式化截止日期
function formatDueDate(dueDateStr) {
  if (!dueDateStr) return '';

  const dueDate = new Date(dueDateStr);
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const diffDays = Math.ceil((dueDate - today) / (1000 * 60 * 60 * 24));

  if (diffDays < 0) {
    return `<span class="due-date overdue">⚠️ 已过期: ${dueDateStr}</span>`;
  } else if (diffDays === 0) {
    return `<span class="due-date today">📅 今天到期</span>`;
  } else {
    return `<span class="due-date normal">截止: ${dueDateStr}</span>`;
  }
}
```

**日期选择器**：
```html
<!-- 使用原生 HTML5 日期选择器 -->
<input type="date" id="dueDate" class="form-control">
```

---

## 9. 开发计划

### 9.1 开发顺序

**阶段 1: 数据库和实体 (1 天)**
1. 更新 Entity 类（TodoList、TodoItem）
2. 创建 Priority 枚举
3. 更新 DTO 类
4. 执行数据库迁移
5. 验证数据库变更

**阶段 2: Service 层 (1 天)**
1. 实现 ListService 新方法
2. 更新 ItemService 方法
3. 添加参数验证逻辑
4. 编写单元测试（可选）

**阶段 3: Controller 层 (1 天)**
1. 实现 ListController 新端点
2. 更新现有 Controller 方法
3. 测试所有 API
4. 验证向后兼容性

**阶段 4: 前端实现 (2 天)**
1. 实现清单标题编辑
2. 实现清单删除功能
3. 实现优先级选择和显示
4. 实现截止日期选择和显示
5. 实现视觉提示（颜色、图标）
6. 集成所有 API 调用

**阶段 5: 联调与测试 (1 天)**
1. 端到端测试所有新功能
2. 测试错误场景
3. 验证向后兼容性
4. 代码优化

**总工期**: 约 6 天

### 9.2 技术栈版本

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | LTS 版本 |
| Spring Boot | 3.x | 最新稳定版 |
| Spring Data JPA | 3.x | 随 Spring Boot |
| H2 Database | 2.x | 开发环境 |
| Thymeleaf | 3.x | 随 Spring Boot |

---

## 10. Scope Freeze 声明

本文档定义的所有功能和 API 设计自发布之日起冻结，后续开发过程中不得接受变更请求：

**冻结内容**：
- 功能范围：清单删除、清单标题编辑、Todo 优先级、Todo 截止日期
- 数据库设计：title、priority、dueDate 字段
- API 设计：所有新增和修改的端点
- 验收标准：所有 P0 和 P1 标准

**非目标明确禁止**：
- 用户系统
- 权限控制
- WebSocket
- Chat
- 邀请机制
- 实时同步
- 清单描述（长文本）
- 清单模板
- 子任务
- 文件附件
- 评论功能
- 提醒通知
- 数据导出

**变更流程**：
如有紧急变更需求，需经以下流程：
1. PM 提出变更申请
2. TechLead 评估技术影响
3. 双方共同评审
4. 发布新版本文档（PRD_V2A_v2.0.md）

---

## 11. 附录

### 11.1 术语表

| 术语 | 说明 |
|------|------|
| Token | 8 位随机字符串,用于唯一标识清单 |
| 清单 | TodoList,包含多个待办事项 |
| 事项 | TodoItem,清单中的单个任务 |
| 优先级 | HIGH/MEDIUM/LOW 三级优先级 |
| 截止日期 | 事项的预期完成日期 |
| 向后兼容 | V2 API 支持 V1 客户端继续使用 |

### 11.2 参考资源

- [V1 PRD](/d/develop/project/todolist/PRD.md)
- [V1 API Contract](/d/develop/project/todolist/API_CONTRACT.md)
- [V1 Tech Design](/d/develop/project/todolist/TECH_DESIGN.md)
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Thymeleaf 文档](https://www.thymeleaf.org/)
- [Fetch API 文档](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API)

### 11.3 联系方式

- **产品负责人**: PM
- **技术负责人**: TechLead
- **项目类型**: 功能增强版本
- **预期工期**: 6 天

---

**文档状态**: Scope Freeze
**产品负责人**: PM
**发布日期**: 2026-02-18
**文档版本**: 2.0-A

---

**文档结束**
