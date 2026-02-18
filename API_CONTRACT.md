# V1 API 契约文档

## 项目概述

**项目名称**: 共享待办清单 (Shared Todo List)
**版本**: V1
**技术栈**: Spring Boot + Thymeleaf
**Base URL**: `http://localhost:8080`

## API 端点总览

| 功能 | HTTP 方法 | 端点 | 描述 |
|------|-----------|------|------|
| 创建待办清单 | POST | `/api/lists` | 创建新的待办清单 |
| 获取待办清单 | GET | `/api/lists/{token}` | 根据 token 获取待办清单详情 |
| 添加待办事项 | POST | `/api/lists/{token}/items` | 向指定清单添加待办事项 |
| 获取待办事项列表 | GET | `/api/lists/{token}/items` | 获取指定清单的所有待办事项 |
| 更新待办事项 | PATCH | `/api/items/{id}` | 更新待办事项的标题或完成状态 |
| 删除待办事项 | DELETE | `/api/items/{id}` | 删除指定的待办事项 |

---

## HTTP 状态码说明

| 状态码 | 说明 | 使用场景 |
|--------|------|----------|
| 200 OK | 请求成功 | GET 请求成功返回数据 |
| 201 Created | 资源创建成功 | POST 请求成功创建资源 |
| 204 No Content | 删除成功 | DELETE 请求成功删除资源 |
| 400 Bad Request | 请求参数错误 | 请求体格式错误、参数验证失败 |
| 404 Not Found | 资源不存在 | Token 或 ID 不存在 |
| 500 Internal Server Error | 服务器内部错误 | 系统异常、数据库异常 |

---

## API 详细说明

### 1. 创建待办清单

创建一个新的待办清单，系统自动生成唯一 token。

**端点**: `POST /api/lists`

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
  "createdAt": "2025-02-18T10:30:00",
  "items": []
}
```

**错误响应** (500 Internal Server Error):
```json
{
  "error": "Failed to generate unique token",
  "message": "系统繁忙,请稍后重试"
}
```

**字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 待办清单的唯一标识符 |
| token | String | 待办清单的唯一访问令牌（用于分享） |
| createdAt | String | 创建时间，格式：yyyy-MM-dd'T'HH:mm:ss |
| items | Array | 待办事项列表（初始为空） |

---

### 2. 获取待办清单

根据 token 获取待办清单的完整信息，包括所有待办事项。

**端点**: `GET /api/lists/{token}`

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
  "createdAt": "2025-02-18T10:30:00",
  "items": [
    {
      "id": 1,
      "title": "学习 Spring Boot",
      "completed": false,
      "createdAt": "2025-02-18T10:31:00",
      "updatedAt": "2025-02-18T10:31:00"
    },
    {
      "id": 2,
      "title": "完成 API 文档",
      "completed": true,
      "createdAt": "2025-02-18T10:35:00",
      "updatedAt": "2025-02-18T11:00:00"
    }
  ]
}
```

**错误响应** (404 Not Found):
```json
{
  "error": "Resource not found",
  "message": "List not found with token: abc123xyz456"
}
```

**字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 待办清单的唯一标识符 |
| token | String | 待办清单的唯一访问令牌 |
| createdAt | String | 创建时间，格式：yyyy-MM-dd'T'HH:mm:ss |
| items | Array | 待办事项列表，见下方 ItemResponse 字段说明 |

---

### 3. 添加待办事项

向指定 token 的待办清单中添加新的待办事项。

**端点**: `POST /api/lists/{token}/items`

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| token | String | 是 | 待办清单的唯一访问令牌 |

**请求示例**:
```http
POST /api/lists/abc123xyz456/items HTTP/1.1
Content-Type: application/json

{
  "title": "学习 Spring Boot"
}
```

**请求体** (CreateItemRequest):
| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| title | String | 是 | 非空 | 待办事项的标题 |

**成功响应** (201 Created):
```json
{
  "id": 1,
  "title": "学习 Spring Boot",
  "completed": false,
  "createdAt": "2025-02-18T10:31:00",
  "updatedAt": "2025-02-18T10:31:00"
}
```

**错误响应** (400 Bad Request):
```json
{
  "error": "Invalid request",
  "message": "Title cannot be empty"
}
```

**错误响应** (404 Not Found):
```json
{
  "error": "Resource not found",
  "message": "List not found with token: abc123xyz456"
}
```

**字段说明** (ItemResponse):
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 待办事项的唯一标识符 |
| title | String | 待办事项的标题 |
| completed | Boolean | 是否已完成（true=已完成，false=未完成） |
| createdAt | String | 创建时间，格式：yyyy-MM-dd'T'HH:mm:ss |
| updatedAt | String | 更新时间，格式：yyyy-MM-dd'T'HH:mm:ss |

---

### 4. 获取待办事项列表

获取指定 token 的待办清单中的所有待办事项。

**端点**: `GET /api/lists/{token}/items`

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
    "title": "学习 Spring Boot",
    "completed": false,
    "createdAt": "2025-02-18T10:31:00",
    "updatedAt": "2025-02-18T10:31:00"
  },
  {
    "id": 2,
    "title": "完成 API 文档",
    "completed": true,
    "createdAt": "2025-02-18T10:35:00",
    "updatedAt": "2025-02-18T11:00:00"
  }
]
```

**错误响应** (404 Not Found):
```json
{
  "error": "Resource not found",
  "message": "List not found with token: abc123xyz456"
}
```

**字段说明**: 见上方 ItemResponse 字段说明

---

### 5. 更新待办事项

更新指定 ID 的待办事项，可更新标题或完成状态。

**端点**: `PATCH /api/items/{id}`

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 待办事项的唯一标识符 |

**请求示例**:
```http
PATCH /api/items/1 HTTP/1.1
Content-Type: application/json

{
  "title": "深入学习 Spring Boot",
  "completed": true
}
```

**请求体** (UpdateItemRequest):
| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| title | String | 否 | 非空（如果提供） | 新的待办事项标题 |
| completed | Boolean | 否 | - | 完成状态（true=已完成，false=未完成） |

**注意**: title 和 completed 至少需要提供一个，也可以同时提供。

**成功响应** (200 OK):
```json
{
  "id": 1,
  "title": "深入学习 Spring Boot",
  "completed": true,
  "createdAt": "2025-02-18T10:31:00",
  "updatedAt": "2025-02-18T11:30:00"
}
```

**错误响应** (400 Bad Request):
```json
{
  "error": "Invalid request",
  "message": "Title cannot be empty"
}
```

**错误响应** (404 Not Found):
```json
{
  "error": "Resource not found",
  "message": "Item not found with id: 1"
}
```

**字段说明**: 见上方 ItemResponse 字段说明

---

### 6. 删除待办事项

删除指定 ID 的待办事项。

**端点**: `DELETE /api/items/{id}`

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 待办事项的唯一标识符 |

**请求示例**:
```http
DELETE /api/items/1 HTTP/1.1
```

**成功响应** (204 No Content):
```
(无响应体)
```

**错误响应** (404 Not Found):
```json
{
  "error": "Resource not found",
  "message": "Item not found with id: 1"
}
```

---

## 数据模型

### ListResponse

待办清单响应模型。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 待办清单的唯一标识符 |
| token | String | 待办清单的唯一访问令牌（用于分享） |
| createdAt | String | 创建时间，格式：yyyy-MM-dd'T'HH:mm:ss |
| items | Array\<ItemResponse\> | 待办事项列表 |

### ItemResponse

待办事项响应模型。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 待办事项的唯一标识符 |
| title | String | 待办事项的标题 |
| completed | Boolean | 是否已完成（true=已完成，false=未完成） |
| createdAt | String | 创建时间，格式：yyyy-MM-dd'T'HH:mm:ss |
| updatedAt | String | 更新时间，格式：yyyy-MM-dd'T'HH:mm:ss |

### CreateItemRequest

创建待办事项请求模型。

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| title | String | 是 | 非空 | 待办事项的标题 |

### UpdateItemRequest

更新待办事项请求模型。

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| title | String | 否 | 非空（如果提供） | 新的待办事项标题 |
| completed | Boolean | 否 | - | 完成状态（true=已完成，false=未完成） |

### ErrorResponse

错误响应模型。

| 字段 | 类型 | 说明 |
|------|------|------|
| error | String | 错误类型简述 |
| message | String | 详细错误信息 |

---

## 通用错误处理

### 400 Bad Request

请求参数错误或请求体格式错误。

**示例响应**:
```json
{
  "error": "Invalid request",
  "message": "请求体格式错误: ..."
}
```

或

```json
{
  "error": "Invalid request",
  "message": "Title cannot be empty"
}
```

### 404 Not Found

请求的资源不存在。

**示例响应**:
```json
{
  "error": "Resource not found",
  "message": "List not found with token: abc123xyz456"
}
```

或

```json
{
  "error": "Resource not found",
  "message": "Item not found with id: 1"
}
```

### 500 Internal Server Error

服务器内部错误，通常是由于系统异常或数据库异常导致。

**示例响应**:
```json
{
  "error": "Failed to generate unique token",
  "message": "系统繁忙,请稍后重试"
}
```

或

```json
{
  "error": "Internal server error",
  "message": "系统繁忙,请稍后重试"
}
```

---

## 使用示例

### 完整流程示例

#### 1. 创建待办清单
```bash
curl -X POST http://localhost:8080/api/lists \
  -H "Content-Type: application/json"
```

**响应**:
```json
{
  "id": 1,
  "token": "abc123xyz456",
  "createdAt": "2025-02-18T10:30:00",
  "items": []
}
```

#### 2. 添加待办事项
```bash
curl -X POST http://localhost:8080/api/lists/abc123xyz456/items \
  -H "Content-Type: application/json" \
  -d '{"title": "学习 Spring Boot"}'
```

**响应**:
```json
{
  "id": 1,
  "title": "学习 Spring Boot",
  "completed": false,
  "createdAt": "2025-02-18T10:31:00",
  "updatedAt": "2025-02-18T10:31:00"
}
```

#### 3. 更新待办事项
```bash
curl -X PATCH http://localhost:8080/api/items/1 \
  -H "Content-Type: application/json" \
  -d '{"completed": true}'
```

**响应**:
```json
{
  "id": 1,
  "title": "学习 Spring Boot",
  "completed": true,
  "createdAt": "2025-02-18T10:31:00",
  "updatedAt": "2025-02-18T11:30:00"
}
```

#### 4. 获取待办清单
```bash
curl -X GET http://localhost:8080/api/lists/abc123xyz456
```

**响应**:
```json
{
  "id": 1,
  "token": "abc123xyz456",
  "createdAt": "2025-02-18T10:30:00",
  "items": [
    {
      "id": 1,
      "title": "学习 Spring Boot",
      "completed": true,
      "createdAt": "2025-02-18T10:31:00",
      "updatedAt": "2025-02-18T11:30:00"
    }
  ]
}
```

#### 5. 删除待办事项
```bash
curl -X DELETE http://localhost:8080/api/items/1
```

**响应**: 204 No Content（无响应体）

---

## 注意事项

1. **Token 安全性**: Token 是访问待办清单的唯一凭证，请妥善保管。
2. **日期时间格式**: 所有日期时间字段使用 ISO 8601 格式（yyyy-MM-dd'T'HH:mm:ss）。
3. **部分更新**: 更新待办事项时，可以只提供 title 或 completed 中的一个，也可以同时提供。
4. **错误处理**: 所有错误响应都遵循统一的 ErrorResponse 格式。
5. **幂等性**: 删除操作具有幂等性，多次删除同一资源返回相同结果。

---

## 附录

### 相关文件

- **Controller 层**:
  - `/src/main/java/com/example/todolist/controller/ListController.java`
  - `/src/main/java/com/example/todolist/controller/ItemController.java`
  - `/src/main/java/com/example/todolist/controller/ItemManagementController.java`

- **DTO 层**:
  - `/src/main/java/com/example/todolist/dto/ListResponse.java`
  - `/src/main/java/com/example/todolist/dto/ItemResponse.java`
  - `/src/main/java/com/example/todolist/dto/CreateItemRequest.java`
  - `/src/main/java/com/example/todolist/dto/UpdateItemRequest.java`
  - `/src/main/java/com/example/todolist/dto/ErrorResponse.java`

- **异常处理**:
  - `/src/main/java/com/example/todolist/exception/GlobalExceptionHandler.java`
  - `/src/main/java/com/example/todolist/exception/NotFoundException.java`

---

**文档版本**: V1.0
**最后更新**: 2025-02-18
**维护者**: TechLead Team
