# 共享待办清单 V1 - 产品需求文档 (PRD)

**文档版本**: 1.0
**创建日期**: 2026-02-18
**项目类型**: 学习项目 (MVP)
**产品负责人**: PM

---

## 1. 产品概述

### 1.1 项目背景
"共享待办清单"是一个轻量级的协作工具,旨在解决多人简单共享任务的场景。用户可以创建待办清单,通过短链接与他人共享,多人可以同时查看和编辑同一清单。

### 1.2 核心价值
- **零门槛使用**: 无需注册登录,打开即用
- **简单分享**: 通过短链接快速共享清单
- **实时同步**: 刷新页面即可获取最新状态
- **多人协作**: 多人可同时编辑同一清单

### 1.3 目标用户
- 小团队成员进行简单任务分配
- 朋友/家人之间的共享购物清单、事件提醒
- 临时性协作场景(如活动筹备)

---

## 2. MVP 功能范围

### 2.1 功能总览

| 功能模块 | 优先级 | 说明 |
|---------|--------|------|
| 清单创建与分享 | P0 | 核心功能,必须实现 |
| 待办事项管理 | P0 | 核心功能,必须实现 |
| 清单访问与查看 | P0 | 核心功能,必须实现 |
| 数据持久化 | P0 | 核心功能,必须实现 |
| 基础错误处理 | P1 | 重要功能,应该实现 |

### 2.2 详细功能列表

#### 2.2.1 清单创建与分享 (P0)

**用户故事**: 作为一名用户,我想创建一个新的待办清单并获得分享链接,以便与他人协作。

**功能点**:
1. **创建清单**
   - 访问首页,显示"创建新清单"按钮
   - 点击后自动创建新清单
   - 清单自动分配唯一 token (8位随机字符串)
   - 创建成功后跳转到清单详情页

2. **生成分享链接**
   - 清单详情页显示分享链接
   - 链接格式: `http://localhost:8080/list/{token}`
   - 提供"复制链接"按钮(使用 Clipboard API)

3. **Token 生成规则**
   - 长度: 8个字符
   - 字符集: 大小写字母 + 数字 (A-Z, a-z, 0-9)
   - 唯一性: 数据库级别保证不重复
   - 示例: `aB3xK9mP`

**验收标准**:
- [ ] 可以成功创建清单
- [ ] Token 唯一性校验有效
- [ ] 分享链接可正常访问
- [ ] 复制链接功能可用

---

#### 2.2.2 待办事项管理 (P0)

**用户故事**: 作为一名用户,我想添加、编辑、删除和标记完成待办事项,以便管理我的任务。

**功能点**:

1. **添加待办事项**
   - 输入框输入任务标题
   - 按回车或点击"添加"按钮创建
   - 新事项默认状态为"未完成"
   - 标题最大长度: 200 字符
   - 空标题不可提交

2. **标记完成/未完成**
   - 点击复选框切换状态
   - 视觉反馈: 已完成任务显示删除线

3. **编辑待办事项**
   - 点击"编辑"按钮进入编辑模式
   - 显示输入框,可修改标题
   - 保存后更新数据库

4. **删除待办事项**
   - 点击"删除"按钮
   - 弹出确认对话框(使用原生 confirm)
   - 确认后从数据库删除

5. **批量操作(可选,时间允许)**
   - 清除已完成项
   - 全部标记为完成

**数据字段**:
```
TodoItem {
  id: Long
  title: String (200字符限制)
  completed: Boolean
  listId: Long (外键)
  createdAt: LocalDateTime
  updatedAt: LocalDateTime
}
```

**验收标准**:
- [ ] 可以添加待办事项
- [ ] 可以切换完成状态
- [ ] 可以编辑待办事项
- [ ] 可以删除待办事项
- [ ] 标题为空时无法提交
- [ ] 超长标题被截断或拒绝
- [ ] 刷新页面后数据不丢失

---

#### 2.2.3 清单访问与查看 (P0)

**用户故事**: 作为一名用户,我想通过分享链接访问清单并查看所有待办事项。

**功能点**:

1. **访问清单**
   - 通过 token 访问清单: `/list/{token}`
   - 如果 token 不存在,返回 404 页面

2. **清单详情页**
   - 显示所有待办事项
   - 显示清单统计(总数、已完成数)
   - 显示分享链接

3. **404 页面**
   - Token 无效时显示友好提示
   - 提供"创建新清单"按钮

**API 端点**:
```
GET  /api/lists/{token}        - 获取清单详情
GET  /api/lists/{token}/items  - 获取清单的所有事项
```

**验收标准**:
- [ ] 有效 token 可正常访问清单
- [ ] 无效 token 显示 404 页面
- [ ] 清单详情正确显示
- [ ] 统计数据准确

---

#### 2.2.4 数据持久化 (P0)

**用户故事**: 作为系统,我需要将数据持久化存储,以便刷新页面后数据不丢失。

**数据库设计**:

**表 1: todo_list (清单表)**
```sql
CREATE TABLE todo_list (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  token VARCHAR(8) NOT NULL UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_token (token)
);
```

**表 2: todo_item (待办事项表)**
```sql
CREATE TABLE todo_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  list_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  completed BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (list_id) REFERENCES todo_list(id) ON DELETE CASCADE,
  INDEX idx_list_id (list_id)
);
```

**技术实现**:
- 使用 Spring Boot JPA (Hibernate)
- 数据库: H2 (开发环境) 或 MySQL (生产环境)
- 级联删除: 删除清单时自动删除其所有事项

**验收标准**:
- [ ] 数据库表结构正确创建
- [ ] 清单和事项可正确保存
- [ ] 刷新页面后数据保留
- [ ] 删除清单时关联事项被删除

---

#### 2.2.5 基础错误处理 (P1)

**功能点**:
1. **网络错误提示**
   - API 请求失败时显示错误提示
   - 使用简单的 alert 或页面内提示

2. **输入验证**
   - 前端验证: 空标题、超长标题
   - 后端验证: 标题长度、必填字段

3. **异常处理**
   - Token 不存在返回 404
   - 数据库异常返回 500
   - 参数错误返回 400

**验收标准**:
- [ ] 网络错误有用户可见的提示
- [ ] 前端验证正常工作
- [ ] 后端返回正确的 HTTP 状态码

---

### 2.3 核心流程

#### 流程 1: 创建清单
```
用户访问首页
  ↓
点击"创建新清单"
  ↓
后端创建清单并生成 token
  ↓
重定向到 /list/{token}
  ↓
显示清单详情页(空清单)
```

#### 流程 2: 添加待办事项
```
用户在清单详情页输入标题
  ↓
前端验证(非空、长度)
  ↓
POST /api/lists/{token}/items
  ↓
后端保存到数据库
  ↓
返回新创建的 item JSON
  ↓
前端更新 DOM,插入新事项
```

#### 流程 3: 分享清单
```
用户点击"复制链接"
  ↓
复制当前 URL 到剪贴板
  ↓
显示"已复制"提示
  ↓
用户将链接发送给他人
```

#### 流程 4: 访问分享的清单
```
用户打开分享链接
  ↓
GET /api/lists/{token}
  ↓
后端根据 token 查询清单
  ↓
返回清单数据和事项列表
  ↓
前端渲染清单详情页
```

#### 流程 5: 切换完成状态
```
用户点击复选框
  ↓
PATCH /api/items/{id} {completed: true}
  ↓
后端更新数据库
  ↓
返回 200 OK
  ↓
前端更新样式(添加删除线)
```

---

## 3. 非目标 (V1 禁止实现)

### 3.1 功能边界

| 功能 | 说明 | 何时考虑 |
|------|------|----------|
| 用户注册/登录 | 无需账号系统 | V2 或后续 |
| 权限控制 | 所有人可编辑所有清单 | V2 或后续 |
| 实时同步 | 无 WebSocket,需刷新更新 | V2 或后续 |
| 清单列表页 | 无"我的清单"功能 | V2 或后续 |
| 清单标题/描述编辑 | 清单无标题,仅通过 token 识别 | V2 或后续 |
| 事项优先级/截止日期 | 仅支持标题和完成状态 | V2 或后续 |
| 事项分类/标签 | 不支持 | V2 或后续 |
| 拖拽排序 | 不支持 | V2 或后续 |
| 操作历史/撤销 | 不支持 | V2 或后续 |
| 搜索/过滤 | 不支持 | V2 或后续 |

### 3.2 技术约束 (V1 禁止)

- ❌ 不使用前端框架 (React/Vue/Angular)
- ❌ 不使用 WebSocket 或 SSE
- ❌ 不使用 Redis 等缓存
- ❌ 不使用消息队列
- ❌ 不使用微服务架构
- ❌ 不实现复杂的权限系统
- ❌ 不实现 OAuth 或第三方登录
- ❌ 不使用复杂的前端状态管理
- ❌ 不实现自动化测试(可手动测试)

### 3.3 明确不做

- 不做清单的"归档"或"删除"功能 (可手动清空事项)
- 不做"最近访问"记录
- 不做 QR 码生成
- 不做导出功能 (PDF/Excel)
- 不做深色模式
- 不做移动端适配优化 (响应式即可)
- 不做 SEO 优化
- 不做日志分析
- 不做监控告警

---

## 4. API 设计

### 4.1 REST API 端点

**清单相关**
```
POST   /api/lists                    - 创建新清单
GET    /api/lists/{token}            - 获取清单详情
DELETE /api/lists/{token}            - 删除清单(可选)
```

**待办事项相关**
```
GET    /api/lists/{token}/items      - 获取清单的所有事项
POST   /api/lists/{token}/items      - 添加新事项
PATCH  /api/items/{id}               - 更新事项(完成状态、标题)
DELETE /api/items/{id}               - 删除事项
```

### 4.2 请求/响应示例

**创建清单**
```
POST /api/lists

Response 201:
{
  "id": 1,
  "token": "aB3xK9mP",
  "createdAt": "2026-02-18T10:00:00"
}
```

**获取清单详情**
```
GET /api/lists/aB3xK9mP

Response 200:
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
    }
  ]
}
```

**添加事项**
```
POST /api/lists/aB3xK9mP/items
Content-Type: application/json

Request:
{
  "title": "买牛奶"
}

Response 201:
{
  "id": 1,
  "title": "买牛奶",
  "completed": false,
  "createdAt": "2026-02-18T10:01:00"
}
```

**更新事项**
```
PATCH /api/items/1
Content-Type: application/json

Request:
{
  "completed": true
}

Response 200:
{
  "id": 1,
  "title": "买牛奶",
  "completed": true,
  "updatedAt": "2026-02-18T10:05:00"
}
```

### 4.3 HTTP 状态码

| 状态码 | 场景 |
|--------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 400 | 参数错误 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

---

## 5. 前端实现

### 5.1 技术栈
- **模板引擎**: Thymeleaf
- **HTTP 请求**: Fetch API
- **DOM 操作**: 原生 JavaScript (Vanilla JS)
- **样式**: 简单 CSS (可使用 Bootstrap CDN)

### 5.2 页面结构

**页面 1: 首页**
- URL: `/`
- 内容:
  - 标题: "共享待办清单"
  - 按钮: "创建新清单"

**页面 2: 清单详情页**
- URL: `/list/{token}`
- 内容:
  - 分享链接区域
  - 统计信息 (总数、已完成数)
  - 添加事项输入框
  - 事项列表
  - 每个事项: 复选框、标题、编辑按钮、删除按钮

### 5.3 交互设计

**添加事项**
```javascript
// 伪代码
async function addItem() {
  const title = input.value.trim();
  if (!title) return alert('标题不能为空');

  const response = await fetch(`/api/lists/${token}/items`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title })
  });

  if (response.ok) {
    const item = await response.json();
    renderItem(item);
    input.value = '';
    updateStats();
  }
}
```

**切换完成状态**
```javascript
async function toggleComplete(id, completed) {
  const response = await fetch(`/api/items/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ completed })
  });

  if (response.ok) {
    updateItemStyle(id, completed);
    updateStats();
  }
}
```

### 5.4 样式要求
- 简洁清爽的设计
- 已完成任务使用删除线
- 鼠标悬停时显示操作按钮
- 响应式布局(简单即可)

---

## 6. 验收标准 (Definition of Done)

### 6.1 功能验收清单

#### 必须完成 (P0)
- [ ] 可以创建新清单
- [ ] Token 唯一且可访问
- [ ] 可以添加待办事项
- [ ] 可以标记事项完成/未完成
- [ ] 可以编辑事项标题
- [ ] 可以删除事项
- [ ] 数据持久化正常工作
- [ ] 刷新页面后数据保留
- [ ] 可以复制分享链接
- [ ] 通过分享链接可访问清单
- [ ] 无效 token 显示 404

#### 应该完成 (P1)
- [ ] 前端输入验证(空标题、超长)
- [ ] 后端参数验证
- [ ] 网络错误提示
- [ ] 删除确认对话框
- [ ] 统计信息正确显示

### 6.2 可用性标准

| 指标 | 标准 |
|------|------|
| 页面加载时间 | < 1 秒(本地环境) |
| API 响应时间 | < 200ms (本地环境) |
| 并发支持 | 10 人同时操作不崩溃 |
| 浏览器兼容 | Chrome/Firefox/Edge 最新版 |
| 移动端可用 | 基本功能在手机浏览器可正常使用 |

### 6.3 代码质量

- [ ] 代码可编译运行
- [ ] 无明显 bug
- [ ] API 设计符合 REST 规范
- [ ] 数据库表结构合理
- [ ] 前端代码有基本注释
- [ ] 后端代码有基本注释

### 6.4 测试场景

**场景 1: 创建清单并添加事项**
1. 访问首页
2. 点击"创建新清单"
3. 复制分享链接
4. 添加 3 个事项
5. 标记 1 个为完成
6. 刷新页面
7. 验证: 所有数据保留

**场景 2: 分享清单**
1. 创建清单并添加事项
2. 复制链接
3. 在无痕模式下打开链接
4. 验证: 可以看到所有事项
5. 添加新事项
6. 返回原页面刷新
7. 验证: 可以看到新事项

**场景 3: 编辑和删除**
1. 创建清单并添加事项
2. 编辑事项标题
3. 切换完成状态
4. 删除事项
5. 验证: 所有操作生效

**场景 4: 错误处理**
1. 访问不存在的 token
2. 验证: 显示 404 页面
3. 添加空标题事项
4. 验证: 提示错误或无法提交

---

## 7. 技术实现建议

### 7.1 后端技术栈
- **Java 17**
- **Spring Boot 4.0.2**
- **Spring Data JPA**
- **H2 Database** (开发) / **MySQL** (生产)
- **Thymeleaf**

### 7.2 项目结构
```
src/main/java/com/example/todolist/
├── TodolistApplication.java
├── controller/
│   ├── ListController.java
│   └── ItemController.java
├── service/
│   ├── ListService.java
│   └── ItemService.java
├── repository/
│   ├── TodoListRepository.java
│   └── TodoItemRepository.java
├── entity/
│   ├── TodoList.java
│   └── TodoItem.java
└── dto/
    ├── CreateListRequest.java
    ├── CreateItemRequest.java
    └── UpdateItemRequest.java

src/main/resources/
├── application.properties
├── templates/
│   ├── index.html
│   └── list.html
└── static/
    └── css/
        └── style.css
```

### 7.3 数据库配置

**application.properties**
```properties
# H2 数据库(开发环境)
spring.datasource.url=jdbc:h2:mem:todolist
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA 配置
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Thymeleaf
spring.thymeleaf.cache=false
```

### 7.4 实体类示例

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
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "list", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TodoItem> items = new ArrayList<>();
}

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
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

---

## 8. 用户场景示例

### 场景 1: 家庭购物清单

**背景**: 小明家庭需要共同管理购物清单

**流程**:
1. 小明访问网站,创建新清单
2. 添加"牛奶"、"鸡蛋"、"面包"
3. 复制分享链接发送到家庭微信群
4. 妈妈点击链接,看到清单
5. 妈妈添加"洗洁精"
6. 小明刷新页面,看到新添加的"洗洁精"
7. 爸爸访问链接,标记"牛奶"为已购买
8. 小明和妈妈刷新页面,看到"牛奶"已完成

### 场景 2: 活动筹备任务

**背景**: 团队筹备周末团建活动

**流程**:
1. 组织者创建清单
2. 添加任务:"订场地"、"买饮料"、"通知参与人"、"准备游戏"
3. 分享链接到团队群
4. 成员 A 主动认领"订场地",完成后标记
5. 成员 B 认领"买饮料"
6. 所有成员刷新页面,看到最新进展
7. 组织者一目了然,跟进未完成任务

---

## 9. 后续版本规划 (V2+)

### V2 可能功能
- 添加清单标题和描述
- 用户系统和权限控制
- 清单列表页(我的创建、我的参与)
- WebSocket 实时同步
- 移动端优化
- 事项优先级和截止日期
- 操作历史和撤销

### V3 可能功能
- 清单模板
- 子任务
- 文件附件
- 评论功能
- 提醒通知
- 数据导出
- 深色模式

---

## 10. 附录

### 10.1 术语表

| 术语 | 说明 |
|------|------|
| Token | 8 位随机字符串,用于唯一标识清单 |
| 清单 | TodoList,包含多个待办事项 |
| 事项 | TodoItem,清单中的单个任务 |
| Thymeleaf | 服务端 Java 模板引擎 |

### 10.2 参考资源
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Thymeleaf 文档](https://www.thymeleaf.org/)
- [Fetch API 文档](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API)

### 10.3 联系方式
- **产品负责人**: PM
- **技术负责人**: 后端工程师
- **项目类型**: 学习项目
- **预期工期**: 3-5 天

---

**文档结束**
