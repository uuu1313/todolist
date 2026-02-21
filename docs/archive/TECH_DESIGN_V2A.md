# 共享待办清单 V2-A - 技术设计文档

**文档版本**: 2.0-A
**创建日期**: 2026-02-18
**技术负责人**: TechLead
**文档状态**: API 冻结前最终版

**重要声明**: 本文档基于 PRD_V2A v2.0-A 编写,所有 API 契约一旦确认,后续开发过程中不得修改。

**向后兼容性承诺**: V2-A 不破坏 V1 API,所有 V1 客户端可继续正常使用。

---

## 1. 数据库设计变更

### 1.1 todo_list 表变更

#### 新增字段

```sql
-- 新增 title 字段（可选,默认为 NULL）
ALTER TABLE todo_list ADD COLUMN title VARCHAR(100) DEFAULT NULL;

-- 为现有数据生成默认标题
UPDATE todo_list
SET title = CONCAT('我的清单 ', DATE_FORMAT(created_at, '%Y-%m-%d'))
WHERE title IS NULL;

-- 设置为必填字段
ALTER TABLE todo_list MODIFY COLUMN title VARCHAR(100) NOT NULL;
```

#### 完整表结构

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

#### 字段说明

| 字段名 | 类型 | 约束 | 说明 | 变更类型 |
|--------|------|------|------|----------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 主键 | 不变 |
| token | VARCHAR(8) | NOT NULL, UNIQUE | 访问令牌 | 不变 |
| title | VARCHAR(100) | NOT NULL | 清单标题 | **新增** |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 | 不变 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 | 不变 |

#### 索引设计

- PRIMARY KEY: `id` (聚簇索引)
- UNIQUE INDEX: `idx_token` (token 唯一性索引)

---

### 1.2 todo_item 表变更

#### 新增字段

```sql
-- 新增 priority 和 due_date 字段
ALTER TABLE todo_item
ADD COLUMN priority VARCHAR(6) NOT NULL DEFAULT 'MEDIUM',
ADD COLUMN due_date DATE DEFAULT NULL;

-- 添加优先级约束检查
ALTER TABLE todo_item
ADD CONSTRAINT chk_priority CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW'));
```

#### 完整表结构

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

#### 字段说明

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

#### 索引设计

- PRIMARY KEY: `id` (聚簇索引)
- INDEX: `idx_list_id` (外键索引,优化查询性能)
- CONSTRAINT: `chk_priority` (CHECK 约束,确保 priority 值合法)

---

### 1.3 数据迁移策略

#### 迁移脚本 (V1 → V2-A)

```sql
-- =====================================================
-- V2-A 数据库迁移脚本
-- 执行前请备份数据库
-- =====================================================

-- 步骤 1: 添加 todo_list.title 字段
ALTER TABLE todo_list ADD COLUMN title VARCHAR(100) DEFAULT NULL;

-- 步骤 2: 为现有数据生成默认标题
UPDATE todo_list
SET title = CONCAT('我的清单 ', DATE_FORMAT(created_at, '%Y-%m-%d'))
WHERE title IS NULL;

-- 步骤 3: 设置 title 为必填
ALTER TABLE todo_list MODIFY COLUMN title VARCHAR(100) NOT NULL;

-- 步骤 4: 添加 todo_item.priority 字段
ALTER TABLE todo_item
ADD COLUMN priority VARCHAR(6) NOT NULL DEFAULT 'MEDIUM';

-- 步骤 5: 添加 todo_item.due_date 字段
ALTER TABLE todo_item
ADD COLUMN due_date DATE DEFAULT NULL;

-- 步骤 6: 添加优先级约束
ALTER TABLE todo_item
ADD CONSTRAINT chk_priority CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW'));

-- 步骤 7: 验证迁移
SELECT COUNT(*) AS total_lists FROM todo_list;
SELECT COUNT(*) AS lists_with_title FROM todo_list WHERE title IS NOT NULL;
SELECT COUNT(*) AS total_items FROM todo_item;
SELECT COUNT(*) AS items_with_priority FROM todo_item WHERE priority IS NOT NULL;
```

#### 回滚脚本 (V2-A → V1)

```sql
-- =====================================================
-- V2-A 回滚脚本 (谨慎使用)
-- 执行前请备份数据库
-- =====================================================

-- 步骤 1: 删除优先级约束
ALTER TABLE todo_item DROP CONSTRAINT chk_priority;

-- 步骤 2: 删除 due_date 字段
ALTER TABLE todo_item DROP COLUMN due_date;

-- 步骤 3: 删除 priority 字段
ALTER TABLE todo_item DROP COLUMN priority;

-- 步骤 4: 删除 title 字段
ALTER TABLE todo_list DROP COLUMN title;
```

---

## 2. API 契约设计

### 2.1 新增 API 端点

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
  "error": "Resource not found",
  "message": "List not found with token: abc123xyz456"
}
```

**业务逻辑**:
1. 根据 token 查询清单
2. 如果不存在,返回 404
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
  "error": "Invalid request",
  "message": "标题不能为空"
}
```

或

```json
{
  "error": "Invalid request",
  "message": "标题长度不能超过 100 个字符"
}
```

**错误响应** (404 Not Found):
```json
{
  "error": "Resource not found",
  "message": "List not found with token: abc123xyz456"
}
```

**业务逻辑**:
1. 验证 title: 非空、长度 ≤ 100
2. 验证失败返回 400
3. 根据 token 查询清单
4. 如果不存在,返回 404
5. 更新 title
6. 保存到数据库
7. 返回更新后的清单

---

### 2.2 扩展现有 API 端点（保持向后兼容）

#### API 3: 创建清单（扩展响应）

**端点**: `POST /api/lists`

**变更**: 响应中新增 `title` 字段

**请求示例**:
```http
POST /api/lists HTTP/1.1
Content-Type: application/json
```

**请求体**: 无（空请求体）

**成功响应** (201 Created):
```json
{
  "id": 1,
  "token": "abc123xyz456",
  "title": "我的清单 2026-02-18",
  "createdAt": "2026-02-18T10:00:00"
}
```

**字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 清单ID |
| token | string | 访问令牌 (8位) |
| title | string | 清单标题 (默认: "我的清单 yyyy-MM-dd") |
| createdAt | string | 创建时间 |

**向后兼容性**:
- V1 客户端忽略 `title` 字段,不影响现有功能

---

#### API 4: 获取清单详情（扩展响应）

**端点**: `GET /api/lists/{token}`

**变更**: 响应中新增 `title` 字段,items 中新增 `priority` 和 `dueDate` 字段

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| token | String | 是 | 待办清单的唯一访问令牌 |

**请求示例**:
```http
GET /api/lists/abc123xyz456 HTTP/1.1
```

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

**字段说明** (ListResponse):
| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 清单ID |
| token | string | 清单 token |
| title | string | 清单标题 |
| createdAt | string | 清单创建时间 |
| items | array | 事项列表 |

**字段说明** (ItemResponse):
| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 事项ID |
| title | string | 事项标题 |
| completed | boolean | 是否完成 |
| priority | string | 优先级 (HIGH/MEDIUM/LOW) |
| dueDate | string | 截止日期 (yyyy-MM-dd 或 null) |
| createdAt | string | 创建时间 |
| updatedAt | string | 更新时间 |

**向后兼容性**:
- V1 客户端忽略新增字段,不影响现有功能

---

#### API 5: 获取待办事项列表（扩展响应）

**端点**: `GET /api/lists/{token}/items`

**变更**: 响应中新增 `priority` 和 `dueDate` 字段

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| token | String | 是 | 待办清单的唯一访问令牌 |

**请求示例**:
```http
GET /api/lists/abc123xyz456/items HTTP/1.1
```

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
```

**向后兼容性**:
- V1 客户端忽略新增字段,不影响现有功能

---

#### API 6: 添加待办事项（扩展请求和响应）

**端点**: `POST /api/lists/{token}/items`

**变更**: 请求中新增 `priority` 和 `dueDate` 可选字段,响应中新增对应字段

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| token | String | 是 | 待办清单的唯一访问令牌 |

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

**错误响应** (400 Bad Request):
```json
{
  "error": "Invalid request",
  "message": "优先级值无效,必须为 HIGH、MEDIUM 或 LOW"
}
```

或

```json
{
  "error": "Invalid request",
  "message": "日期格式错误,必须为 yyyy-MM-dd"
}
```

**向后兼容性**:
- V1 客户端不传 `priority` 和 `dueDate`,服务端设置默认值
- V1 客户端忽略响应中的新增字段

---

#### API 7: 更新待办事项（扩展请求和响应）

**端点**: `PATCH /api/items/{id}`

**变更**: 请求中新增 `priority` 和 `dueDate` 可选字段,响应中新增对应字段

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 待办事项的唯一标识符 |

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
  "createdAt": "2026-02-18T10:01:00",
  "updatedAt": "2026-02-18T12:00:00"
}
```

**清除截止日期**:
```http
PATCH /api/items/1 HTTP/1.1
Content-Type: application/json

{
  "dueDate": null
}
```

**错误响应** (400 Bad Request):
```json
{
  "error": "Invalid request",
  "message": "请求体不能为空"
}
```

或

```json
{
  "error": "Invalid request",
  "message": "优先级值无效,必须为 HIGH、MEDIUM 或 LOW"
}
```

或

```json
{
  "error": "Invalid request",
  "message": "日期格式错误,必须为 yyyy-MM-dd"
}
```

**向后兼容性**:
- V1 客户端不传新增字段,不影响现有功能
- V1 客户端忽略响应中的新增字段

---

### 2.3 HTTP 状态码规范（沿用 V1）

| 状态码 | 场景 | 响应体 |
|--------|------|--------|
| 200 | 成功 (GET/PATCH) | 返回数据 |
| 201 | 创建成功 (POST) | 返回新创建的资源 |
| 204 | 删除成功 (DELETE) | 无响应体 |
| 400 | 参数错误 | `{ "error": "...", "message": "..." }` |
| 404 | 资源不存在 | `{ "error": "...", "message": "..." }` |
| 500 | 服务器错误 | `{ "error": "...", "message": "..." }` |

---

### 2.4 错误响应格式规范（沿用 V1）

**统一格式**:
```json
{
  "error": "ERROR_CODE",
  "message": "用户友好的错误提示"
}
```

**错误码定义**:
| 错误码 | HTTP 状态码 | 说明 |
|--------|-------------|------|
| Resource not found | 404 | 清单或事项不存在 |
| Invalid request | 400 | 标题为空或超长 |
| Invalid priority | 400 | 优先级值无效（不是 HIGH/MEDIUM/LOW） |
| Invalid dueDate | 400 | 截止日期格式错误或无效 |
| Failed to generate unique token | 500 | Token 生成失败 |
| Internal server error | 500 | 服务器内部错误 |

---

## 3. DTO 类设计

### 3.1 新增 DTO 类

#### UpdateListRequest

```java
package com.example.todolist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 更新清单标题请求 DTO
 */
public class UpdateListRequest {

    @NotBlank(message = "标题不能为空")
    @Size(min = 1, max = 100, message = "标题长度必须在 1-100 个字符之间")
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
```

---

### 3.2 修改现有 DTO 类

#### ListResponse

```java
package com.example.todolist.dto;

import com.example.todolist.entity.TodoList;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 清单响应 DTO
 */
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

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }

    // Getter 和 Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public List<ItemResponse> getItems() { return items; }
    public void setItems(List<ItemResponse> items) { this.items = items; }
}
```

---

#### ItemResponse

```java
package com.example.todolist.dto;

import com.example.todolist.entity.TodoItem;
import java.time.format.DateTimeFormatter;

/**
 * 事项响应 DTO
 */
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
        this.priority = item.getPriority().name();  // 新增: 枚举转字符串
        this.dueDate = item.getDueDate() != null
            ? item.getDueDate().toString()
            : null;  // 新增: LocalDate 转 yyyy-MM-dd
        this.createdAt = formatDateTime(item.getCreatedAt());
        this.updatedAt = formatDateTime(item.getUpdatedAt());
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }

    // Getter 和 Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
```

---

#### CreateItemRequest

```java
package com.example.todolist.dto;

import com.example.todolist.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 创建事项请求 DTO
 */
public class CreateItemRequest {

    @NotBlank(message = "标题不能为空")
    @Size(min = 1, max = 200, message = "标题长度必须在 1-200 个字符之间")
    private String title;

    private Priority priority = Priority.MEDIUM;  // 新增字段,默认 MEDIUM

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为 yyyy-MM-dd")
    private String dueDate;  // 新增字段,可选

    // Getter 和 Setter
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * 将 dueDate 字符串转换为 LocalDate
     * @return LocalDate 或 null
     */
    public LocalDate getDueDateAsLocalDate() {
        if (dueDate == null || dueDate.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dueDate);
        } catch (Exception e) {
            throw new IllegalArgumentException("日期格式错误,必须为 yyyy-MM-dd");
        }
    }
}
```

---

#### UpdateItemRequest

```java
package com.example.todolist.dto;

import com.example.todolist.entity.Priority;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 更新事项请求 DTO
 */
public class UpdateItemRequest {

    @Size(min = 1, max = 200, message = "标题长度必须在 1-200 个字符之间")
    private String title;

    private Boolean completed;

    private Priority priority;  // 新增字段

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为 yyyy-MM-dd")
    private String dueDate;  // 新增字段

    // Getter 和 Setter
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * 将 dueDate 字符串转换为 LocalDate
     * @return LocalDate 或 null
     */
    public LocalDate getDueDateAsLocalDate() {
        if (dueDate == null || dueDate.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dueDate);
        } catch (Exception e) {
            throw new IllegalArgumentException("日期格式错误,必须为 yyyy-MM-dd");
        }
    }

    /**
     * 验证请求体至少包含一个字段
     * @return true 如果请求体有效
     */
    public boolean isValid() {
        return title != null || completed != null || priority != null || dueDate != null;
    }
}
```

---

### 3.3 枚举类设计

#### Priority

```java
package com.example.todolist.entity;

/**
 * 优先级枚举
 */
public enum Priority {
    HIGH,
    MEDIUM,
    LOW
}
```

---

## 4. Entity 类更新

### 4.1 TodoList 实体

```java
package com.example.todolist.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "todo_list")
public class TodoList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 8)
    private String token;

    @Column(nullable = false, length = 100)  // 新增字段
    private String title;

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

    /**
     * 生成默认标题
     * 格式: "我的清单 yyyy-MM-dd"
     */
    private String generateDefaultTitle() {
        return "我的清单 " + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    // Getter 和 Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<TodoItem> getItems() { return items; }
    public void setItems(List<TodoItem> items) { this.items = items; }

    // 辅助方法
    public void addItem(TodoItem item) {
        items.add(item);
        item.setList(this);
    }

    public void removeItem(TodoItem item) {
        items.remove(item);
        item.setList(null);
    }
}
```

---

### 4.2 TodoItem 实体

```java
package com.example.todolist.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Enumerated(EnumType.STRING)  // 新增字段
    @Column(nullable = false, length = 6)
    private Priority priority = Priority.MEDIUM;

    @Column(name = "due_date")  // 新增字段
    private LocalDate dueDate;

    @Column(nullable = false)
    private Boolean completed = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 构造方法
    public TodoItem() {}

    public TodoItem(TodoList list, String title) {
        this.list = list;
        this.title = title;
        this.completed = false;
        this.priority = Priority.MEDIUM;  // 默认优先级
    }

    // Getter 和 Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @JsonIgnore
    public TodoList getList() { return list; }
    public void setList(TodoList list) { this.list = list; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

**注意**: `@JsonIgnore` 用于防止序列化时出现循环引用。

---

## 5. Service 层设计

### 5.1 ListService 更新

```java
package com.example.todolist.service;

import com.example.todolist.entity.TodoList;
import com.example.todolist.exception.NotFoundException;
import com.example.todolist.repository.TodoListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ListService {

    @Autowired
    private TodoListRepository listRepository;

    @Autowired
    private TokenService tokenService;

    /**
     * 创建新清单
     * 自动生成默认标题
     */
    public TodoList createList() {
        String token = tokenService.generateUniqueToken();
        TodoList list = new TodoList(token);  // 构造方法自动生成 title
        return listRepository.save(list);
    }

    /**
     * 根据 token 获取清单
     */
    @Transactional(readOnly = true)
    public TodoList getListByToken(String token) {
        return listRepository.findByToken(token)
            .orElseThrow(() -> new NotFoundException("List not found"));
    }

    /**
     * 更新清单标题 (V2-A 新增)
     */
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

    /**
     * 删除清单 (V2-A 新增)
     * 级联删除由 JPA 自动处理
     */
    public void deleteList(String token) {
        TodoList list = listRepository.findByToken(token)
            .orElseThrow(() -> new NotFoundException("List not found"));
        listRepository.delete(list);
    }
}
```

---

### 5.2 ItemService 更新

```java
package com.example.todolist.service;

import com.example.todolist.entity.Priority;
import com.example.todolist.entity.TodoItem;
import com.example.todolist.entity.TodoList;
import com.example.todolist.exception.NotFoundException;
import com.example.todolist.repository.TodoItemRepository;
import com.example.todolist.repository.TodoListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Service
@Transactional
public class ItemService {

    @Autowired
    private TodoListRepository listRepository;

    @Autowired
    private TodoItemRepository itemRepository;

    /**
     * 添加事项 (V2-A 扩展)
     * @param token 清单 token
     * @param title 事项标题
     * @param priority 优先级 (可为 null,默认 MEDIUM)
     * @param dueDate 截止日期 (可为 null)
     */
    public TodoItem addItem(String token, String title, Priority priority, LocalDate dueDate) {
        // 验证标题
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        title = title.trim();
        if (title.length() > 200) {
            throw new IllegalArgumentException("标题长度不能超过 200 个字符");
        }

        // 查找清单
        TodoList list = listRepository.findByToken(token)
            .orElseThrow(() -> new NotFoundException("List not found"));

        // 创建事项
        TodoItem item = new TodoItem(list, title);
        item.setPriority(priority != null ? priority : Priority.MEDIUM);
        item.setDueDate(dueDate);

        return itemRepository.save(item);
    }

    /**
     * 获取清单的所有事项
     */
    @Transactional(readOnly = true)
    public List<TodoItem> getItemsByToken(String token) {
        TodoList list = listRepository.findByToken(token)
            .orElseThrow(() -> new NotFoundException("List not found"));
        return itemRepository.findByListIdOrderByCreatedAtAsc(list.getId());
    }

    /**
     * 更新事项 (V2-A 扩展)
     * @param id 事项ID
     * @param title 标题 (可为 null)
     * @param completed 完成状态 (可为 null)
     * @param priority 优先级 (可为 null)
     * @param dueDateStr 截止日期字符串 (可为 null)
     */
    public TodoItem updateItem(Long id, String title, Boolean completed, Priority priority, String dueDateStr) {
        TodoItem item = itemRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Item not found"));

        // 更新标题
        if (title != null) {
            title = title.trim();
            if (title.isEmpty()) {
                throw new IllegalArgumentException("标题不能为空");
            }
            if (title.length() > 200) {
                throw new IllegalArgumentException("标题长度不能超过 200 个字符");
            }
            item.setTitle(title);
        }

        // 更新完成状态
        if (completed != null) {
            item.setCompleted(completed);
        }

        // 更新优先级
        if (priority != null) {
            item.setPriority(priority);
        }

        // 更新截止日期
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

    /**
     * 删除事项
     */
    public void deleteItem(Long id) {
        TodoItem item = itemRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Item not found"));
        itemRepository.delete(item);
    }

    /**
     * 解析日期字符串
     * @param dateStr 日期字符串 (yyyy-MM-dd)
     * @return LocalDate
     * @throws IllegalArgumentException 如果日期格式无效
     */
    private LocalDate parseDate(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            // 验证日期合理性 (如拒绝 2026-02-30)
            if (date.getYear() < 1900 || date.getYear() > 2100) {
                throw new IllegalArgumentException("日期年份必须在 1900-2100 之间");
            }
            return date;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日期格式错误,必须为 yyyy-MM-dd");
        }
    }
}
```

---

## 6. Controller 层设计

### 6.1 ListController 更新

```java
package com.example.todolist.controller;

import com.example.todolist.dto.ListResponse;
import com.example.todolist.dto.UpdateListRequest;
import com.example.todolist.entity.TodoList;
import com.example.todolist.service.ListService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lists")
public class ListController {

    @Autowired
    private ListService listService;

    /**
     * 创建清单
     * POST /api/lists
     */
    @PostMapping
    public ResponseEntity<ListResponse> createList() {
        TodoList list = listService.createList();
        return ResponseEntity.status(201).body(new ListResponse(list));
    }

    /**
     * 获取清单详情
     * GET /api/lists/{token}
     */
    @GetMapping("/{token}")
    public ResponseEntity<ListResponse> getList(@PathVariable String token) {
        TodoList list = listService.getListByToken(token);
        return ResponseEntity.ok(new ListResponse(list));
    }

    /**
     * 更新清单标题 (V2-A 新增)
     * PATCH /api/lists/{token}
     */
    @PatchMapping("/{token}")
    public ResponseEntity<ListResponse> updateList(
        @PathVariable String token,
        @RequestBody @Valid UpdateListRequest request
    ) {
        TodoList list = listService.updateListTitle(token, request.getTitle());
        return ResponseEntity.ok(new ListResponse(list));
    }

    /**
     * 删除清单 (V2-A 新增)
     * DELETE /api/lists/{token}
     */
    @DeleteMapping("/{token}")
    public ResponseEntity<Void> deleteList(@PathVariable String token) {
        listService.deleteList(token);
        return ResponseEntity.noContent().build();
    }
}
```

---

### 6.2 ItemController 更新

```java
package com.example.todolist.controller;

import com.example.todolist.dto.CreateItemRequest;
import com.example.todolist.dto.ItemResponse;
import com.example.todolist.dto.UpdateItemRequest;
import com.example.todolist.entity.Priority;
import com.example.todolist.entity.TodoItem;
import com.example.todolist.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lists/{token}/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    /**
     * 添加事项 (V2-A 扩展)
     * POST /api/lists/{token}/items
     */
    @PostMapping
    public ResponseEntity<ItemResponse> addItem(
        @PathVariable String token,
        @RequestBody @Valid CreateItemRequest request
    ) {
        Priority priority = request.getPriority();
        var dueDate = request.getDueDateAsLocalDate();

        TodoItem item = itemService.addItem(token, request.getTitle(), priority, dueDate);
        return ResponseEntity.status(201).body(new ItemResponse(item));
    }

    /**
     * 获取清单的所有事项
     * GET /api/lists/{token}/items
     */
    @GetMapping
    public ResponseEntity<List<ItemResponse>> getItems(@PathVariable String token) {
        List<TodoItem> items = itemService.getItemsByToken(token);
        List<ItemResponse> responses = items.stream()
            .map(ItemResponse::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}

@RestController
@RequestMapping("/api/items")
public class ItemManagementController {

    @Autowired
    private ItemService itemService;

    /**
     * 更新事项 (V2-A 扩展)
     * PATCH /api/items/{id}
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ItemResponse> updateItem(
        @PathVariable Long id,
        @RequestBody @Valid UpdateItemRequest request
    ) {
        // 验证请求体至少包含一个字段
        if (!request.isValid()) {
            return ResponseEntity.badRequest().build();
        }

        var dueDateStr = request.getDueDate();
        TodoItem item = itemService.updateItem(
            id,
            request.getTitle(),
            request.getCompleted(),
            request.getPriority(),
            dueDateStr
        );
        return ResponseEntity.ok(new ItemResponse(item));
    }

    /**
     * 删除事项
     * DELETE /api/items/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## 7. 异常处理更新

### 7.1 全局异常处理器

```java
package com.example.todolist.exception;

import com.example.todolist.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(404)
            .body(new ErrorResponse("Resource not found", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException e) {
        String message = e.getMessage();
        String error = "Invalid request";

        // 根据错误消息定制错误码
        if (message.contains("优先级")) {
            error = "Invalid priority";
        } else if (message.contains("日期")) {
            error = "Invalid dueDate";
        } else if (message.contains("标题")) {
            error = "Invalid title";
        }

        return ResponseEntity.status(400)
            .body(new ErrorResponse(error, message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        return ResponseEntity.status(500)
            .body(new ErrorResponse("Failed to generate unique token", "系统繁忙,请稍后重试"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleInternalServerError(Exception e) {
        e.printStackTrace(); // 开发环境打印堆栈
        return ResponseEntity.status(500)
            .body(new ErrorResponse("Internal server error", "系统繁忙,请稍后重试"));
    }
}
```

---

## 8. 向后兼容性保证

### 8.1 数据库层面

| 策略 | 说明 |
|------|------|
| 新增字段 | 所有新增字段都设置默认值 |
| 级联删除 | V1 已有 `ON DELETE CASCADE`,V2-A 继续使用 |
| 字段类型 | 不修改现有字段类型 |
| 索引 | 不删除现有索引 |

---

### 8.2 API 层面

| 策略 | 说明 |
|------|------|
| 新增字段 | 所有响应新增字段,V1 客户端可忽略 |
| 可选参数 | 请求中新增字段都是可选的 |
| 默认值 | 新增字段不传时使用默认值 |
| HTTP 状态码 | 沿用 V1 状态码规范 |
| 错误响应 | 沿用 V1 错误响应格式 |

---

### 8.3 业务逻辑层面

| 场景 | V1 客户端行为 | V2-A 处理 |
|------|--------------|-----------|
| 创建清单 | 不传 title | 自动生成默认标题 |
| 创建事项 | 不传 priority, dueDate | priority 默认 MEDIUM,dueDate 默认 null |
| 更新事项 | 只传 completed | 只更新 completed,其他字段保持不变 |
| 获取清单 | 忽略 title, priority, dueDate | 正常返回,客户端忽略新字段 |

---

### 8.4 测试场景

#### 向后兼容性测试用例

1. **V1 客户端创建清单**
   ```bash
   curl -X POST http://localhost:8080/api/lists
   ```
   预期: 响应包含 title 字段,但 V1 客户端忽略

2. **V1 客户端添加事项（不传新字段）**
   ```bash
   curl -X POST http://localhost:8080/api/lists/{token}/items \
     -H "Content-Type: application/json" \
     -d '{"title": "买牛奶"}'
   ```
   预期: 响应包含 priority=MEDIUM 和 dueDate=null,但 V1 客户端忽略

3. **V1 客户端更新事项（只传 completed）**
   ```bash
   curl -X PATCH http://localhost:8080/api/items/1 \
     -H "Content-Type: application/json" \
     -d '{"completed": true}'
   ```
   预期: 只更新 completed,priority 和 dueDate 保持不变

---

## 9. API Freeze 前检查

### 9.1 API 端点完整性检查

| API 端点 | 状态 | 版本 | 说明 |
|----------|------|------|------|
| POST /api/lists | ✅ 已定义 | V1 | 创建清单 (响应扩展) |
| GET /api/lists/{token} | ✅ 已定义 | V1 | 获取清单详情 (响应扩展) |
| PATCH /api/lists/{token} | ✅ 新增 | V2-A | 更新清单标题 |
| DELETE /api/lists/{token} | ✅ 新增 | V2-A | 删除清单 |
| GET /api/lists/{token}/items | ✅ 已定义 | V1 | 获取清单的所有事项 (响应扩展) |
| POST /api/lists/{token}/items | ✅ 已定义 | V1 | 添加事项 (请求/响应扩展) |
| PATCH /api/items/{id} | ✅ 已定义 | V1 | 更新事项 (请求/响应扩展) |
| DELETE /api/items/{id} | ✅ 已定义 | V1 | 删除事项 |

**结论**: 所有 API 端点已完整定义,无遗漏。

---

### 9.2 数据结构完整性检查

**Entity**:
- ✅ TodoList (id, token, title, createdAt, updatedAt) - title 新增
- ✅ TodoItem (id, listId, title, priority, dueDate, completed, createdAt, updatedAt) - priority, dueDate 新增

**DTO (Request)**:
- ✅ CreateItemRequest (title, priority, dueDate) - priority, dueDate 新增
- ✅ UpdateItemRequest (title, completed, priority, dueDate) - priority, dueDate 新增
- ✅ UpdateListRequest (title) - 新增

**DTO (Response)**:
- ✅ ListResponse (id, token, title, createdAt, items[]) - title 新增
- ✅ ItemResponse (id, title, completed, priority, dueDate, createdAt, updatedAt) - priority, dueDate 新增
- ✅ ErrorResponse (error, message)

**枚举**:
- ✅ Priority (HIGH, MEDIUM, LOW) - 新增

**结论**: 所有数据结构已完整定义,无遗漏。

---

### 9.3 业务规则完整性检查

| 业务规则 | 实现位置 | 状态 |
|----------|----------|------|
| Token 唯一性 | TokenService + 数据库唯一约束 | ✅ (V1) |
| 默认标题生成 | TodoList 构造方法 | ✅ (V2-A) |
| 标题长度限制 (1-100) | UpdateListRequest 验证 | ✅ (V2-A) |
| 事项标题长度限制 (1-200) | CreateItemRequest/UpdateItemRequest 验证 | ✅ (V1) |
| Priority 默认值 | TodoItem 实体 + CreateItemRequest | ✅ (V2-A) |
| Priority 枚举验证 | 数据库 CHECK 约束 + Service 层 | ✅ (V2-A) |
| DueDate 格式验证 | CreateItemRequest/UpdateItemRequest + Service 层 | ✅ (V2-A) |
| DueDate 可为空 | TodoItem 实体 | ✅ (V2-A) |
| 级联删除 | JPA CascadeType.ALL | ✅ (V1) |
| HTTP 状态码规范 | Controller 层 | ✅ (V1) |

**结论**: 所有业务规则已明确定义,无遗漏。

---

### 9.4 场景完整性检查

| 场景 | API 覆盖 | 状态 |
|------|----------|------|
| 创建清单 | POST /api/lists | ✅ (V1, 响应扩展) |
| 获取清单详情 | GET /api/lists/{token} | ✅ (V1, 响应扩展) |
| 删除清单 | DELETE /api/lists/{token} | ✅ (V2-A) |
| 更新清单标题 | PATCH /api/lists/{token} | ✅ (V2-A) |
| 添加事项（含优先级/截止日期） | POST /api/lists/{token}/items | ✅ (V1, 请求扩展) |
| 更新事项（含优先级/截止日期） | PATCH /api/items/{id} | ✅ (V1, 请求扩展) |
| 清除截止日期 | PATCH /api/items/{id} {dueDate: null} | ✅ (V2-A) |
| Token 不存在 | 所有 GET/POST/PATCH/DELETE | ✅ (404) |
| Item ID 不存在 | PATCH/DELETE | ✅ (404) |
| 标题为空 | POST/PATCH | ✅ (400) |
| 标题超长 | POST/PATCH | ✅ (400) |
| Priority 无效值 | POST/PATCH | ✅ (400) |
| DueDate 无效格式 | POST/PATCH | ✅ (400) |
| DueDate 无效日期 | POST/PATCH | ✅ (400) |

**结论**: 所有可能的场景均已覆盖,无遗漏。

---

## 10. 实现要点

### 10.1 数据库迁移要点

1. **开发环境**: 使用 `spring.jpa.hibernate.ddl-auto=update`
2. **生产环境**: 使用 Flyway 或 Liquibase 管理迁移脚本
3. **数据迁移**: 为现有数据设置默认标题
4. **回滚准备**: 准备回滚脚本以防万一

---

### 10.2 日期处理要点

1. **数据库类型**: `DATE` (只存储日期,不存储时间)
2. **Java 类型**: `LocalDate`
3. **JSON 格式**: `yyyy-MM-dd` (ISO 8601 日期格式)
4. **验证**: 正则表达式 `^\d{4}-\d{2}-\d{2}$`
5. **解析**: `LocalDate.parse(dateStr)`
6. **时区**: 不涉及时区问题 (只存储日期)

---

### 10.3 优先级处理要点

1. **数据库存储**: `VARCHAR(6)` (HIGH/MEDIUM/LOW)
2. **Java 类型**: `enum Priority`
3. **枚举映射**: `@Enumerated(EnumType.STRING)`
4. **验证**: 数据库 CHECK 约束 + Service 层验证
5. **默认值**: `Priority.MEDIUM`

---

### 10.4 向后兼容性要点

1. **响应扩展**: 新增字段,V1 客户端忽略
2. **请求扩展**: 新增字段可选,不传时使用默认值
3. **默认值策略**: title 自动生成,priority 默认 MEDIUM,dueDate 默认 null
4. **部分更新**: PATCH 只更新提供的字段
5. **错误处理**: 沿用 V1 错误响应格式

---

### 10.5 性能优化要点

1. **索引**: idx_token (token 唯一性索引)
2. **级联删除**: 数据库层面 `ON DELETE CASCADE`
3. **懒加载**: `@ManyToOne(fetch = FetchType.LAZY)`
4. **只读事务**: 查询方法使用 `@Transactional(readOnly = true)`

---

## 11. 测试建议

### 11.1 单元测试

- ✅ TokenService 生成逻辑
- ✅ ListService 新方法 (updateListTitle, deleteList)
- ✅ ItemService 更新方法 (priority, dueDate)
- ✅ 日期解析逻辑
- ✅ 优先级枚举转换

---

### 11.2 集成测试

- ✅ 所有 API 端点
- ✅ 向后兼容性场景
- ✅ 错误场景 (400, 404)
- ✅ 数据库迁移脚本

---

### 11.3 手动测试场景

参考 PRD_V2A.md 第 7 节 "测试场景"。

---

## 12. 开发计划

### 12.1 开发顺序

**阶段 1: 数据库和实体 (1 天)**
1. 创建 Priority 枚举
2. 更新 Entity 类（TodoList、TodoItem）
3. 执行数据库迁移
4. 验证数据库变更

**阶段 2: DTO 层 (0.5 天)**
1. 创建 UpdateListRequest
2. 更新 ListResponse、ItemResponse
3. 更新 CreateItemRequest、UpdateItemRequest

**阶段 3: Service 层 (1 天)**
1. 实现 ListService 新方法
2. 更新 ItemService 方法
3. 添加参数验证逻辑

**阶段 4: Controller 层 (0.5 天)**
1. 实现 ListController 新端点
2. 更新现有 Controller 方法
3. 测试所有 API

**阶段 5: 前端实现 (2 天)**
1. 实现清单标题编辑
2. 实现清单删除功能
3. 实现优先级选择和显示
4. 实现截止日期选择和显示

**阶段 6: 联调与测试 (1 天)**
1. 端到端测试所有新功能
2. 测试向后兼容性
3. 测试错误场景
4. 代码优化

**总工期**: 约 6 天

---

### 12.2 技术栈版本

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | LTS 版本 |
| Spring Boot | 3.x | 最新稳定版 |
| Spring Data JPA | 3.x | 随 Spring Boot |
| H2 Database | 2.x | 开发环境 |
| Thymeleaf | 3.x | 随 Spring Boot |

---

## 13. 附录

### 13.1 测试 API 的 curl 命令

#### V2-A 新增 API

**删除清单**:
```bash
curl -X DELETE http://localhost:8080/api/lists/aB3xK9mP
```

**更新清单标题**:
```bash
curl -X PATCH http://localhost:8080/api/lists/aB3xK9mP \
  -H "Content-Type: application/json" \
  -d '{"title": "家庭购物清单"}'
```

#### V1 API 扩展

**创建清单（自动生成标题）**:
```bash
curl -X POST http://localhost:8080/api/lists
```

**添加事项（设置优先级和截止日期）**:
```bash
curl -X POST http://localhost:8080/api/lists/aB3xK9mP/items \
  -H "Content-Type: application/json" \
  -d '{"title": "买牛奶", "priority": "HIGH", "dueDate": "2026-02-20"}'
```

**更新事项（更新优先级和截止日期）**:
```bash
curl -X PATCH http://localhost:8080/api/items/1 \
  -H "Content-Type: application/json" \
  -d '{"priority": "LOW", "dueDate": "2026-02-25"}'
```

**清除截止日期**:
```bash
curl -X PATCH http://localhost:8080/api/items/1 \
  -H "Content-Type: application/json" \
  -d '{"dueDate": null}'
```

---

### 13.2 常见问题 FAQ

**Q1: 标题长度限制是多少?**
A: 清单标题 1-100 字符,事项标题 1-200 字符。

**Q2: 优先级如何排序显示?**
A: PRD 未要求排序,按创建时间升序显示即可。

**Q3: 截止日期如何验证有效性?**
A: 使用 `LocalDate.parse()`,会自动拒绝无效日期 (如 2026-02-30)。

**Q4: 如何清除截止日期?**
A: 发送 `PATCH /api/items/{id}`,请求体 `{"dueDate": null}`。

**Q5: V1 客户端如何处理新字段?**
A: JSON 解析器会忽略未知字段,不影响现有功能。

**Q6: 数据库迁移会丢失数据吗?**
A: 不会,迁移脚本只是添加新字段,不删除或修改现有数据。

**Q7: 如何回滚 V2-A?**
A: 执行回滚脚本,删除新增字段 (参考 1.3 节)。

---

### 13.3 参考资料

- [V2-A PRD](/d/develop/project/todolist/PRD_V2A.md)
- [V1 API Contract](/d/develop/project/todolist/API_CONTRACT.md)
- [V1 Tech Design](/d/develop/project/todolist/TECH_DESIGN.md)
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Spring Data JPA 文档](https://spring.io/projects/spring-data-jpa)
- [Thymeleaf 文档](https://www.thymeleaf.org/)
- [Java LocalDate 文档](https://docs.oracle.com/javase/8/docs/api/java/time/LocalDate.html)

---

## 14. 冻结声明

本文档定义的所有 API 契约自发布之日起冻结,后续开发过程中不得修改:

**API 端点 (冻结)**:
- DELETE /api/lists/{token} (新增)
- PATCH /api/lists/{token} (新增)
- POST /api/lists (响应扩展)
- GET /api/lists/{token} (响应扩展)
- GET /api/lists/{token}/items (响应扩展)
- POST /api/lists/{token}/items (请求/响应扩展)
- PATCH /api/items/{id} (请求/响应扩展)
- DELETE /api/items/{id} (不变)

**数据结构 (冻结)**:
- TodoList Entity (新增 title)
- TodoItem Entity (新增 priority, dueDate)
- Priority 枚举 (新增)
- UpdateListRequest DTO (新增)
- ListResponse DTO (新增 title)
- ItemResponse DTO (新增 priority, dueDate)
- CreateItemRequest DTO (新增 priority, dueDate)
- UpdateItemRequest DTO (新增 priority, dueDate)

**业务规则 (冻结)**:
- 默认标题生成规则 ("我的清单 yyyy-MM-dd")
- Priority 枚举值 (HIGH/MEDIUM/LOW)
- Priority 默认值 (MEDIUM)
- DueDate 格式 (yyyy-MM-dd)
- DueDate 默认值 (null)
- 清单标题验证规则 (1-100 字符)
- HTTP 状态码规范 (沿用 V1)
- 错误处理规范 (沿用 V1)

**向后兼容性 (冻结)**:
- 所有 V1 API 继续可用
- 新增字段都是可选的
- 新增字段都有默认值
- V1 客户端可忽略新字段

如有变更需求,需经 PM 和 TechLead 共同评审后发布新版本文档。

---

**文档状态**: API 冻结前最终版
**技术负责人**: TechLead
**发布日期**: 2026-02-18
**文档版本**: 2.0-A

---

**文档结束**
