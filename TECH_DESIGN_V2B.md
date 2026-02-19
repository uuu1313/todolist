# 共享待办清单 V2-B - 技术设计文档 (TECH_DESIGN)

**文档版本**: 2.0-B
**创建日期**: 2026-02-19
**技术负责人**: TechLead
**文档状态**: Design Complete

---

## 1. 概述

### 1.1 设计目标

基于 PRD_V2B.md，设计并实现多用户协作基础能力，包括：
- 用户模型和用户名管理
- 清单成员关系管理
- 邀请令牌机制
- 基于角色的权限控制

### 1.2 设计原则

- **可运行优先**: 确保功能可用，不过度设计
- **向后兼容**: 保持 V1/V2-A API 继续可用
- **简单直接**: 避免过度抽象，保持代码清晰
- **渐进增强**: 在现有基础上添加新功能，不重构现有代码

### 1.3 技术栈

- **Java 17** + **Spring Boot 3.2.0**
- **Spring Data JPA** (Hibernate)
- **H2 Database** (开发环境)
- **Apache Commons Lang 3.x** (随机字符串生成)

---

## 2. 数据库设计

### 2.1 新增表：user

**表名**: `user`

```sql
CREATE TABLE user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**字段说明**:

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 主键 |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 用户名 |
| created_at | TIMESTAMP | NOT NULL | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL | 更新时间 |

**索引**:
- `idx_username` - 用户名唯一索引（加速查询）

---

### 2.2 新增表：list_member

**表名**: `list_member`

```sql
CREATE TABLE list_member (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  list_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role VARCHAR(6) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  FOREIGN KEY (list_id) REFERENCES todo_list(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
  UNIQUE KEY uk_list_user (list_id, user_id),
  INDEX idx_list_id (list_id),
  INDEX idx_user_id (user_id),
  CONSTRAINT chk_role CHECK (role IN ('OWNER', 'MEMBER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**字段说明**:

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 主键 |
| list_id | BIGINT | NOT NULL, FOREIGN KEY | 清单ID |
| user_id | BIGINT | NOT NULL, FOREIGN KEY | 用户ID |
| role | VARCHAR(6) | NOT NULL, CHECK | 角色（OWNER/MEMBER） |
| created_at | TIMESTAMP | NOT NULL | 加入时间 |

**约束**:
- `uk_list_user` - 唯一约束，防止用户重复加入同一清单
- `chk_role` - 检查约束，角色只能是 OWNER 或 MEMBER
- 外键级联删除：清单或用户删除时，自动删除成员关系

**索引**:
- `idx_list_id` - 清单ID索引（加速查询清单成员）
- `idx_user_id` - 用户ID索引（加速查询用户加入的清单）

---

### 2.3 新增表：invite_token

**表名**: `invite_token`

```sql
CREATE TABLE invite_token (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  list_id BIGINT NOT NULL,
  token VARCHAR(12) UNIQUE NOT NULL,
  created_at TIMESTAMP NOT NULL,
  FOREIGN KEY (list_id) REFERENCES todo_list(id) ON DELETE CASCADE,
  INDEX idx_token (token),
  INDEX idx_list_id (list_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**字段说明**:

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 主键 |
| list_id | BIGINT | NOT NULL, FOREIGN KEY | 清单ID |
| token | VARCHAR(12) | UNIQUE, NOT NULL | 邀请令牌 |
| created_at | TIMESTAMP | NOT NULL | 创建时间 |

**索引**:
- `idx_token` - 令牌唯一索引（加速验证邀请）
- `idx_list_id` - 清单ID索引（加速查询清单的邀请）

**注意**: V2-B 阶段不存储创建者信息，简化设计。

---

### 2.4 现有表变更：todo_item

**表名**: `todo_item` (修改)

```sql
ALTER TABLE todo_item
ADD COLUMN created_by_id BIGINT DEFAULT NULL,
ADD COLUMN updated_by_id BIGINT DEFAULT NULL;

ALTER TABLE todo_item
ADD FOREIGN KEY (created_by_id) REFERENCES user(id) ON DELETE SET NULL,
ADD FOREIGN KEY (updated_by_id) REFERENCES user(id) ON DELETE SET NULL;
```

**新增字段说明**:

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| created_by_id | BIGINT | FOREIGN KEY, nullable | 创建者用户ID |
| updated_by_id | BIGINT | FOREIGN KEY, nullable | 最后更新者用户ID |

**外键行为**: `ON DELETE SET NULL` - 用户删除时，字段置为 NULL，不删除 todo

---

## 3. 实体关系图 (ERD)

### 3.1 关系概述

```
User (用户)
  ↓ 1:N
ListMember (成员关系)
  ↑ N:1
  ↓ N:1
TodoList (清单)
  ↓ 1:N
TodoItem (待办事项)

TodoList
  ↓ 1:N
InviteToken (邀请令牌)
```

### 3.2 关系详解

**User ↔ ListMember (One-to-Many)**
- 一个用户可以有多个成员关系（加入多个清单）
- 一个成员关系只属于一个用户

**TodoList ↔ ListMember (One-to-Many)**
- 一个清单可以有多个成员
- 一个成员关系只属于一个清单

**TodoList ↔ InviteToken (One-to-Many)**
- 一个清单可以生成多个邀请令牌
- 一个邀请令牌只属于一个清单

**User ↔ TodoItem (Many-to-Many, through ListMember)**
- 间接关系：通过 ListMember 表建立多对多关系
- 用户可以创建/更新多个 todo
- 一个 todo 有一个创建者和一个更新者

### 3.3 多对多关系实现

**User ↔ TodoList (Many-to-Many, through ListMember)**

```
User                    ListMember                  TodoList
----                    ------------                  -------
id (PK)    ←----------  user_id (FK)  ----------→   id (PK)
username                 list_id (FK)                   token
                         role (OWNER/MEMBER)           title
                         created_at                    created_at
```

- **中间表**: `list_member`
- **关系字段**: `user_id`, `list_id`
- **附加属性**: `role`, `created_at`
- **唯一约束**: `(list_id, user_id)` 防止重复加入

---

## 4. 实体类设计

### 4.1 User 实体

**包路径**: `com.example.todolist.entity.User`

```java
package com.example.todolist.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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

    // Constructors
    public User() {}

    public User(String username) {
        this.username = username;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
```

**设计说明**:
- 使用 `@CreationTimestamp` 和 `@UpdateTimestamp` 自动管理时间
- `username` 唯一约束由数据库保证
- 默认构造函数用于 JPA 反射

---

### 4.2 MemberRole 枚举

**包路径**: `com.example.todolist.entity.MemberRole`

```java
package com.example.todolist.entity;

/**
 * 清单成员角色枚举
 */
public enum MemberRole {
    /**
     * 所有者 - 可以管理成员
     */
    OWNER,

    /**
     * 成员 - 可以编辑 todos
     */
    MEMBER
}
```

**设计说明**:
- 使用 `EnumType.STRING` 存储到数据库
- 简单的两个角色，无需复杂的权限系统

---

### 4.3 ListMember 实体

**包路径**: `com.example.todolist.entity.ListMember`

```java
package com.example.todolist.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "list_member",
    uniqueConstraints = @UniqueConstraint(columnNames = {"list_id", "user_id"})
)
public class ListMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    @JsonIgnore
    private TodoList list;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private MemberRole role = MemberRole.MEMBER;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public ListMember() {}

    public ListMember(TodoList list, User user, MemberRole role) {
        this.list = list;
        this.user = user;
        this.role = role;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TodoList getList() {
        return list;
    }

    public void setList(TodoList list) {
        this.list = list;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public MemberRole getRole() {
        return role;
    }

    public void setRole(MemberRole role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
```

**设计说明**:
- `FetchType.LAZY` 避免 N+1 查询问题
- `@JsonIgnore` 防止序列化时的循环引用
- 唯一约束在 `@Table` 注解中声明
- 默认角色为 `MEMBER`

---

### 4.4 InviteToken 实体

**包路径**: `com.example.todolist.entity.InviteToken`

```java
package com.example.todolist.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "invite_token")
public class InviteToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    @JsonIgnore
    private TodoList list;

    @Column(unique = true, nullable = false, length = 12)
    private String token;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public InviteToken() {}

    public InviteToken(TodoList list, String token) {
        this.list = list;
        this.token = token;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TodoList getList() {
        return list;
    }

    public void setList(TodoList list) {
        this.list = list;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
```

**设计说明**:
- `token` 长度固定为 12
- 唯一约束防止重复
- 不存储创建者，简化设计

---

### 4.5 TodoItem 实体（扩展）

**包路径**: `com.example.todolist.entity.TodoItem`

**变更内容**: 添加两个新字段

```java
// 在现有 TodoItem 类中添加以下字段和 getter/setter

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "created_by_id")
private User createdBy;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "updated_by_id")
private User updatedBy;

// Getters and Setters
public User getCreatedBy() {
    return createdBy;
}

public void setCreatedBy(User createdBy) {
    this.createdBy = createdBy;
}

public User getUpdatedBy() {
    return updatedBy;
}

public void setUpdatedBy(User updatedBy) {
    this.updatedBy = updatedBy;
}
```

**设计说明**:
- `nullable = true` 允许 NULL（向后兼容 V1/V2-A）
- `FetchType.LAZY` 避免不必要的数据加载
- 使用 `@JsonIgnore` 避免循环引用

---

## 5. Repository 接口设计

### 5.1 UserRepository

**包路径**: `com.example.todolist.repository.UserRepository`

```java
package com.example.todolist.repository;

import com.example.todolist.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查询用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查用户名是否存在（排除指定用户ID）
     */
    boolean existsByUsernameAndIdNot(String username, Long id);
}
```

---

### 5.2 ListMemberRepository

**包路径**: `com.example.todolist.repository.ListMemberRepository`

```java
package com.example.todolist.repository;

import com.example.todolist.entity.ListMember;
import com.example.todolist.entity.TodoList;
import com.example.todolist.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListMemberRepository extends JpaRepository<ListMember, Long> {

    /**
     * 查询清单的所有成员
     */
    List<ListMember> findByListId(Long listId);

    /**
     * 查询清单的所有成员（通过实体）
     */
    List<ListMember> findByList(TodoList list);

    /**
     * 查询用户加入的所有清单
     */
    List<ListMember> findByUserId(Long userId);

    /**
     * 查询用户在指定清单中的成员关系
     */
    Optional<ListMember> findByListAndUser(TodoList list, User user);

    /**
     * 检查用户是否是指定清单的成员
     */
    boolean existsByListAndUser(TodoList list, User user);

    /**
     * 检查用户是否是指定清单的所有者
     */
    @Query("SELECT CASE WHEN lm.role = 'OWNER' THEN true ELSE false END " +
           "FROM ListMember lm WHERE lm.list = :list AND lm.user = :user")
    boolean isOwner(@Param("list") TodoList list, @Param("user") User user);

    /**
     * 删除清单的所有成员
     */
    void deleteByListId(Long listId);
}
```

---

### 5.3 InviteTokenRepository

**包路径**: `com.example.todolist.repository.InviteTokenRepository`

```java
package com.example.todolist.repository;

import com.example.todolist.entity.InviteToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InviteTokenRepository extends JpaRepository<InviteToken, Long> {

    /**
     * 根据令牌查询邀请
     */
    Optional<InviteToken> findByToken(String token);

    /**
     * 检查令牌是否存在
     */
    boolean existsByToken(String token);

    /**
     * 查询清单的所有邀请令牌
     */
    List<InviteToken> findByListId(Long listId);

    /**
     * 删除清单的所有邀请令牌
     */
    void deleteByListId(Long listId);
}
```

---

## 6. API 设计

### 6.1 新增 API 端点

#### API 1: 创建用户

**端点**: `POST /api/users`

**功能**: 创建新用户（首次访问时自动调用）

**请求示例**:
```http
POST /api/users HTTP/1.1
Content-Type: application/json

{
  "username": "用户_abc123"
}
```

**请求体** (CreateUserRequest):
```java
{
  "username": "用户_abc123"  // 可选，不传则自动生成
}
```

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
```java
{
  "username": "张三"
}
```

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

### 6.2 修改现有 API 端点

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
      "createdBy": "张三",
      "updatedBy": "李四",
      "createdAt": "2026-02-19T10:01:00",
      "updatedAt": "2026-02-19T10:05:00"
    }
  ]
}
```

**向后兼容性**:
- V1/V2-A 客户端忽略新增字段

---

### 6.3 HTTP 状态码规范

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

### 6.4 错误响应格式规范

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

## 7. Service 层设计

### 7.1 UserService

**包路径**: `com.example.todolist.service.UserService`

```java
package com.example.todolist.service;

import com.example.todolist.entity.User;
import com.example.todolist.repository.UserRepository;
import com.example.todolist.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * 创建匿名用户（自动生成用户名）
     */
    public User createAnonymousUser() {
        User user = new User();
        user.setUsername(generateUsername());
        return userRepository.save(user);
    }

    /**
     * 创建用户（指定用户名）
     */
    public User createUser(String username) {
        // 验证用户名唯一性
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User(username);
        return userRepository.save(user);
    }

    /**
     * 更新用户名
     */
    public User updateUser(Long userId, String newUsername) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("用户不存在"));

        if (userRepository.existsByUsernameAndIdNot(newUsername, userId)) {
            throw new IllegalArgumentException("用户名已存在");
        }

        user.setUsername(newUsername);
        return userRepository.save(user);
    }

    /**
     * 生成随机用户名
     * 格式: "用户_" + 6位随机字符
     */
    private String generateUsername() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return "用户_" + sb.toString();
    }
}
```

---

### 7.2 MemberService

**包路径**: `com.example.todolist.service.MemberService`

```java
package com.example.todolist.service;

import com.example.todolist.entity.ListMember;
import com.example.todolist.entity.MemberRole;
import com.example.todolist.entity.TodoList;
import com.example.todolist.entity.User;
import com.example.todolist.repository.ListMemberRepository;
import com.example.todolist.repository.TodoListRepository;
import com.example.todolist.repository.UserRepository;
import com.example.todolist.exception.NotFoundException;
import com.example.todolist.exception.ForbiddenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MemberService {

    @Autowired
    private ListMemberRepository memberRepository;

    @Autowired
    private TodoListRepository listRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 添加成员
     */
    public ListMember addMember(String listToken, Long userId, MemberRole role) {
        TodoList list = listRepository.findByToken(listToken)
            .orElseThrow(() -> new NotFoundException("清单不存在"));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("用户不存在"));

        // 检查是否已是成员
        if (memberRepository.existsByListAndUser(list, user)) {
            throw new IllegalStateException("用户已是成员");
        }

        ListMember member = new ListMember(list, user, role);
        return memberRepository.save(member);
    }

    /**
     * 移除成员
     */
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

    /**
     * 获取成员列表
     */
    public List<MemberResponse> getMembers(String listToken) {
        TodoList list = listRepository.findByToken(listToken)
            .orElseThrow(() -> new NotFoundException("清单不存在"));

        return memberRepository.findByList(list).stream()
            .map(MemberResponse::new)
            .collect(Collectors.toList());
    }

    /**
     * 检查用户是否是所有者
     */
    private boolean isOwner(TodoList list, User user) {
        return memberRepository.isOwner(list, user);
    }
}
```

---

### 7.3 InviteService

**包路径**: `com.example.todolist.service.InviteService`

```java
package com.example.todolist.service;

import com.example.todolist.entity.InviteToken;
import com.example.todolist.entity.MemberRole;
import com.example.todolist.entity.TodoList;
import com.example.todolist.repository.InviteTokenRepository;
import com.example.todolist.repository.TodoListRepository;
import com.example.todolist.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@Transactional
public class InviteService {

    @Autowired
    private InviteTokenRepository inviteTokenRepository;

    @Autowired
    private TodoListRepository listRepository;

    @Autowired
    private MemberService memberService;

    /**
     * 创建邀请令牌
     */
    public InviteToken createInvite(String listToken) {
        TodoList list = listRepository.findByToken(listToken)
            .orElseThrow(() -> new NotFoundException("清单不存在"));

        String token = generateInviteToken();
        InviteToken invite = new InviteToken(list, token);
        return inviteTokenRepository.save(invite);
    }

    /**
     * 通过邀请令牌加入清单
     */
    public TodoList joinList(String inviteToken, Long userId) {
        InviteToken invite = inviteTokenRepository.findByToken(inviteToken)
            .orElseThrow(() -> new NotFoundException("邀请令牌无效"));

        TodoList list = invite.getList();
        memberService.addMember(list.getToken(), userId, MemberRole.MEMBER);
        return list;
    }

    /**
     * 生成唯一的邀请令牌
     * 格式: 12位随机字符
     */
    private String generateInviteToken() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        String token;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            token = sb.toString();
        } while (inviteTokenRepository.existsByToken(token));
        return token;
    }
}
```

---

## 8. Controller 层设计

### 8.1 UserController

**包路径**: `com.example.todolist.controller.UserController`

```java
package com.example.todolist.controller;

import com.example.todolist.dto.CreateUserRequest;
import com.example.todolist.dto.UpdateUserRequest;
import com.example.todolist.dto.UserResponse;
import com.example.todolist.entity.User;
import com.example.todolist.service.UserService;
import com.example.todolist.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        @RequestBody UpdateUserRequest request
    ) {
        try {
            User user = userService.updateUser(id, request.getUsername());
            return ResponseEntity.ok(new UserResponse(user));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

---

### 8.2 MemberController

**包路径**: `com.example.todolist.controller.MemberController`

```java
package com.example.todolist.controller;

import com.example.todolist.dto.MemberResponse;
import com.example.todolist.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        @RequestHeader(value = "X-User-Id", required = false) Long operatorId
    ) {
        if (operatorId == null) {
            return ResponseEntity.status(403).build();
        }
        memberService.removeMember(token, userId, operatorId);
        return ResponseEntity.noContent().build();
    }
}
```

---

### 8.3 InviteController

**包路径**: `com.example.todolist.controller.InviteController`

```java
package com.example.todolist.controller;

import com.example.todolist.dto.InviteResponse;
import com.example.todolist.dto.JoinListRequest;
import com.example.todolist.dto.JoinResponse;
import com.example.todolist.entity.InviteToken;
import com.example.todolist.entity.TodoList;
import com.example.todolist.service.InviteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lists")
public class InviteController {

    @Autowired
    private InviteService inviteService;

    @PostMapping("/{token}/invites")
    public ResponseEntity<InviteResponse> createInvite(
        @PathVariable String token,
        @RequestHeader(value = "X-User-Id", required = false) Long creatorId
    ) {
        // V2-B 暂不验证权限，后续版本添加
        InviteToken invite = inviteService.createInvite(token);
        String inviteUrl = "http://localhost:8080/join?invite=" + invite.getToken();
        return ResponseEntity.status(201).body(new InviteResponse(invite, inviteUrl));
    }

    @PostMapping("/join")
    public ResponseEntity<JoinResponse> joinList(
        @RequestBody @Valid JoinListRequest request,
        @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        TodoList list = inviteService.joinList(request.getInviteToken(), userId);
        return ResponseEntity.ok(new JoinResponse(list.getToken(), "MEMBER"));
    }
}
```

---

### 8.4 修改现有 Controller

#### ListController 扩展

**变更内容**: 创建清单时自动添加所有者

```java
// 在 ListController 的 createList 方法中添加：

@Autowired
private MemberService memberService;

@PostMapping
public ResponseEntity<ListResponse> createList(
    @RequestHeader(value = "X-User-Id", required = false) Long userId
) {
    String token = tokenService.generateToken();
    TodoList list = listService.createList(token);

    // 如果提供了用户ID，自动添加为所有者
    if (userId != null) {
        try {
            memberService.addMember(token, userId, MemberRole.OWNER);
        } catch (Exception e) {
            // 忽略错误，保持向后兼容
        }
    }

    return ResponseEntity.status(201).body(new ListResponse(list));
}
```

#### ItemController 扩展

**变更内容**: 创建/更新 todo 时记录用户

```java
// 在 ItemController 中添加：

@Autowired
private UserRepository userRepository;

@PostMapping
public ResponseEntity<ItemResponse> createItem(
    @PathVariable String token,
    @RequestBody @Valid CreateItemRequest request,
    @RequestHeader(value = "X-User-Id", required = false) Long userId
) {
    TodoItem item = itemService.createItem(token, request);

    // 如果提供了用户ID，记录创建者
    if (userId != null) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            item.setCreatedBy(user);
            itemService.updateItem(item);
        }
    }

    return ResponseEntity.status(201).body(new ItemResponse(item));
}
```

---

## 9. DTO 设计

### 9.1 请求 DTO

**CreateUserRequest**:
```java
package com.example.todolist.dto;

public class CreateUserRequest {
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
```

**UpdateUserRequest**:
```java
package com.example.todolist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateUserRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 1, max = 50, message = "用户名长度必须在1-50字符之间")
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
```

**JoinListRequest**:
```java
package com.example.todolist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class JoinListRequest {

    @NotBlank(message = "邀请令牌不能为空")
    @Size(min = 12, max = 12, message = "邀请令牌必须是12位")
    private String inviteToken;

    public String getInviteToken() {
        return inviteToken;
    }

    public void setInviteToken(String inviteToken) {
        this.inviteToken = inviteToken;
    }
}
```

---

### 9.2 响应 DTO

**UserResponse**:
```java
package com.example.todolist.dto;

import com.example.todolist.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class UserResponse {
    private Long id;
    private String username;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }

    // Getters...
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

**MemberResponse**:
```java
package com.example.todolist.dto;

import com.example.todolist.entity.ListMember;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class MemberResponse {
    private Long id;
    private Long userId;
    private String username;
    private String role;
    private String roleDisplay; // "所有者" 或 "成员"

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime joinedAt;

    public MemberResponse(ListMember member) {
        this.id = member.getId();
        this.userId = member.getUser().getId();
        this.username = member.getUser().getUsername();
        this.role = member.getRole().name();
        this.roleDisplay = member.getRole() == MemberRole.OWNER ? "所有者" : "成员";
        this.joinedAt = member.getCreatedAt();
    }

    // Getters...
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getRoleDisplay() { return roleDisplay; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
}
```

**InviteResponse**:
```java
package com.example.todolist.dto;

import com.example.todolist.entity.InviteToken;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class InviteResponse {
    private String inviteToken;
    private String inviteUrl;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public InviteResponse(InviteToken invite, String inviteUrl) {
        this.inviteToken = invite.getToken();
        this.inviteUrl = inviteUrl;
        this.createdAt = invite.getCreatedAt();
    }

    // Getters...
    public String getInviteToken() { return inviteToken; }
    public String getInviteUrl() { return inviteUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

**JoinResponse**:
```java
package com.example.todolist.dto;

public class JoinResponse {
    private String listToken;
    private String role;
    private String message;

    public JoinResponse(String listToken, String role) {
        this.listToken = listToken;
        this.role = role;
        this.message = "成功加入清单";
    }

    // Getters...
    public String getListToken() { return listToken; }
    public String getRole() { return role; }
    public String getMessage() { return message; }
}
```

---

### 9.3 扩展现有 DTO

**ItemResponse 扩展**:

```java
// 在现有 ItemResponse 类中添加以下字段：

private String createdBy;
private String updatedBy;

public ItemResponse(TodoItem item) {
    // ... 现有字段 ...

    // 新增：创建者和更新者用户名
    if (item.getCreatedBy() != null) {
        this.createdBy = item.getCreatedBy().getUsername();
    }
    if (item.getUpdatedBy() != null) {
        this.updatedBy = item.getUpdatedBy().getUsername();
    }
}

// Getters...
public String getCreatedBy() { return createdBy; }
public String getUpdatedBy() { return updatedBy; }
```

---

## 10. 异常处理

### 10.1 新增异常类

**ForbiddenException**:
```java
package com.example.todolist.exception;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
```

---

### 10.2 全局异常处理器扩展

**在 GlobalExceptionHandler 中添加**:

```java
@ExceptionHandler(ForbiddenException.class)
public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException e) {
    ErrorResponse error = new ErrorResponse("Forbidden", e.getMessage());
    return ResponseEntity.status(403).body(error);
}

@ExceptionHandler(IllegalStateException.class)
public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
    ErrorResponse error = new ErrorResponse("Conflict", e.getMessage());
    return ResponseEntity.status(409).body(error);
}
```

---

## 11. 权限模型

### 11.1 角色定义

| 角色 | 权限 | 说明 |
|------|------|------|
| OWNER | - 查看清单<br>- 查看成员列表<br>- 编辑清单标题<br>- 添加/编辑/删除 todo<br>- **生成邀请令牌**<br>- **移除成员** | 清单创建者，可以有多个 |
| MEMBER | - 查看清单<br>- 查看成员列表<br>- 编辑清单标题<br>- 添加/编辑/删除 todo | 被邀请的协作者 |
| 非成员 | - 查看清单<br>- 添加/编辑/删除 todo | 公开访问（V2-B 不限制） |

### 11.2 权限检查逻辑

```java
/**
 * 权限检查工具类
 */
public class PermissionChecker {

    @Autowired
    private ListMemberRepository memberRepository;

    /**
     * 检查用户是否是 OWNER
     */
    public boolean isOwner(TodoList list, User user) {
        return memberRepository.isOwner(list, user);
    }

    /**
     * 检查用户是否是 MEMBER
     */
    public boolean isMember(TodoList list, User user) {
        return memberRepository.existsByListAndUser(list, user);
    }

    /**
     * 验证 OWNER 权限，否则抛出异常
     */
    public void requireOwner(TodoList list, User user) {
        if (!isOwner(list, user)) {
            throw new ForbiddenException("只有清单所有者可以执行此操作");
        }
    }

    /**
     * 验证 MEMBER 权限，否则抛出异常
     */
    public void requireMember(TodoList list, User user) {
        if (!isMember(list, user)) {
            throw new ForbiddenException("只有清单成员可以执行此操作");
        }
    }
}
```

### 11.3 权限验证点

**生成邀请令牌**:
```java
// POST /api/lists/{token}/invites
if (!permissionChecker.isOwner(list, operator)) {
    throw new ForbiddenException("只有清单所有者可以生成邀请令牌");
}
```

**移除成员**:
```java
// DELETE /api/lists/{token}/members/{userId}
if (!permissionChecker.isOwner(list, operator)) {
    throw new ForbiddenException("只有清单所有者可以移除成员");
}
```

---

## 12. 数据库迁移

### 12.1 迁移脚本（H2）

**文件路径**: `src/main/resources/db/migration/V2__add_collaboration.sql`

```sql
-- 创建 user 表
CREATE TABLE user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

-- 创建 list_member 表
CREATE TABLE list_member (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  list_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role VARCHAR(6) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  FOREIGN KEY (list_id) REFERENCES todo_list(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
  UNIQUE (list_id, user_id)
);

-- 创建 invite_token 表
CREATE TABLE invite_token (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  list_id BIGINT NOT NULL,
  token VARCHAR(12) UNIQUE NOT NULL,
  created_at TIMESTAMP NOT NULL,
  FOREIGN KEY (list_id) REFERENCES todo_list(id) ON DELETE CASCADE
);

-- 扩展 todo_item 表
ALTER TABLE todo_item
ADD COLUMN created_by_id BIGINT DEFAULT NULL,
ADD COLUMN updated_by_id BIGINT DEFAULT NULL;

ALTER TABLE todo_item
ADD FOREIGN KEY (created_by_id) REFERENCES user(id) ON DELETE SET NULL,
ADD FOREIGN KEY (updated_by_id) REFERENCES user(id) ON DELETE SET NULL;
```

---

### 12.2 Maven 依赖

**添加 Apache Commons Lang**:

```xml
<!-- 在 pom.xml 的 dependencies 中添加 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
</dependency>
```

---

## 13. 前端集成建议

### 13.1 自动创建用户

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

### 13.2 生成邀请链接

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

### 13.3 加入清单

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

---

## 14. 测试策略

### 14.1 单元测试（可选）

**UserService 测试**:
```java
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testCreateAnonymousUser() {
        User user = userService.createAnonymousUser();
        assertNotNull(user.getId());
        assertTrue(user.getUsername().startsWith("用户_"));
    }

    @Test
    void testUpdateUsername() {
        User user = userService.createAnonymousUser();
        User updated = userService.updateUser(user.getId(), "新用户名");
        assertEquals("新用户名", updated.getUsername());
    }
}
```

### 14.2 手动测试场景

**场景 1: 用户创建和修改**
1. 清除浏览器 localStorage
2. 访问应用首页
3. 验证: 自动创建用户，用户名格式为"用户_xxxxxx"
4. 修改用户名为"张三"
5. 验证: 用户名更新成功

**场景 2: 邀请成员**
1. 用户A创建清单
2. 验证: 用户A自动成为 OWNER
3. 用户A点击"邀请成员"
4. 验证: 生成邀请令牌（12位）
5. 用户B访问邀请链接
6. 验证: 用户B加入清单，角色为 MEMBER

**场景 3: 移除成员**
1. 用户A（OWNER）移除用户B（MEMBER）
2. 验证: 移除成功
3. 用户B尝试生成邀请令牌
4. 验证: 返回 403 错误

---

## 15. 向后兼容性

### 15.1 数据库兼容

- **user 表**: 新表，不影响现有数据
- **list_member 表**: 新表，不影响现有数据
- **invite_token 表**: 新表，不影响现有数据
- **todo_item 表**: 新增字段为 nullable，现有数据兼容

### 15.2 API 兼容

**新增 API**:
- 所有新 API 使用新路径，不影响现有 API

**扩展 API**:
- 请求头 `X-User-Id` 为可选，不传时保持原有行为
- 响应体新增字段，V1/V2-A 客户端可忽略

### 15.3 业务逻辑兼容

- **创建清单**: 不传 `X-User-Id` 时，清单正常创建，无所有者
- **创建 todo**: 不传 `X-User-Id` 时，`created_by_id` 为 NULL
- **查询 todo**: 不加载 `created_by` 关联，避免性能影响

---

## 16. 性能考虑

### 16.1 数据库索引

- `user.username` - 唯一索引
- `list_member(list_id, user_id)` - 唯一复合索引
- `list_member.list_id` - 单列索引（加速查询成员）
- `invite_token.token` - 唯一索引

### 16.2 N+1 查询优化

**问题**:
```java
// 查询成员列表时，每个 member 会查询一次 user
List<ListMember> members = memberRepository.findByList(list);
for (ListMember member : members) {
    member.getUser().getUsername(); // N+1 查询
}
```

**解决方案**: 使用 `@EntityGraph` 或 JOIN FETCH

```java
@EntityGraph(attributePaths = {"user"})
List<ListMember> findByList(TodoList list);
```

### 16.3 懒加载配置

- `ListMember.list` - LAZY（避免循环加载）
- `ListMember.user` - LAZY（避免加载过多用户数据）
- `InviteToken.list` - LAZY（避免加载清单详情）

---

## 17. 安全考虑

### 17.1 用户名验证

- 长度: 1-50 字符
- 字符集: 字母、数字、中文、下划线
- 防止 SQL 注入: 使用参数化查询
- 防止 XSS: 前端转义

### 17.2 邀请令牌安全

- 长度: 12 位（62^12 种组合）
- 字符集: 小写字母 + 数字
- 唯一性: 数据库唯一约束
- 可复用: V2-B 不限制使用次数

### 17.3 权限验证

- 所有敏感操作验证 `X-User-Id`
- 检查用户角色（OWNER/MEMBER）
- 防止越权访问

---

## 18. 部署检查清单

### 18.1 开发环境

- [ ] H2 数据库配置正确
- [ ] Flyway 迁移脚本可执行
- [ ] 应用启动无错误
- [ ] 所有 API 可访问

### 18.2 功能测试

- [ ] 用户创建和修改
- [ ] 清单创建时自动添加所有者
- [ ] 生成邀请令牌
- [ ] 通过邀请加入清单
- [ ] 查看成员列表
- [ ] 移除成员
- [ ] 权限验证（OWNER vs MEMBER）

### 18.3 兼容性测试

- [ ] V1 API 继续可用
- [ ] 不传 `X-User-Id` 时正常工作
- [ ] 现有清单数据不受影响
- [ ] 现有 todo 数据不受影响

---

## 19. 未来扩展（非 V2-B 范围）

### 19.1 V2-C 可能的功能

- 退出清单功能
- 转移所有权
- 邀请令牌过期机制
- 成员角色升级（MEMBER → OWNER）
- 清单公开/私有设置

### 19.2 V3 可能的功能

- JWT 认证
- 用户个人资料（头像、简介）
- 用户搜索
- 实时同步（WebSocket）
- 通知推送

---

## 20. 附录

### 20.1 术语表

| 术语 | 说明 |
|------|------|
| User | 用户，包含用户名 |
| ListMember | 清单成员关系，连接用户和清单 |
| MemberRole | 成员角色（OWNER/MEMBER） |
| InviteToken | 邀请令牌，用于加入清单 |
| OWNER | 所有者角色，可以管理成员 |
| MEMBER | 成员角色，可以编辑 todos |
| X-User-Id | HTTP 请求头，用于标识当前用户 |

### 20.2 文件清单

**新增文件**:
- `src/main/java/com/example/todolist/entity/User.java`
- `src/main/java/com/example/todolist/entity/MemberRole.java`
- `src/main/java/com/example/todolist/entity/ListMember.java`
- `src/main/java/com/example/todolist/entity/InviteToken.java`
- `src/main/java/com/example/todolist/repository/UserRepository.java`
- `src/main/java/com/example/todolist/repository/ListMemberRepository.java`
- `src/main/java/com/example/todolist/repository/InviteTokenRepository.java`
- `src/main/java/com/example/todolist/service/UserService.java`
- `src/main/java/com/example/todolist/service/MemberService.java`
- `src/main/java/com/example/todolist/service/InviteService.java`
- `src/main/java/com/example/todolist/controller/UserController.java`
- `src/main/java/com/example/todolist/controller/MemberController.java`
- `src/main/java/com/example/todolist/controller/InviteController.java`
- `src/main/java/com/example/todolist/dto/CreateUserRequest.java`
- `src/main/java/com/example/todolist/dto/UpdateUserRequest.java`
- `src/main/java/com/example/todolist/dto/JoinListRequest.java`
- `src/main/java/com/example/todolist/dto/UserResponse.java`
- `src/main/java/com/example/todolist/dto/MemberResponse.java`
- `src/main/java/com/example/todolist/dto/InviteResponse.java`
- `src/main/java/com/example/todolist/dto/JoinResponse.java`
- `src/main/java/com/example/todolist/exception/ForbiddenException.java`
- `src/main/resources/db/migration/V2__add_collaboration.sql`

**修改文件**:
- `src/main/java/com/example/todolist/entity/TodoItem.java` - 添加 created_by, updated_by
- `src/main/java/com/example/todolist/dto/ItemResponse.java` - 添加 createdBy, updatedBy
- `src/main/java/com/example/todolist/controller/ListController.java` - 自动添加所有者
- `src/main/java/com/example/todolist/controller/ItemController.java` - 记录创建者/更新者
- `src/main/java/com/example/todolist/exception/GlobalExceptionHandler.java` - 添加异常处理
- `pom.xml` - 添加 commons-lang3 依赖

### 20.3 联系方式

- **技术负责人**: TechLead
- **产品负责人**: PM
- **项目类型**: 协作基础版本
- **预期工期**: 6.5 天

---

**文档状态**: Design Complete
**技术负责人**: TechLead
**发布日期**: 2026-02-19
**文档版本**: 2.0-B

---

**文档结束**
