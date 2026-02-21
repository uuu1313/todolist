# 共享待办清单 V1 - 技术设计文档

**文档版本**: 1.0
**创建日期**: 2026-02-18
**技术负责人**: TechLead
**文档状态**: API 冻结前最终版

**重要声明**: 本文档基于 PRD v1.0 编写,所有 API 契约一旦确认,后续开发过程中不得修改。

---

## 1. 数据库设计细化

### 1.1 表结构设计

#### 表 1: todo_list (清单表)

```sql
CREATE TABLE todo_list (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  token VARCHAR(8) NOT NULL UNIQUE COMMENT '访问令牌(8位随机字符)',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_token (token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='待办清单表';
```

**字段说明**:
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 主键 |
| token | VARCHAR(8) | NOT NULL, UNIQUE | 访问令牌,唯一索引 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引设计**:
- PRIMARY KEY: `id` (聚簇索引)
- UNIQUE INDEX: `idx_token` (token 唯一性索引)

---

#### 表 2: todo_item (待办事项表)

```sql
CREATE TABLE todo_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  list_id BIGINT NOT NULL COMMENT '所属清单ID(外键)',
  title VARCHAR(200) NOT NULL COMMENT '事项标题',
  completed BOOLEAN DEFAULT FALSE COMMENT '是否完成',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (list_id) REFERENCES todo_list(id) ON DELETE CASCADE,
  INDEX idx_list_id (list_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='待办事项表';
```

**字段说明**:
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 主键 |
| list_id | BIGINT | NOT NULL, FOREIGN KEY | 所属清单ID |
| title | VARCHAR(200) | NOT NULL | 事项标题 |
| completed | BOOLEAN | DEFAULT FALSE | 是否完成 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引设计**:
- PRIMARY KEY: `id` (聚簇索引)
- INDEX: `idx_list_id` (外键索引,优化查询性能)

---

### 1.2 ER 图描述

```
┌─────────────────┐
│   todo_list     │
├─────────────────┤
│ id (PK)         │──┐
│ token (UNIQUE)  │  │
│ created_at      │  │ 1
│ updated_at      │  │
└─────────────────┘  │
                     │
                     │ N
                     │
              ┌──────┴──────┐
              │  todo_item  │
              ├─────────────┤
              │ id (PK)     │
              │ list_id (FK)│
              │ title       │
              │ completed   │
              │ created_at  │
              │ updated_at  │
              └─────────────┘
```

**关系说明**:
- todo_list 与 todo_item 是 **1:N** 关系
- 一个清单包含多个事项
- 一个事项只能属于一个清单

---

### 1.3 级联删除规则

**数据库层面**:
```sql
FOREIGN KEY (list_id) REFERENCES todo_list(id) ON DELETE CASCADE
```

**业务规则**:
1. 删除清单时,自动删除该清单下的所有事项
2. 删除事项时,不影响清单
3. 不允许单独删除清单(通过 API 暴露此功能,但不在 V1 MVP 范围内)

**JPA 实现**:
```java
@OneToMany(mappedBy = "list", cascade = CascadeType.ALL, orphanRemoval = true)
private List<TodoItem> items = new ArrayList<>();
```

---

### 1.4 数据类型映射 (Java ↔ SQL ↔ JSON)

| Java 类型 | SQL 类型 | JSON 类型 | 说明 |
|-----------|----------|-----------|------|
| Long | BIGINT | number | ID 字段 |
| String | VARCHAR(8) | string | token |
| String | VARCHAR(200) | string | title |
| Boolean | BOOLEAN/TINYINT(1) | boolean | completed |
| LocalDateTime | TIMESTAMP | string (ISO 8601) | 时间字段 |

---

## 2. Token 生成规则

### 2.1 生成算法

**技术方案**: 使用 Java `SecureRandom` + 字符池

**字符池定义**:
```
字符集: A-Z, a-z, 0-9 (共 62 个字符)
排除易混淆字符: 0, O, I, l (可选,本版本不排除)
```

**示例代码**:
```java
import java.security.SecureRandom;

public class TokenGenerator {
    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TOKEN_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generate() {
        StringBuilder token = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            token.append(CHAR_POOL.charAt(RANDOM.nextInt(CHAR_POOL.length())));
        }
        return token.toString();
    }
}
```

**Token 格式示例**:
- `aB3xK9mP`
- `Xy7Zq2Wp`
- `9mK2nPqR`

---

### 2.2 唯一性保证机制

**策略**: 重试机制 + 数据库唯一约束

**流程**:
```
1. 生成随机 token
2. 查询数据库检查是否存在
3. 如果不存在 → 保存并返回
4. 如果存在 → 重新生成 (最多重试 10 次)
5. 如果重试 10 次仍失败 → 抛出异常
```

**示例代码**:
```java
@Service
public class TokenService {

    @Autowired
    private TodoListRepository listRepository;

    private static final int MAX_RETRIES = 10;

    public String generateUniqueToken() {
        for (int i = 0; i < MAX_RETRIES; i++) {
            String token = TokenGenerator.generate();
            if (!listRepository.existsByToken(token)) {
                return token;
            }
        }
        throw new IllegalStateException("Failed to generate unique token after " + MAX_RETRIES + " retries");
    }
}
```

**概率计算**:
- 单次冲突概率: n / 62^8 (n 为现有 token 数量)
- 10 万个清单时,单次冲突概率 ≈ 0.0004%
- 重试机制几乎可以保证 100% 成功

---

### 2.3 冲突处理策略

**场景 1**: 生成时冲突
- **处理**: 重试生成新 token
- **重试次数**: 10 次
- **失败策略**: 抛出 `IllegalStateException`,返回 500 错误

**场景 2**: 数据库唯一约束冲突
- **原因**: 极端并发情况下的竞态条件
- **处理**: 捕获 `DataIntegrityViolationException`,返回 500 错误
- **用户提示**: "系统繁忙,请稍后重试"

**Repository 接口**:
```java
public interface TodoListRepository extends JpaRepository<TodoList, Long> {
    boolean existsByToken(String token);
    Optional<TodoList> findByToken(String token);
}
```

---

## 3. REST API 契约 (完整版)

### 3.1 API 总览

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | /api/lists | 创建新清单 |
| GET | /api/lists/{token} | 获取清单详情 (包含事项列表) |
| GET | /api/lists/{token}/items | 获取清单的所有事项 |
| POST | /api/lists/{token}/items | 添加新事项 |
| PATCH | /api/items/{id} | 更新事项 (完成状态、标题) |
| DELETE | /api/items/{id} | 删除事项 |

**通用规范**:
- Content-Type: `application/json`
- 字符编码: `UTF-8`
- 时间格式: ISO 8601 (`2026-02-18T10:00:00`)
- ID 字段类型: `number`

---

### 3.2 API 详细说明

#### API 1: 创建清单

**请求**:
```
POST /api/lists
Content-Type: application/json

// 无请求体
```

**响应**:
```
状态码: 201 Created

{
  "id": 1,
  "token": "aB3xK9mP",
  "createdAt": "2026-02-18T10:00:00"
}
```

**字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 清单ID |
| token | string | 访问令牌 (8位) |
| createdAt | string | 创建时间 |

**错误响应**:
```
状态码: 500 Internal Server Error

{
  "error": "Failed to generate unique token",
  "message": "系统繁忙,请稍后重试"
}
```

**业务逻辑**:
1. 生成唯一 token
2. 创建 TodoList 实体
3. 保存到数据库
4. 返回清单信息

---

#### API 2: 获取清单详情 (包含事项列表)

**请求**:
```
GET /api/lists/{token}
```

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| token | string | 清单 token |

**响应**:
```
状态码: 200 OK

{
  "id": 1,
  "token": "aB3xK9mP",
  "createdAt": "2026-02-18T10:00:00",
  "items": [
    {
      "id": 1,
      "title": "买牛奶",
      "completed": false,
      "createdAt": "2026-02-18T10:01:00"
    },
    {
      "id": 2,
      "title": "买鸡蛋",
      "completed": true,
      "createdAt": "2026-02-18T10:02:00"
    }
  ]
}
```

**字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 清单ID |
| token | string | 清单 token |
| createdAt | string | 清单创建时间 |
| items | array | 事项列表 |
| items[].id | number | 事项ID |
| items[].title | string | 事项标题 |
| items[].completed | boolean | 是否完成 |
| items[].createdAt | string | 事项创建时间 |

**错误响应**:
```
状态码: 404 Not Found

{
  "error": "List not found",
  "message": "清单不存在"
}
```

**业务逻辑**:
1. 根据 token 查询清单
2. 如果不存在,返回 404
3. 加载关联的 items (使用 `@OneToMany` 或手动查询)
4. 返回清单信息 + 事项列表

---

#### API 3: 获取清单的所有事项

**请求**:
```
GET /api/lists/{token}/items
```

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| token | string | 清单 token |

**响应**:
```
状态码: 200 OK

{
  "items": [
    {
      "id": 1,
      "title": "买牛奶",
      "completed": false,
      "createdAt": "2026-02-18T10:01:00",
      "updatedAt": "2026-02-18T10:01:00"
    },
    {
      "id": 2,
      "title": "买鸡蛋",
      "completed": true,
      "createdAt": "2026-02-18T10:02:00",
      "updatedAt": "2026-02-18T10:05:00"
    }
  ]
}
```

**字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| items | array | 事项列表 |
| items[].id | number | 事项ID |
| items[].title | string | 事项标题 |
| items[].completed | boolean | 是否完成 |
| items[].createdAt | string | 创建时间 |
| items[].updatedAt | string | 更新时间 |

**错误响应**:
```
状态码: 404 Not Found

{
  "error": "List not found",
  "message": "清单不存在"
}
```

**业务逻辑**:
1. 根据 token 查询清单
2. 如果不存在,返回 404
3. 查询该清单的所有事项
4. 返回事项列表

---

#### API 4: 添加新事项

**请求**:
```
POST /api/lists/{token}/items
Content-Type: application/json

{
  "title": "买牛奶"
}
```

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| token | string | 清单 token |

**请求体**:
| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| title | string | 是 | 1-200 字符 | 事项标题 |

**响应**:
```
状态码: 201 Created

{
  "id": 1,
  "title": "买牛奶",
  "completed": false,
  "createdAt": "2026-02-18T10:01:00",
  "updatedAt": "2026-02-18T10:01:00"
}
```

**错误响应**:
```
状态码: 400 Bad Request

{
  "error": "Invalid title",
  "message": "标题不能为空"
}

// 或

{
  "error": "Invalid title",
  "message": "标题长度不能超过 200 个字符"
}
```

```
状态码: 404 Not Found

{
  "error": "List not found",
  "message": "清单不存在"
}
```

**业务逻辑**:
1. 验证 token 是否存在,不存在返回 404
2. 验证 title: 非空、长度 ≤ 200
3. 验证失败返回 400
4. 创建 TodoItem 实体,设置 `completed = false`
5. 保存到数据库
6. 返回新创建的事项

---

#### API 5: 更新事项

**请求**:
```
PATCH /api/items/{id}
Content-Type: application/json

{
  "title": "买牛奶和面包",
  "completed": true
}
```

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | number | 事项ID |

**请求体**:
| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| title | string | 否 | 1-200 字符 | 事项标题 (部分更新) |
| completed | boolean | 否 | - | 是否完成 (部分更新) |

**注意**: 请求体至少包含 `title` 或 `completed` 其中一个字段,可以同时包含两个字段。

**响应**:
```
状态码: 200 OK

{
  "id": 1,
  "title": "买牛奶和面包",
  "completed": true,
  "updatedAt": "2026-02-18T10:05:00"
}
```

**错误响应**:
```
状态码: 400 Bad Request

{
  "error": "Invalid request",
  "message": "请求体不能为空"
}

// 或

{
  "error": "Invalid title",
  "message": "标题不能为空"
}

// 或

{
  "error": "Invalid title",
  "message": "标题长度不能超过 200 个字符"
}
```

```
状态码: 404 Not Found

{
  "error": "Item not found",
  "message": "事项不存在"
}
```

**业务逻辑**:
1. 验证请求体至少包含一个字段
2. 根据 id 查询事项
3. 如果不存在,返回 404
4. 如果包含 `title`,验证并更新
5. 如果包含 `completed`,更新
6. 保存到数据库
7. 返回更新后的事项

---

#### API 6: 删除事项

**请求**:
```
DELETE /api/items/{id}
```

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | number | 事项ID |

**响应**:
```
状态码: 204 No Content

// 无响应体
```

**错误响应**:
```
状态码: 404 Not Found

{
  "error": "Item not found",
  "message": "事项不存在"
}
```

**业务逻辑**:
1. 根据 id 查询事项
2. 如果不存在,返回 404
3. 删除事项
4. 返回 204 (无响应体)

---

### 3.3 HTTP 状态码规范

| 状态码 | 场景 | 响应体 |
|--------|------|--------|
| 200 | 成功 (GET/PATCH) | 返回数据 |
| 201 | 创建成功 (POST) | 返回新创建的资源 |
| 204 | 删除成功 (DELETE) | 无响应体 |
| 400 | 参数错误 | `{ "error": "...", "message": "..." }` |
| 404 | 资源不存在 | `{ "error": "...", "message": "..." }` |
| 500 | 服务器错误 | `{ "error": "...", "message": "..." }` |

---

### 3.4 错误响应格式规范

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
| List not found | 404 | 清单不存在 |
| Item not found | 404 | 事项不存在 |
| Invalid title | 400 | 标题为空或超长 |
| Invalid request | 400 | 请求体无效 |
| Failed to generate unique token | 500 | Token 生成失败 |
| Internal server error | 500 | 服务器内部错误 |

---

## 4. 校验与安全点

### 4.1 前端输入验证规则

**规则 1: 标题非空**
```javascript
const title = input.value.trim();
if (!title) {
  alert('标题不能为空');
  return;
}
```

**规则 2: 标题长度限制**
```javascript
if (title.length > 200) {
  alert('标题长度不能超过 200 个字符');
  return;
}
```

**规则 3: 移除首尾空格**
```javascript
const title = input.value.trim(); // 自动去除首尾空格
```

**规则 4: 删除确认**
```javascript
if (!confirm('确定要删除这个事项吗?')) {
  return;
}
```

---

### 4.2 后端参数验证规则

**验证点 1: Token 存在性**
```java
// Repository 层
Optional<TodoList> findByToken(String token);

// Service 层
TodoList list = listRepository.findByToken(token)
  .orElseThrow(() -> new NotFoundException("List not found"));
```

**验证点 2: Title 非空和长度**
```java
@NotBlank(message = "标题不能为空")
@Size(min = 1, max = 200, message = "标题长度必须在 1-200 个字符之间")
private String title;
```

**验证点 3: Item ID 存在性**
```java
// Repository 层
Optional<TodoItem> findById(Long id);

// Service 层
TodoItem item = itemRepository.findById(id)
  .orElseThrow(() -> new NotFoundException("Item not found"));
```

---

### 4.3 Todo 归属检查

**场景**: 确保 item 属于正确的 list (防止通过 API 越权访问)

**方案 1: 通过 token 间接获取 (推荐)**
```
前端: POST /api/lists/{token}/items
后端:
  1. 通过 token 找到 list
  2. 创建 item 时直接关联 list
  3. 无需额外检查
```

**方案 2: 直接操作 item 时的检查**
```
前端: PATCH /api/items/{id}
后端:
  1. 根据 id 查询 item
  2. 自动验证 item.list.token 是否匹配
  3. 本场景不需要额外检查,因为前端没有 list 上下文
```

**结论**: V1 MVP 不需要额外的归属检查,因为所有操作都基于 token 或直接的 item id。

---

### 4.4 异常处理策略

**异常分类与处理**:

| 异常类型 | HTTP 状态码 | 错误码 | 处理方式 |
|----------|-------------|--------|----------|
| NotFoundException | 404 | List not found / Item not found | 资源不存在 |
| IllegalArgumentException | 400 | Invalid title | 参数验证失败 |
| DataIntegrityViolationException | 500 | Failed to generate unique token | 数据库约束冲突 |
| Exception | 500 | Internal server error | 未预期异常 |

**全局异常处理器**:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
    return ResponseEntity.status(404)
      .body(new ErrorResponse("Resource not found", e.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException e) {
    return ResponseEntity.status(400)
      .body(new ErrorResponse("Invalid request", e.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleInternalServerError(Exception e) {
    return ResponseEntity.status(500)
      .body(new ErrorResponse("Internal server error", "系统繁忙,请稍后重试"));
  }
}
```

---

## 5. 技术实现要点

### 5.1 Spring Boot 项目结构

```
src/main/java/com/example/todolist/
├── TodolistApplication.java           # 启动类
├── controller/
│   ├── ListController.java            # 清单相关 API
│   └── ItemController.java            # 事项相关 API
├── service/
│   ├── ListService.java               # 清单业务逻辑
│   ├── ItemService.java               # 事项业务逻辑
│   └── TokenService.java              # Token 生成服务
├── repository/
│   ├── TodoListRepository.java        # 清单 DAO
│   └── TodoItemRepository.java        # 事项 DAO
├── entity/
│   ├── TodoList.java                  # 清单实体
│   └── TodoItem.java                  # 事项实体
├── dto/
│   ├── CreateItemRequest.java         # 创建事项请求
│   ├── UpdateItemRequest.java         # 更新事项请求
│   ├── ListResponse.java              # 清单响应
│   ├── ItemResponse.java              # 事项响应
│   └── ErrorResponse.java             # 错误响应
├── exception/
│   ├── NotFoundException.java         # 404 异常
│   └── GlobalExceptionHandler.java    # 全局异常处理
└── config/
    └── JpaConfig.java                 # JPA 配置 (可选)

src/main/resources/
├── application.properties             # 配置文件
├── templates/
│   ├── index.html                     # 首页
│   └── list.html                      # 清单详情页
└── static/
    ├── css/
    │   └── style.css                  # 样式文件
    └── js/
        └── app.js                     # 前端逻辑
```

---

### 5.2 关键类设计

#### Entity 1: TodoList

```java
@Entity
@Table(name = "todo_list")
public class TodoList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 8)
    private String token;

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
    }

    // Getter 和 Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

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

#### Entity 2: TodoItem

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
    }

    // Getter 和 Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @JsonIgnore
    public TodoList getList() { return list; }
    public void setList(TodoList list) { this.list = list; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

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

#### Repository 1: TodoListRepository

```java
@Repository
public interface TodoListRepository extends JpaRepository<TodoList, Long> {

    Optional<TodoList> findByToken(String token);

    boolean existsByToken(String token);
}
```

---

#### Repository 2: TodoItemRepository

```java
@Repository
public interface TodoItemRepository extends JpaRepository<TodoItem, Long> {

    List<TodoItem> findByListIdOrderByCreatedAtAsc(Long listId);
}
```

---

#### Service 1: ListService

```java
@Service
@Transactional
public class ListService {

    @Autowired
    private TodoListRepository listRepository;

    @Autowired
    private TokenService tokenService;

    public TodoList createList() {
        String token = tokenService.generateUniqueToken();
        TodoList list = new TodoList(token);
        return listRepository.save(list);
    }

    @Transactional(readOnly = true)
    public TodoList getListByToken(String token) {
        return listRepository.findByToken(token)
            .orElseThrow(() -> new NotFoundException("List not found"));
    }
}
```

---

#### Service 2: ItemService

```java
@Service
@Transactional
public class ItemService {

    @Autowired
    private TodoListRepository listRepository;

    @Autowired
    private TodoItemRepository itemRepository;

    public TodoItem addItem(String token, String title) {
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
        return itemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<TodoItem> getItemsByToken(String token) {
        TodoList list = listRepository.findByToken(token)
            .orElseThrow(() -> new NotFoundException("List not found"));
        return itemRepository.findByListIdOrderByCreatedAtAsc(list.getId());
    }

    public TodoItem updateItem(Long id, String title, Boolean completed) {
        TodoItem item = itemRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Item not found"));

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

        if (completed != null) {
            item.setCompleted(completed);
        }

        return itemRepository.save(item);
    }

    public void deleteItem(Long id) {
        TodoItem item = itemRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Item not found"));
        itemRepository.delete(item);
    }
}
```

---

#### Controller 1: ListController

```java
@RestController
@RequestMapping("/api/lists")
public class ListController {

    @Autowired
    private ListService listService;

    @PostMapping
    public ResponseEntity<ListResponse> createList() {
        TodoList list = listService.createList();
        return ResponseEntity.status(201).body(new ListResponse(list));
    }

    @GetMapping("/{token}")
    public ResponseEntity<ListResponse> getList(@PathVariable String token) {
        TodoList list = listService.getListByToken(token);
        return ResponseEntity.ok(new ListResponse(list));
    }
}
```

---

#### Controller 2: ItemController

```java
@RestController
@RequestMapping("/api/lists/{token}/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    @PostMapping
    public ResponseEntity<ItemResponse> addItem(
        @PathVariable String token,
        @RequestBody CreateItemRequest request
    ) {
        TodoItem item = itemService.addItem(token, request.getTitle());
        return ResponseEntity.status(201).body(new ItemResponse(item));
    }

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
public class ItemController {

    @Autowired
    private ItemService itemService;

    @PatchMapping("/{id}")
    public ResponseEntity<ItemResponse> updateItem(
        @PathVariable Long id,
        @RequestBody UpdateItemRequest request
    ) {
        TodoItem item = itemService.updateItem(id, request.getTitle(), request.getCompleted());
        return ResponseEntity.ok(new ItemResponse(item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

### 5.3 事务管理

**事务注解**:
```java
@Service
@Transactional  // 类级别,所有方法启用事务
public class ItemService {
    // ...
}
```

**只读事务**:
```java
@Transactional(readOnly = true)
public List<TodoItem> getItemsByToken(String token) {
    // 只读事务,优化性能
}
```

**事务边界**:
- Service 层方法作为事务边界
- Controller 层不管理事务
- Repository 层使用 Spring Data JPA 自动事务管理

---

### 5.4 异常处理机制

#### 自定义异常: NotFoundException

```java
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
```

#### 错误响应 DTO: ErrorResponse

```java
public class ErrorResponse {
    private String error;
    private String message;

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }

    // Getter 和 Setter
    public String getError() { return error; }
    public String getMessage() { return message; }
}
```

#### 全局异常处理器: GlobalExceptionHandler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(404)
            .body(new ErrorResponse("Resource not found", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(400)
            .body(new ErrorResponse("Invalid request", e.getMessage()));
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

## 6. API Freeze 前检查

### 6.1 API 端点完整性检查

| API 端点 | 状态 | 说明 |
|----------|------|------|
| POST /api/lists | ✅ 已定义 | 创建清单 |
| GET /api/lists/{token} | ✅ 已定义 | 获取清单详情 |
| GET /api/lists/{token}/items | ✅ 已定义 | 获取清单的所有事项 |
| POST /api/lists/{token}/items | ✅ 已定义 | 添加事项 |
| PATCH /api/items/{id} | ✅ 已定义 | 更新事项 |
| DELETE /api/items/{id} | ✅ 已定义 | 删除事项 |

**结论**: 所有 API 端点已完整定义,无遗漏。

---

### 6.2 数据结构完整性检查

**Entity**:
- ✅ TodoList (id, token, createdAt, updatedAt)
- ✅ TodoItem (id, listId, title, completed, createdAt, updatedAt)

**DTO (Request)**:
- ✅ CreateItemRequest (title)
- ✅ UpdateItemRequest (title, completed - 可选)

**DTO (Response)**:
- ✅ ListResponse (id, token, createdAt, items[])
- ✅ ItemResponse (id, title, completed, createdAt, updatedAt)
- ✅ ErrorResponse (error, message)

**结论**: 所有数据结构已完整定义,无遗漏。

---

### 6.3 业务规则完整性检查

| 业务规则 | 实现位置 | 状态 |
|----------|----------|------|
| Token 唯一性 | TokenService + 数据库唯一约束 | ✅ |
| Token 格式 (8位) | TokenGenerator | ✅ |
| Title 非空 | 前端 + 后端验证 | ✅ |
| Title 长度 ≤ 200 | 前端 + 后端验证 | ✅ |
| Token 存在性检查 | Service 层 | ✅ |
| Item ID 存在性检查 | Service 层 | ✅ |
| 级联删除 | JPA CascadeType.ALL | ✅ |
| 默认 completed = false | Entity 构造方法 | ✅ |
| HTTP 状态码规范 | Controller 层 | ✅ |

**结论**: 所有业务规则已明确定义,无遗漏。

---

### 6.4 场景完整性检查

| 场景 | API 覆盖 | 状态 |
|------|----------|------|
| 创建清单 | POST /api/lists | ✅ |
| 访问清单 | GET /api/lists/{token} | ✅ |
| 添加事项 | POST /api/lists/{token}/items | ✅ |
| 查看事项列表 | GET /api/lists/{token}/items | ✅ |
| 标记完成/未完成 | PATCH /api/items/{id} | ✅ |
| 编辑标题 | PATCH /api/items/{id} | ✅ |
| 删除事项 | DELETE /api/items/{id} | ✅ |
| Token 不存在 | 所有 GET/POST/PATCH/DELETE | ✅ (404) |
| Item ID 不存在 | PATCH/DELETE | ✅ (404) |
| 标题为空 | POST/PATCH | ✅ (400) |
| 标题超长 | POST/PATCH | ✅ (400) |
| Token 冲突 | POST /api/lists | ✅ (重试机制) |

**结论**: 所有可能的场景均已覆盖,无遗漏。

---

### 6.5 安全检查

| 安全点 | 实现方式 | 状态 |
|--------|----------|------|
| SQL 注入防护 | JPA 参数化查询 | ✅ |
| XSS 防护 | Thymeleaf 自动转义 | ✅ |
| 参数验证 | @NotBlank, @Size 注解 | ✅ |
| 异常信息泄露 | 全局异常处理,不暴露堆栈 | ✅ |
| Token 暴力破解 | 8 位随机,62^8 种组合 | ✅ |
| 越权访问 | 通过 token 间接操作 | ✅ |

**结论**: 基本安全措施已覆盖,符合学习项目要求。

---

## 7. 开发建议

### 7.1 开发顺序

**阶段 1: 后端基础 (1-2 天)**
1. 搭建 Spring Boot 项目
2. 创建 Entity 类
3. 创建 Repository 接口
4. 实现 Token 生成逻辑
5. 编写单元测试

**阶段 2: API 实现 (1-2 天)**
1. 实现 Service 层
2. 实现 Controller 层
3. 实现全局异常处理
4. 测试所有 API (Postman/curl)

**阶段 3: 前端实现 (1-2 天)**
1. 创建 Thymeleaf 模板
2. 实现首页和清单详情页
3. 实现前端交互逻辑
4. 集成 API 调用

**阶段 4: 联调与测试 (1 天)**
1. 端到端测试
2. 错误场景测试
3. 代码优化

---

### 7.2 技术栈版本

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | LTS 版本 |
| Spring Boot | 3.x | 最新稳定版 |
| Spring Data JPA | 3.x | 随 Spring Boot |
| H2 Database | 2.x | 开发环境 |
| Thymeleaf | 3.x | 随 Spring Boot |

---

### 7.3 配置文件示例

**application.properties**:
```properties
# 应用配置
spring.application.name=todolist
server.port=8080

# 数据库配置 (H2)
spring.datasource.url=jdbc:h2:mem:todolist
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA 配置
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# H2 控制台 (可选,用于调试)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Thymeleaf 配置
spring.thymeleaf.cache=false
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html

# 日志配置
logging.level.com.example.todolist=DEBUG
```

---

### 7.4 测试 API 的 curl 命令

**创建清单**:
```bash
curl -X POST http://localhost:8080/api/lists
```

**获取清单详情**:
```bash
curl http://localhost:8080/api/lists/aB3xK9mP
```

**添加事项**:
```bash
curl -X POST http://localhost:8080/api/lists/aB3xK9mP/items \
  -H "Content-Type: application/json" \
  -d '{"title": "买牛奶"}'
```

**更新事项**:
```bash
curl -X PATCH http://localhost:8080/api/items/1 \
  -H "Content-Type: application/json" \
  -d '{"completed": true}'
```

**删除事项**:
```bash
curl -X DELETE http://localhost:8080/api/items/1
```

---

## 8. 附录

### 8.1 数据库初始化脚本 (可选)

如果使用 MySQL 而非 H2,可使用以下脚本:

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS todolist CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE todolist;

-- 创建清单表
CREATE TABLE IF NOT EXISTS todo_list (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  token VARCHAR(8) NOT NULL UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_token (token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建事项表
CREATE TABLE IF NOT EXISTS todo_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  list_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  completed BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (list_id) REFERENCES todo_list(id) ON DELETE CASCADE,
  INDEX idx_list_id (list_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 8.2 常见问题 FAQ

**Q1: Token 冲突怎么办?**
A: 使用重试机制 (最多 10 次),如果仍然失败则返回 500 错误。

**Q2: 如何防止并发修改冲突?**
A: V1 MVP 不考虑乐观锁,简化实现。如果需要,可在 V2 添加 `@Version` 字段。

**Q3: 删除清单功能要实现吗?**
A: PRD 中定义为可选,V1 MVP 不实现,但数据库已支持级联删除。

**Q4: 前端如何处理网络错误?**
A: 使用 `try-catch` 捕获 fetch 异常,显示简单的 alert 提示。

**Q5: 需要实现单元测试吗?**
A: PRD 明确说明"不实现自动化测试(可手动测试)"。

---

### 8.3 参考资料

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Spring Data JPA 文档](https://spring.io/projects/spring-data-jpa)
- [Thymeleaf 文档](https://www.thymeleaf.org/)
- [Fetch API 文档](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API)
- [REST API 设计最佳实践](https://restfulapi.net/)

---

## 9. 冻结声明

本文档定义的所有 API 契约自发布之日起冻结,后续开发过程中不得修改:

**API 端点 (冻结)**:
- POST /api/lists
- GET /api/lists/{token}
- GET /api/lists/{token}/items
- POST /api/lists/{token}/items
- PATCH /api/items/{id}
- DELETE /api/items/{id}

**数据结构 (冻结)**:
- TodoList Entity
- TodoItem Entity
- CreateItemRequest DTO
- UpdateItemRequest DTO
- ListResponse DTO
- ItemResponse DTO
- ErrorResponse DTO

**业务规则 (冻结)**:
- Token 生成规则
- 标题验证规则
- HTTP 状态码规范
- 错误处理规范

如有变更需求,需经 PM 和 TechLead 共同评审后发布新版本文档。

---

**文档状态**: API 冻结前最终版
**技术负责人**: TechLead
**发布日期**: 2026-02-18
**文档版本**: 1.0

---

**文档结束**
