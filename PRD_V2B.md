# 共享待办清单 V2-B - 产品需求文档 (PRD)

**文档版本**: 2.0-B
**创建日期**: 2026-02-19
**项目类型**: 协作基础版本
**产品负责人**: PM
**文档状态**: Scope Freeze

---

## 1. 版本概述

### 1.1 V2-B 目标

在 V2-A 基础上，引入多用户协作基础能力，实现用户模型、成员关系、邀请机制和角色权限。

**核心原则**：
- 保持简单可运行
- 不破坏 V1/V2-A API
- 串行执行，Scope Freeze 后不接受变更
- 最小可行协作功能

### 1.2 功能总览

| 功能模块 | 优先级 | 说明 |
|---------|--------|------|
| 用户模型 | P0 | 基础用户（仅用户名） |
| 清单成员关系 | P0 | 用户属于清单 |
| 邀请令牌机制 | P0 | 生成邀请链接/令牌 |
| 角色权限 | P0 | 所有者/成员权限区分 |
| 成员列表查看 | P0 | 查看清单所有成员 |

### 1.3 与 V1/V2-A 的关系

**V1/V2-A 功能保留**：
- 清单创建与分享
- 待办事项的所有管理功能（添加、编辑、删除、完成状态、优先级、截止日期）
- Token 机制（继续用于公开访问）
- 所有 V1/V2-A API 继续可用

**V2-B 新增能力**：
- 用户识别（用户名）
- 清单成员管理
- 邀请机制
- 基于角色的权限控制

---

## 2. 功能范围定义

### 2.1 用户模型 (P0)

**用户故事**: 作为一名用户，我想用简单的用户名标识自己，以便在协作中被识别。

**功能描述**：
- 极简用户模型，只有用户名（无需密码、邮箱、注册流程）
- 用户首次访问时生成临时用户名（如"用户_abc123"）
- 用户可以修改自己的用户名
- 用户名用于标识 todo 创建者和编辑者

**数据字段**：
```
User {
  id: Long
  username: String (50字符，唯一)
  createdAt: LocalDateTime
}
```

**用户交互流程**：
```
用户首次访问应用
  ↓
后端自动创建用户，生成临时用户名
  ↓
前端存储 userId 到 localStorage
  ↓
用户可以在设置中修改用户名
  ↓
后续请求携带 userId
```

**API 设计**：
```
POST /api/users
Response: 201 Created
{
  "id": 1,
  "username": "用户_abc123",
  "createdAt": "2026-02-19T10:00:00"
}

PATCH /api/users/{id}
Request: { "username": "新用户名" }
Response: 200 OK
{
  "id": 1,
  "username": "新用户名",
  "updatedAt": "2026-02-19T11:00:00"
}
```

**验收标准**：
- [ ] 首次访问自动创建用户
- [ ] 用户名唯一性验证
- [ ] 用户可以修改用户名
- [ ] 用户名长度 1-50 字符
- [ ] userId 存储在 localStorage
- [ ] 临时用户名格式友好（如"用户_随机6位字符"）

---

### 2.2 清单成员关系 (P0)

**用户故事**: 作为一名清单所有者，我想管理哪些用户可以协作我的清单。

**功能描述**：
- 清单与用户是多对多关系
- 每个成员关系包含角色信息（所有者/成员）
- 清单创建时自动成为所有者
- 一个用户可以属于多个清单
- 一个清单可以有多个成员

**数据字段**：
```
ListMember {
  id: Long
  listId: Long (外键 -> todo_list)
  userId: Long (外键 -> user)
  role: String (枚举: OWNER/MEMBER)
  joinedAt: LocalDateTime
}
```

**角色定义**：
| 角色 | 权限 | 说明 |
|------|------|------|
| OWNER | 添加/移除成员、编辑清单、编辑所有 todos | 清单创建者，可以有多个 |
| MEMBER | 编辑清单、编辑 todos | 被邀请的协作者 |

**API 设计**：
```
GET /api/lists/{token}/members
Response: 200 OK
[
  {
    "id": 1,
    "userId": 1,
    "username": "张三",
    "role": "OWNER",
    "joinedAt": "2026-02-19T10:00:00"
  },
  {
    "id": 2,
    "userId": 2,
    "username": "李四",
    "role": "MEMBER",
    "joinedAt": "2026-02-19T11:00:00"
  }
]
```

**验收标准**：
- [ ] 清单创建时创建者成为 OWNER
- [ ] 一个清单可以有多个 OWNER
- [ ] 一个用户可以多次加入同一清单（不允许）
- [ ] 成员关系持久化
- [ ] 可以查询清单的所有成员

---

### 2.3 邀请令牌机制 (P0)

**用户故事**: 作为一名清单所有者，我想生成邀请链接，以便其他人可以加入协作。

**功能描述**：
- 所有者可以生成邀请令牌
- 邀请令牌是唯一的随机字符串（12位）
- 用户通过邀请令牌加入清单
- 邀请令牌可以设置过期时间（可选，V2-B 暂不实现）
- 邀请令牌一次性使用后失效（可选，V2-B 暂不实现，令牌可复用）

**数据字段**：
```
InviteToken {
  id: Long
  listId: Long (外键 -> todo_list)
  token: String (12位，唯一)
  createdBy: Long (外键 -> user)
  createdAt: LocalDateTime
  maxUses: Integer (可选，V2-B 暂不限制)
  useCount: Integer (默认 0，V2-B 暂不统计)
}
```

**用户交互流程**：
```
清单所有者点击"邀请成员"
  ↓
后端生成邀请令牌
  ↓
前端显示邀请链接（如：/join?invite=xyz123abc456）
  ↓
所有者分享链接给其他用户
  ↓
其他用户访问链接
  ↓
后端验证令牌，添加用户为成员
  ↓
重定向到清单详情页
```

**API 设计**：
```
POST /api/lists/{token}/invites
Response: 201 Created
{
  "inviteToken": "xyz123abc456",
  "inviteUrl": "http://localhost:8080/join?invite=xyz123abc456",
  "createdAt": "2026-02-19T10:00:00"
}

POST /api/lists/join
Request: { "inviteToken": "xyz123abc456" }
Response: 200 OK
{
  "listToken": "abc123xyz456",
  "role": "MEMBER"
}
```

**验收标准**：
- [ ] 所有者可以生成邀请令牌
- [ ] 邀请令牌是 12 位随机字符串
- [ ] 用户可以通过令牌加入清单
- [ ] 加入后用户成为 MEMBER 角色
- [ ] 重复使用同一令牌返回错误或成功（可复用）
- [ ] 无效令牌返回 404

---

### 2.4 角色权限控制 (P0)

**用户故事**: 作为一名清单所有者，我想控制成员的权限，以便管理协作。

**功能描述**：
- OWNER 可以添加/移除成员
- MEMBER 不能添加/移除成员
- OWNER 和 MEMBER 都可以编辑清单和 todos
- 非成员用户不能编辑（V2-B 暂不限制公开访问）

**权限矩阵**：
| 操作 | OWNER | MEMBER | 非成员 |
|------|-------|--------|--------|
| 查看清单 | ✅ | ✅ | ✅ |
| 查看成员列表 | ✅ | ✅ | ✅ |
| 编辑清单标题 | ✅ | ✅ | ✅ |
| 添加/编辑/删除 todo | ✅ | ✅ | ✅ |
| 生成邀请令牌 | ✅ | ❌ | ❌ |
| 移除成员 | ✅ | ❌ | ❌ |

**API 设计**：
```
DELETE /api/lists/{token}/members/{userId}
权限: 仅 OWNER
Response: 204 No Content

错误响应 (403 Forbidden):
{
  "error": "Forbidden",
  "message": "只有清单所有者可以移除成员"
}
```

**验收标准**：
- [ ] OWNER 可以生成邀请令牌
- [ ] MEMBER 不能生成邀请令牌
- [ ] OWNER 可以移除成员
- [ ] MEMBER 不能移除成员
- [ ] OWNER 和 MEMBER 都可以编辑 todos
- [ ] 权限验证在 API 层执行
- [ ] 权限不足返回 403

---

### 2.5 成员列表查看 (P0)

**用户故事**: 作为一名清单用户，我想查看清单的所有成员，以便知道谁在协作。

**功能描述**：
- 清单详情页显示成员列表
- 显示成员用户名和角色
- OWNER 可以看到移除成员按钮
- MEMBER 只能看到成员列表

**UI 展示**：
```
成员 (3)
👤 张三 (所有者)  [移除]
👤 李四 (成员)
👤 王五 (成员)  [移除]
```

**API 设计**：
```
GET /api/lists/{token}/members
Response: 200 OK
[
  {
    "id": 1,
    "userId": 1,
    "username": "张三",
    "role": "OWNER",
    "joinedAt": "2026-02-19T10:00:00"
  },
  {
    "id": 2,
    "userId": 2,
    "username": "李四",
    "role": "MEMBER",
    "joinedAt": "2026-02-19T11:00:00"
  }
]
```

**验收标准**：
- [ ] 清单详情页显示成员列表
- [ ] 显示成员用户名
- [ ] 显示成员角色（所有者/成员）
- [ ] OWNER 可以移除其他成员
- [ ] 不能移除自己（V2-B 暂不实现退出功能）
- [ ] 成员按加入时间排序

---

## 3. 数据库设计

### 3.1 新增表：user

```sql
CREATE TABLE user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名(唯一)',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

**字段说明**：
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 主键 |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 用户名 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

---

### 3.2 新增表：list_member

```sql
CREATE TABLE list_member (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  list_id BIGINT NOT NULL COMMENT '清单ID(外键)',
  user_id BIGINT NOT NULL COMMENT '用户ID(外键)',
  role VARCHAR(6) NOT NULL DEFAULT 'MEMBER' COMMENT '角色(OWNER/MEMBER)',
  joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  FOREIGN KEY (list_id) REFERENCES todo_list(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
  UNIQUE KEY uk_list_user (list_id, user_id),
  INDEX idx_list_id (list_id),
  INDEX idx_user_id (user_id),
  CONSTRAINT chk_role CHECK (role IN ('OWNER', 'MEMBER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='清单成员关系表';
```

**字段说明**：
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 主键 |
| list_id | BIGINT | NOT NULL, FOREIGN KEY | 清单ID |
| user_id | BIGINT | NOT NULL, FOREIGN KEY | 用户ID |
| role | VARCHAR(6) | NOT NULL, DEFAULT 'MEMBER', CHECK | 角色 |
| joined_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 加入时间 |

**唯一约束**：
- `(list_id, user_id)` 组合唯一，防止用户重复加入同一清单

---

### 3.3 新增表：invite_token

```sql
CREATE TABLE invite_token (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  list_id BIGINT NOT NULL COMMENT '清单ID(外键)',
  token VARCHAR(12) NOT NULL UNIQUE COMMENT '邀请令牌(12位)',
  created_by BIGINT NOT NULL COMMENT '创建者用户ID(外键)',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  FOREIGN KEY (list_id) REFERENCES todo_list(id) ON DELETE CASCADE,
  FOREIGN KEY (created_by) REFERENCES user(id) ON DELETE CASCADE,
  INDEX idx_token (token),
  INDEX idx_list_id (list_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请令牌表';
```

**字段说明**：
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 主键 |
| list_id | BIGINT | NOT NULL, FOREIGN KEY | 清单ID |
| token | VARCHAR(12) | NOT NULL, UNIQUE | 邀请令牌 |
| created_by | BIGINT | NOT NULL, FOREIGN KEY | 创建者用户ID |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

---

### 3.4 现有表变更

**todo_item 表新增字段**：
```sql
ALTER TABLE todo_item
ADD COLUMN created_by_id BIGINT DEFAULT NULL COMMENT '创建者用户ID(外键)',
ADD COLUMN updated_by_id BIGINT DEFAULT NULL COMMENT '最后更新者用户ID(外键)';

ALTER TABLE todo_item
ADD FOREIGN KEY (created_by_id) REFERENCES user(id) ON DELETE SET NULL,
ADD FOREIGN KEY (updated_by_id) REFERENCES user(id) ON DELETE SET NULL;
```

**字段说明**：
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| created_by_id | BIGINT | FOREIGN KEY, nullable | 创建者用户ID |
| updated_by_id | BIGINT | FOREIGN KEY, nullable | 最后更新者用户ID |

---

## 4. API 设计

### 4.1 新增 API 端点

#### API 1: 创建用户

**端点**: `POST /api/users`

**功能**: 创建新用户（首次访问时自动调用）

**请求示例**:
```http
POST /api/users HTTP/1.1
Content-Type: application/json

{
  "username": "用户_abc123"  // 可选，不传则自动生成
}
```

**请求体** (CreateUserRequest):
| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| username | String | 否 | 1-50 字符 | 用户名（可选） |

**成功响应** (201 Created):
```json
{
  "id": 1,
  "username": "用户_abc123",
  "createdAt": "2026-02-19T10:00:00"
}
```

**错误响应** (400 Bad Request):
```json
{
  "error": "Username already exists",
  "message": "用户名已存在"
}
```

**业务逻辑**:
1. 如果提供了 username，验证唯一性
2. 如果未提供 username，自动生成（格式："用户_" + 6位随机字符）
3. 创建用户
4. 返回用户信息

---

#### API 2: 更新用户名

**端点**: `PATCH /api/users/{id}`

**功能**: 修改用户名

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 用户ID |

**请求示例**:
```http
PATCH /api/users/1 HTTP/1.1
Content-Type: application/json

{
  "username": "张三"
}
```

**请求体** (UpdateUserRequest):
| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| username | String | 是 | 1-50 字符，唯一 | 新用户名 |

**成功响应** (200 OK):
```json
{
  "id": 1,
  "username": "张三",
  "updatedAt": "2026-02-19T11:00:00"
}
```

**错误响应** (404 Not Found):
```json
{
  "error": "User not found",
  "message": "用户不存在"
}
```

---

#### API 3: 获取清单成员列表

**端点**: `GET /api/lists/{token}/members`

**功能**: 查询清单的所有成员

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| token | String | 是 | 清单token |

**成功响应** (200 OK):
```json
[
  {
    "id": 1,
    "userId": 1,
    "username": "张三",
    "role": "OWNER",
    "joinedAt": "2026-02-19T10:00:00"
  },
  {
    "id": 2,
    "userId": 2,
    "username": "李四",
    "role": "MEMBER",
    "joinedAt": "2026-02-19T11:00:00"
  }
]
```

---

#### API 4: 生成邀请令牌

**端点**: `POST /api/lists/{token}/invites`

**功能**: 生成邀请令牌（仅 OWNER 可调用）

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| token | String | 是 | 清单token |

**请求头**:
| 头 | 值 | 说明 |
|----|-----|------|
| X-User-Id | 1 | 当前用户ID |

**成功响应** (201 Created):
```json
{
  "inviteToken": "xyz123abc456",
  "inviteUrl": "http://localhost:8080/join?invite=xyz123abc456",
  "createdAt": "2026-02-19T10:00:00"
}
```

**错误响应** (403 Forbidden):
```json
{
  "error": "Forbidden",
  "message": "只有清单所有者可以生成邀请令牌"
}
```

---

#### API 5: 通过邀请令牌加入清单

**端点**: `POST /api/lists/join`

**功能**: 用户通过邀请令牌加入清单

**请求示例**:
```http
POST /api/lists/join HTTP/1.1
Content-Type: application/json
X-User-Id: 2

{
  "inviteToken": "xyz123abc456"
}
```

**请求体** (JoinListRequest):
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| inviteToken | String | 是 | 邀请令牌（12位） |

**请求头**:
| 头 | 值 | 说明 |
|----|-----|------|
| X-User-Id | 2 | 当前用户ID |

**成功响应** (200 OK):
```json
{
  "listToken": "abc123xyz456",
  "role": "MEMBER",
  "message": "成功加入清单"
}
```

**错误响应** (404 Not Found):
```json
{
  "error": "Invalid invite token",
  "message": "邀请令牌无效或已过期"
}
```

**错误响应** (409 Conflict):
```json
{
  "error": "Already a member",
  "message": "你已经是该清单的成员"
}
```

**业务逻辑**:
1. 验证邀请令牌是否存在
2. 查询令牌对应的清单
3. 检查用户是否已是成员
4. 添加用户为 MEMBER 角色
5. 返回清单 token

---

#### API 6: 移除成员

**端点**: `DELETE /api/lists/{token}/members/{userId}`

**功能**: 移除清单成员（仅 OWNER 可调用）

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| token | String | 是 | 清单token |
| userId | Long | 是 | 要移除的用户ID |

**请求头**:
| 头 | 值 | 说明 |
|----|-----|------|
| X-User-Id | 1 | 当前用户ID |

**成功响应** (204 No Content):
```
(无响应体)
```

**错误响应** (403 Forbidden):
```json
{
  "error": "Forbidden",
  "message": "只有清单所有者可以移除成员"
}
```

**错误响应** (404 Not Found):
```json
{
  "error": "Member not found",
  "message": "成员不存在"
}
```

---

### 4.2 修改现有 API 端点

#### API 7: 创建清单（扩展逻辑）

**端点**: `POST /api/lists`

**变更**: 创建清单时自动将创建者添加为 OWNER

**请求头**:
| 头 | 值 | 说明 |
|----|-----|------|
| X-User-Id | 1 | 当前用户ID |

**业务逻辑变更**:
1. 创建清单
2. 如果请求头包含 X-User-Id，在 list_member 表中添加记录（role=OWNER）

**向后兼容性**:
- 如果请求头不包含 X-User-Id，清单正常创建，无所有者（兼容 V1/V2-A）

---

#### API 8: 创建/更新 todo（扩展逻辑）

**端点**: `POST /api/lists/{token}/items`, `PATCH /api/items/{id}`

**变更**: 记录创建者和更新者

**请求头**:
| 头 | 值 | 说明 |
|----|-----|------|
| X-User-Id | 1 | 当前用户ID |

**业务逻辑变更**:
- 创建 todo 时，将 X-User-Id 写入 created_by_id
- 更新 todo 时，将 X-User-Id 写入 updated_by_id

**向后兼容性**:
- 如果请求头不包含 X-User-Id，字段为 NULL（兼容 V1/V2-A）

---

#### API 9: 获取 todo 列表（扩展响应）

**端点**: `GET /api/lists/{token}`

**变更**: 响应中包含每个 todo 的创建者用户名

**成功响应** (200 OK):
```json
{
  "id": 1,
  "token": "abc123xyz456",
  "title": "我的清单",
  "createdAt": "2026-02-19T10:00:00",
  "items": [
    {
      "id": 1,
      "title": "买牛奶",
      "completed": false,
      "priority": "HIGH",
      "dueDate": "2026-02-20",
      "createdBy": "张三",  // 新增字段
      "updatedBy": "李四",  // 新增字段
      "createdAt": "2026-02-19T10:01:00",
      "updatedAt": "2026-02-19T10:05:00"
    }
  ]
}
```

**向后兼容性**:
- V1/V2-A 客户端忽略新增字段

---

### 4.3 HTTP 状态码规范

| 状态码 | 场景 |
|--------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 204 | 删除成功 |
| 400 | 参数错误 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 409 | 冲突（如用户已是成员） |
| 500 | 服务器错误 |

---

### 4.4 错误响应格式规范

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
| Username already exists | 400 | 用户名已存在 |
| Forbidden | 403 | 权限不足 |
| Invalid invite token | 404 | 邀请令牌无效 |
| Already a member | 409 | 用户已是成员 |

---

## 5. 非目标确认

### 5.1 明确不做（V2-B 禁止实现）

| 功能 | 说明 | 何时考虑 |
|------|------|----------|
| 实时同步 / WebSocket | 无实时更新，需刷新 | V2-C 或后续 |
| 聊天 / 评论功能 | 不支持 | V3 或后续 |
| 通知推送 | 不支持 | V3 或后续 |
| JWT 认证 | 使用简单的 X-User-Id 头 | V3 或后续 |
| 密码登录 | 无需账号系统 | V3 或后续 |
| 邮箱验证 | 不支持 | V3 或后续 |
| 退出清单 | 成员无法主动退出 | V2-C 或后续 |
| 转移所有权 | 不能将所有者权限转给他人 | V2-C 或后续 |
| 邀请令牌过期 | 令牌永不过期 | V2-C 或后续 |
| 邀请令牌一次性使用 | 令牌可重复使用 | V2-C 或后续 |
| 成员角色编辑 | 不能将 MEMBER 升级为 OWNER | V2-C 或后续 |
| 清单公开/私有设置 | 所有清单默认公开 | V2-C 或后续 |
| 用户个人资料 | 只有用户名，无头像、简介等 | V3 或后续 |
| 用户搜索 | 不支持 | V3 或后续 |
| 微服务架构 | 使用单体架构 | 永不 |

### 5.2 技术约束（V2-B 禁止）

- ❌ 不使用 WebSocket 或 SSE
- ❌ 不使用 Redis 等缓存
- ❌ 不使用消息队列
- ❌ 不使用微服务架构
- ❌ 不实现 JWT 或 OAuth
- ❌ 不实现复杂的权限系统（如 RBAC）
- ❌ 不实现自动化测试（可手动测试）
- ❌ 不实现邮件发送功能
- ❌ 不实现短信发送功能

### 5.3 明确不做（延续 V1/V2-A）

- 不做 QR 码生成
- 不做深色模式
- 不做移动端原生应用
- 不做 SEO 优化
- 不做日志分析
- 不做监控告警
- 不做数据导出
- 不做清单模板
- 不做子任务
- 不做文件附件
- 不做拖拽排序
- 不做操作历史/撤销

---

## 6. 验收标准

### 6.1 功能验收清单

#### 必须完成 (P0)
- [ ] 首次访问自动创建用户
- [ ] 用户名唯一性验证
- [ ] 用户可以修改用户名
- [ ] 清单创建时创建者成为 OWNER
- [ ] OWNER 可以生成邀请令牌
- [ ] 邀请令牌是 12 位随机字符串
- [ ] 用户可以通过邀请令牌加入清单
- [ ] 加入后用户成为 MEMBER
- [ ] 可以查询清单的所有成员
- [ ] 成员列表显示用户名和角色
- [ ] OWNER 可以移除成员
- [ ] MEMBER 不能生成邀请令牌
- [ ] MEMBER 不能移除成员
- [ ] todo 显示创建者用户名
- [ ] 数据持久化正常工作
- [ ] V1/V2-A API 继续可用（向后兼容）

#### 应该完成 (P1)
- [ ] 前端自动创建用户（首次访问）
- [ ] userId 存储在 localStorage
- [ ] 请求自动携带 X-User-Id 头
- [ ] 成员列表 UI 美观
- [ ] 邀请链接复制功能
- [ ] 错误提示友好

### 6.2 API 验收清单

#### 必须完成 (P0)
- [ ] POST /api/users 正常工作
- [ ] PATCH /api/users/{id} 正常工作
- [ ] GET /api/lists/{token}/members 正常工作
- [ ] POST /api/lists/{token}/invites 正常工作
- [ ] POST /api/lists/join 正常工作
- [ ] DELETE /api/lists/{token}/members/{userId} 正常工作
- [ ] 权限验证正确（OWNER vs MEMBER）
- [ ] 所有 API 保持向后兼容

#### 应该完成 (P1)
- [ ] 用户名唯一性验证
- [ ] 邀请令牌唯一性验证
- [ ] 防止重复加入同一清单
- [ ] 错误响应符合统一格式
- [ ] HTTP 状态码使用正确

### 6.3 数据库验收清单

#### 必须完成 (P0)
- [ ] user 表创建成功
- [ ] list_member 表创建成功
- [ ] invite_token 表创建成功
- [ ] todo_item 表成功添加 created_by_id 和 updated_by_id
- [ ] 外键约束正常工作
- [ ] 唯一约束正常工作
- [ ] 级联删除正常工作

#### 应该完成 (P1)
- [ ] 索引优化
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

### 场景 1: 用户创建和修改

1. 清除浏览器 localStorage
2. 访问应用首页
3. 验证: 自动创建用户，用户名格式为"用户_xxxxxx"
4. 修改用户名为"张三"
5. 验证: 用户名更新成功
6. 刷新页面
7. 验证: 用户名保留，userId 不变

### 场景 2: 邀请成员加入清单

1. 用户A创建清单
2. 验证: 用户A自动成为 OWNER
3. 用户A点击"邀请成员"
4. 验证: 生成邀请令牌（12位）
5. 用户A复制邀请链接发送给用户B
6. 用户B访问邀请链接
7. 验证: 用户B自动加入清单，角色为 MEMBER
8. 用户B尝试生成邀请令牌
9. 验证: 返回 403 错误

### 场景 3: 成员列表查看

1. 清单有 3 个成员（1 OWNER + 2 MEMBER）
2. 访问清单详情页
3. 验证: 显示成员列表
4. 验证: 显示每个成员的用户名和角色
5. 验证: OWNER 可以看到"移除"按钮
6. 验证: MEMBER 看不到"移除"按钮

### 场景 4: 移除成员

1. 用户A（OWNER）尝试移除用户B（MEMBER）
2. 验证: 移除成功
3. 用户B尝试访问清单
4. 验证: 仍可访问（V2-B 不限制公开访问）
5. 用户B尝试生成邀请令牌
6. 验证: 返回 403 错误
7. 用户B（MEMBER）尝试移除用户C（MEMBER）
8. 验证: 返回 403 错误

### 场景 5: Todo 创建者追踪

1. 用户A创建 todo
2. 验证: todo 显示"创建者: 用户A"
3. 用户B编辑 todo
4. 验证: todo 显示"更新者: 用户B"
5. 刷新页面
6. 验证: 创建者和更新者信息保留

### 场景 6: 重复加入清单

1. 用户A已是清单成员
2. 用户A再次使用邀请令牌尝试加入
3. 验证: 返回 409 错误（"你已经是该清单的成员"）

### 场景 7: 无效邀请令牌

1. 用户A使用不存在的邀请令牌
2. 验证: 返回 404 错误（"邀请令牌无效"）

### 场景 8: 向后兼容性

1. 不携带 X-User-Id 头创建清单
2. 验证: 清单创建成功，无所有者
3. 不携带 X-User-Id 头创建 todo
4. 验证: todo 创建成功，created_by_id 为 NULL
5. V1 客户端访问清单
6. 验证: 正常工作，忽略新增字段

---

## 8. 实现建议

### 8.1 实体类设计

**User 实体**：
```java
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 构造方法
    public User() {}

    public User(String username) {
        this.username = username;
    }

    // Getter 和 Setter...
}
```

**ListMember 实体**：
```java
@Entity
@Table(name = "list_member")
public class ListMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    private TodoList list;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private MemberRole role = MemberRole.MEMBER;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    // 构造方法
    public ListMember() {}

    public ListMember(TodoList list, User user, MemberRole role) {
        this.list = list;
        this.user = user;
        this.role = role;
    }

    // Getter 和 Setter...
}

// 成员角色枚举
public enum MemberRole {
    OWNER,
    MEMBER
}
```

**InviteToken 实体**：
```java
@Entity
@Table(name = "invite_token")
public class InviteToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    private TodoList list;

    @Column(unique = true, nullable = false, length = 12)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 构造方法
    public InviteToken() {}

    public InviteToken(TodoList list, String token, User createdBy) {
        this.list = list;
        this.token = token;
        this.createdBy = createdBy;
    }

    // Getter 和 Setter...
}
```

**TodoItem 实体（扩展）**：
```java
@Entity
@Table(name = "todo_item")
public class TodoItem {
    // ... 现有字段 ...

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private User updatedBy;

    // ... 现有字段 ...
}
```

### 8.2 Service 层设计

**UserService（新增）**：
```java
@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User createAnonymousUser() {
        User user = new User();
        user.setUsername(generateUsername());
        return userRepository.save(user);
    }

    public User createUser(String username) {
        // 验证用户名唯一性
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User(username);
        return userRepository.save(user);
    }

    public User updateUser(Long userId, String newUsername) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("用户不存在"));

        if (userRepository.existsByUsernameAndIdNot(newUsername, userId)) {
            throw new IllegalArgumentException("用户名已存在");
        }

        user.setUsername(newUsername);
        return userRepository.save(user);
    }

    private String generateUsername() {
        String random = RandomStringUtils.randomAlphanumeric(6).toLowerCase();
        return "用户_" + random;
    }
}
```

**MemberService（新增）**：
```java
@Service
@Transactional
public class MemberService {

    @Autowired
    private ListMemberRepository memberRepository;

    @Autowired
    private TodoListRepository listRepository;

    @Autowired
    private UserRepository userRepository;

    public ListMember addMember(String listToken, Long userId, MemberRole role) {
        TodoList list = listRepository.findByToken(listToken)
            .orElseThrow(() -> new NotFoundException("清单不存在"));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("用户不存在"));

        // 检查是否已是成员
        if (memberRepository.findByListAndUser(list, user).isPresent()) {
            throw new IllegalStateException("用户已是成员");
        }

        ListMember member = new ListMember(list, user, role);
        return memberRepository.save(member);
    }

    public void removeMember(String listToken, Long memberUserId, Long operatorUserId) {
        TodoList list = listRepository.findByToken(listToken)
            .orElseThrow(() -> new NotFoundException("清单不存在"));

        // 检查操作者是否是 OWNER
        User operator = userRepository.findById(operatorUserId)
            .orElseThrow(() -> new NotFoundException("操作者不存在"));

        if (!isOwner(list, operator)) {
            throw new ForbiddenException("只有清单所有者可以移除成员");
        }

        User memberUser = userRepository.findById(memberUserId)
            .orElseThrow(() -> new NotFoundException("成员不存在"));

        ListMember member = memberRepository.findByListAndUser(list, memberUser)
            .orElseThrow(() -> new NotFoundException("成员关系不存在"));

        memberRepository.delete(member);
    }

    public List<MemberResponse> getMembers(String listToken) {
        TodoList list = listRepository.findByToken(listToken)
            .orElseThrow(() -> new NotFoundException("清单不存在"));

        return memberRepository.findByList(list).stream()
            .map(MemberResponse::new)
            .collect(Collectors.toList());
    }

    private boolean isOwner(TodoList list, User user) {
        return memberRepository.findByListAndUser(list, user)
            .map(m -> m.getRole() == MemberRole.OWNER)
            .orElse(false);
    }
}
```

**InviteService（新增）**：
```java
@Service
@Transactional
public class InviteService {

    @Autowired
    private InviteTokenRepository inviteTokenRepository;

    @Autowired
    private TodoListRepository listRepository;

    @Autowired
    private MemberService memberService;

    public InviteToken createInvite(String listToken, Long creatorId) {
        TodoList list = listRepository.findByToken(listToken)
            .orElseThrow(() -> new NotFoundException("清单不存在"));

        String token = generateInviteToken();
        InviteToken invite = new InviteToken(list, token, /* creator */);
        return inviteTokenRepository.save(invite);
    }

    @Transactional
    public TodoList joinList(String inviteToken, Long userId) {
        InviteToken invite = inviteTokenRepository.findByToken(inviteToken)
            .orElseThrow(() -> new NotFoundException("邀请令牌无效"));

        TodoList list = invite.getList();
        memberService.addMember(list.getToken(), userId, MemberRole.MEMBER);
        return list;
    }

    private String generateInviteToken() {
        String token;
        do {
            token = RandomStringUtils.randomAlphanumeric(12).toLowerCase();
        } while (inviteTokenRepository.existsByToken(token));
        return token;
    }
}
```

### 8.3 Controller 层设计

**UserController（新增）**：
```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
        @RequestBody(required = false) CreateUserRequest request
    ) {
        User user;
        if (request != null && request.getUsername() != null) {
            user = userService.createUser(request.getUsername());
        } else {
            user = userService.createAnonymousUser();
        }
        return ResponseEntity.status(201).body(new UserResponse(user));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
        @PathVariable Long id,
        @RequestBody @Valid UpdateUserRequest request
    ) {
        User user = userService.updateUser(id, request.getUsername());
        return ResponseEntity.ok(new UserResponse(user));
    }
}
```

**MemberController（新增）**：
```java
@RestController
@RequestMapping("/api/lists")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @GetMapping("/{token}/members")
    public ResponseEntity<List<MemberResponse>> getMembers(@PathVariable String token) {
        List<MemberResponse> members = memberService.getMembers(token);
        return ResponseEntity.ok(members);
    }

    @DeleteMapping("/{token}/members/{userId}")
    public ResponseEntity<Void> removeMember(
        @PathVariable String token,
        @PathVariable Long userId,
        @RequestHeader("X-User-Id") Long operatorId
    ) {
        memberService.removeMember(token, userId, operatorId);
        return ResponseEntity.noContent().build();
    }
}
```

**InviteController（新增）**：
```java
@RestController
@RequestMapping("/api/lists")
public class InviteController {

    @Autowired
    private InviteService inviteService;

    @PostMapping("/{token}/invites")
    public ResponseEntity<InviteResponse> createInvite(
        @PathVariable String token,
        @RequestHeader("X-User-Id") Long creatorId
    ) {
        InviteToken invite = inviteService.createInvite(token, creatorId);
        String inviteUrl = "http://localhost:8080/join?invite=" + invite.getToken();
        return ResponseEntity.status(201).body(new InviteResponse(invite, inviteUrl));
    }

    @PostMapping("/join")
    public ResponseEntity<JoinResponse> joinList(
        @RequestBody @Valid JoinListRequest request,
        @RequestHeader("X-User-Id") Long userId
    ) {
        TodoList list = inviteService.joinList(request.getInviteToken(), userId);
        return ResponseEntity.ok(new JoinResponse(list.getToken(), "MEMBER"));
    }
}
```

### 8.4 前端实现建议

**自动创建用户**：
```javascript
// 首次访问时自动创建用户
async function ensureUser() {
  let userId = localStorage.getItem('userId');
  if (!userId) {
    const response = await fetch('/api/users', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    });
    const user = await response.json();
    userId = user.id;
    localStorage.setItem('userId', userId);
  }
  return userId;
}

// 请求拦截器，自动添加 X-User-Id 头
async function fetchWithUser(url, options = {}) {
  const userId = localStorage.getItem('userId');
  options.headers = options.headers || {};
  options.headers['X-User-Id'] = userId;
  return fetch(url, options);
}
```

**生成邀请链接**：
```javascript
async function generateInvite(listToken) {
  const response = await fetchWithUser(`/api/lists/${listToken}/invites`, {
    method: 'POST'
  });

  if (response.ok) {
    const data = await response.json();
    // 显示邀请链接
    document.getElementById('invite-url').textContent = data.inviteUrl;
    // 复制到剪贴板
    navigator.clipboard.writeText(data.inviteUrl);
  } else {
    alert('生成邀请链接失败');
  }
}
```

**加入清单**：
```javascript
// 从 URL 参数获取邀请令牌
const urlParams = new URLSearchParams(window.location.search);
const inviteToken = urlParams.get('invite');

if (inviteToken) {
  async function joinList() {
    const response = await fetchWithUser('/api/lists/join', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ inviteToken })
    });

    if (response.ok) {
      const data = await response.json();
      // 重定向到清单详情页
      window.location.href = `/lists/${data.listToken}`;
    } else {
      const error = await response.json();
      alert(error.message);
    }
  }

  joinList();
}
```

**渲染成员列表**：
```javascript
async function loadMembers(listToken) {
  const response = await fetch(`/api/lists/${listToken}/members`);
  const members = await response.json();

  const membersHtml = members.map(member => {
    const roleLabel = member.role === 'OWNER' ? '所有者' : '成员';
    const removeBtn = member.role === 'OWNER' && canRemove
      ? `<button onclick="removeMember(${member.userId})">移除</button>`
      : '';

    return `
      <div class="member-item">
        <span>👤 ${member.username} (${roleLabel})</span>
        ${removeBtn}
      </div>
    `;
  }).join('');

  document.getElementById('members-list').innerHTML = membersHtml;
}
```

---

## 9. 开发计划

### 9.1 开发顺序

**阶段 1: 数据库和实体 (1 天)**
1. 创建 User、ListMember、InviteToken 实体
2. 创建 MemberRole 枚举
3. 更新 TodoItem 实体（添加 created_by、updated_by）
4. 执行数据库迁移
5. 验证数据库变更

**阶段 2: Service 层 (1.5 天)**
1. 实现 UserService
2. 实现 MemberService
3. 实现 InviteService
4. 添加权限验证逻辑
5. 编写单元测试（可选）

**阶段 3: Controller 层 (1 天)**
1. 实现 UserController
2. 实现 MemberController
3. 实现 InviteController
4. 更新现有 Controller（添加 X-User-Id 处理）
5. 测试所有 API

**阶段 4: 前端实现 (2 天)**
1. 实现自动创建用户
2. 实现 userId 持久化
3. 实现请求拦截器
4. 实现邀请链接生成
5. 实现加入清单流程
6. 实现成员列表展示
7. 实现移除成员功能

**阶段 5: 联调与测试 (1 天)**
1. 端到端测试所有新功能
2. 测试权限控制
3. 测试错误场景
4. 验证向后兼容性
5. 代码优化

**总工期**: 约 6.5 天

### 9.2 技术栈版本

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | LTS 版本 |
| Spring Boot | 3.x | 最新稳定版 |
| Spring Data JPA | 3.x | 随 Spring Boot |
| H2 Database | 2.x | 开发环境 |
| Thymeleaf | 3.x | 随 Spring Boot |
| Apache Commons Lang | 3.x | 用于随机字符串生成 |

---

## 10. Scope Freeze 声明

本文档定义的所有功能和 API 设计自发布之日起冻结，后续开发过程中不得接受变更请求：

**冻结内容**：
- 功能范围：用户模型、清单成员关系、邀请令牌机制、角色权限控制、成员列表查看
- 数据库设计：user、list_member、invite_token 表，todo_item 扩展字段
- API 设计：所有新增和修改的端点
- 验收标准：所有 P0 和 P1 标准

**非目标明确禁止**：
- 实时同步 / WebSocket
- 聊天 / 评论功能
- 通知推送
- JWT 认证
- 密码登录
- 邮箱验证
- 退出清单
- 转移所有权
- 邀请令牌过期
- 邀请令牌一次性使用
- 成员角色编辑
- 清单公开/私有设置
- 用户个人资料
- 用户搜索
- 微服务架构

**变更流程**：
如有紧急变更需求，需经以下流程：
1. PM 提出变更申请
2. TechLead 评估技术影响
3. 双方共同评审
4. 发布新版本文档（PRD_V2B_v2.0.md）

---

## 11. 附录

### 11.1 术语表

| 术语 | 说明 |
|------|------|
| User | 用户，包含用户名 |
| ListMember | 清单成员关系，连接用户和清单 |
| MemberRole | 成员角色（OWNER/MEMBER） |
| InviteToken | 邀请令牌，用于加入清单 |
| OWNER | 所有者角色，可以管理成员 |
| MEMBER | 成员角色，可以编辑 todos |
| X-User-Id | HTTP 请求头，用于标识当前用户 |

### 11.2 参考资源

- [V1 PRD](/d/develop/project/todolist/PRD.md)
- [V2-A PRD](/d/develop/project/todolist/PRD_V2A.md)
- [V1 API Contract](/d/develop/project/todolist/API_CONTRACT.md)
- [V1 Tech Design](/d/develop/project/todolist/TECH_DESIGN.md)
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Spring Data JPA 文档](https://spring.io/projects/spring-data-jpa)

### 11.3 联系方式

- **产品负责人**: PM
- **技术负责人**: TechLead
- **项目类型**: 协作基础版本
- **预期工期**: 6.5 天

---

**文档状态**: Scope Freeze
**产品负责人**: PM
**发布日期**: 2026-02-19
**文档版本**: 2.0-B

---

**文档结束**
