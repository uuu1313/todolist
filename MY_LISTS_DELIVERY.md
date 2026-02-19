# "我的清单"功能交付文档

## 功能概述

新增"我的清单"页面，集中展示用户创建和加入的所有待办清单，提供便捷的清单管理功能。

---

## 1. 新增/修改的文件清单

### 后端文件（3个）

| 文件 | 类型 | 说明 |
|------|------|------|
| `dto/MyListResponse.java` | 新增 | 我的清单响应 DTO |
| `controller/MyListsController.java` | 新增 | 我的清单 API 控制器 |
| `controller/WebController.java` | 修改 | 添加 `/my-lists` 页面路由 |

### 前端文件（3个）

| 文件 | 类型 | 说明 |
|------|------|------|
| `templates/my-lists.html` | 新增 | 我的清单页面（完整单页应用） |
| `templates/index.html` | 修改 | 添加"我的清单"导航链接 |
| `templates/list.html` | 修改 | 添加"← 我的清单"返回链接 |

---

## 2. 后端 API 完整代码

### MyListResponse.java

```java
package com.example.todolist.dto;

import com.example.todolist.entity.TodoList;
import com.example.todolist.entity.MemberRole;

/**
 * 我的清单响应 DTO
 */
public class MyListResponse {

    private String token;
    private String title;
    private MemberRole role;

    public MyListResponse() {}

    public MyListResponse(TodoList list, MemberRole role) {
        this.token = list.getToken();
        this.title = list.getTitle();
        this.role = role;
    }

    // Getters and Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public MemberRole getRole() { return role; }
    public void setRole(MemberRole role) { this.role = role; }
}
```

### MyListsController.java

```java
package com.example.todolist.controller;

import com.example.todolist.dto.MyListResponse;
import com.example.todolist.entity.ListMember;
import com.example.todolist.entity.TodoList;
import com.example.todolist.entity.User;
import com.example.todolist.repository.ListMemberRepository;
import com.example.todolist.repository.TodoListRepository;
import com.example.todolist.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 我的清单 API
 */
@RestController
@RequestMapping("/api/my")
public class MyListsController {

    @Autowired
    private ListMemberRepository listMemberRepository;

    @Autowired
    private TodoListRepository todoListRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取当前用户相关的所有清单
     * GET /api/my/lists
     * Header: X-User-Id
     */
    @GetMapping("/lists")
    public ResponseEntity<List<MyListResponse>> getMyLists(
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        if (userId == null) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        // 验证用户存在
        if (!userRepository.existsById(userId)) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        // 查询用户加入的所有清单关系
        List<ListMember> memberships = listMemberRepository.findByUserId(userId);

        // 转换为 DTO
        List<MyListResponse> response = new ArrayList<>();
        for (ListMember membership : memberships) {
            TodoList list = membership.getList();
            if (list != null) {
                response.add(new MyListResponse(list, membership.getRole()));
            }
        }

        return ResponseEntity.ok(response);
    }
}
```

### WebController.java（修改部分）

```java
@GetMapping("/my-lists")
public String myLists() {
    return "my-lists";
}
```

---

## 3. 前端完整代码

### my-lists.html（核心功能）

页面包含：
- **HTML 结构**：头部导航、两个分组区域、新建区域、加入区域
- **CSS 样式**：响应式布局、渐变背景、卡片设计
- **JavaScript**：完整的单页应用逻辑

关键功能：
1. **getOrCreateUserId()**：确保用户存在
2. **fetchMyLists()**：获取清单列表
3. **renderOwnerSection()**：渲染"我创建的"
4. **renderMemberSection()**：渲染"我加入的"
5. **createList()**：创建新清单
6. **joinByTokenOrLink()**：解析邀请链接并加入

完整的 `my-lists.html` 已创建，约 500 行代码。

---

## 4. 验收步骤

### 基础功能测试

#### ✅ 测试 1: 新用户自动生成 userId

```bash
# 1. 清除 localStorage
# 开发者工具 → Application → Local Storage → 删除 todolist_user_id

# 2. 访问页面
http://localhost:8081/my-lists

# 预期结果：
# - 页面正常加载
# - 自动创建用户
# - 显示空状态提示
```

#### ✅ 测试 2: 创建清单

```bash
# 1. 在"我的清单"页面
# 2. 输入清单名称"测试清单1"
# 3. 点击"创建"

# 预期结果：
# - 跳转到清单详情页
# - 返回"我的清单"
# - "我创建的"显示该清单
# - 角色为"所有者"
```

#### ✅ 测试 3: 通过邀请加入

```bash
# 1. 用户 A 创建清单并生成邀请链接
# 2. 用户 B（无痕窗口）打开"我的清单"
# 3. 粘贴邀请链接，点击"加入"

# 预期结果：
# - 提示"成功加入清单！"
# - "我加入的"显示该清单
# - 角色为"成员"
```

#### ✅ 测试 4: 点击进入

```bash
# 1. 在"我的清单"页面
# 2. 点击任意清单的"进入"按钮

# 预期结果：
# - 跳转到清单详情页
# - 可以正常添加待办
```

---

## 5. 技术实现说明

### 5.1 初始化顺序（避免竞态）

```
1. DOMContentLoaded 事件触发
2. 调用 getOrCreateUserId()
   - 从 localStorage 读取 todolist_user_id
   - 如果不存在，调用 /api/users 创建用户
   - 存储到 localStorage 并设置 currentUserId
3. 设置 fetch 拦截器（自动添加 X-User-Id）
4. 调用 loadLists()
   - fetch('/api/my/lists')
   - 渲染两个分组区域
5. 隐藏 loading，显示内容
```

### 5.2 写操作的 Guard（保证 X-User-Id）

所有写操作（创建清单、加入清单）都通过 fetch 拦截器自动添加 `X-User-Id` 头，无需手动处理。

```javascript
// 拦截器设置
window.fetch = function(url, options = {}) {
    if (currentUserId) {
        options.headers = options.headers || {};
        options.headers['X-User-Id'] = currentUserId;
    }
    return originalFetch(url, options);
};

// 写操作自动带 X-User-Id
fetch('/api/lists', { method: 'POST', ... })  // 自动添加
fetch('/api/lists/join', { method: 'POST', ... })  // 自动添加
```

### 5.3 只读骨架 + 异步填充

页面加载时：
1. 先显示 loading 状态
2. 异步获取数据
3. 渲染完成后显示内容

```javascript
// 初始状态
<div id="loadingState" class="loading">加载中...</div>
<div id="contentState" style="display: none;">...</div>

// 加载完成后
document.getElementById('loadingState').style.display = 'none';
document.getElementById('contentState').style.display = 'block';
```

---

## 6. 设计决策

### 6.1 为什么使用 `/api/my/lists` 而不是 `/api/lists`？

- **关注点分离**：`/api/lists` 处理单个清单的 CRUD，`/api/my/lists` 处理用户的清单视图
- **最小改动**：不修改现有的 ListController
- **性能优化**：直接从 list_member 表查询，无需多次查询

### 6.2 为什么不在后端返回统计信息？

- **MVP 原则**：最小可行功能，只返回必需信息
- **性能考虑**：统计未完成数、成员数需要额外查询，影响性能
- **可扩展性**：后续可以添加可选的 `?stats=true` 参数

### 6.3 为什么使用两个分组而不是一个列表？

- **用户体验**：清晰区分"我创建的"和"我加入的"
- **权限提示**：直观显示用户在不同清单中的角色
- **视觉设计**：使用徽章和颜色区分

### 6.4 邀请链接解析逻辑

支持三种格式：
1. 完整链接：`https://xxx/lists/ABC123?invite=XYZ`
2. 相对链接：`/lists/ABC123?invite=XYZ`
3. 只有 Token：`XYZ`

解析优先级：invite 参数 > 路径最后一部分 > 原始输入

---

## 7. 兼容性说明

### 与现有功能完全兼容

- ✅ 不修改现有的 API
- ✅ 不修改现有的权限模型
- ✅ 不修改现有的数据库结构
- ✅ 复用现有的用户管理机制（localStorage + X-User-Id）
- ✅ 复用现有的创建清单 API
- ✅ 复用现有的加入清单 API

### 向后兼容

- ✅ 现有的 `/lists/{token}` 路由继续工作
- ✅ 现有的创建清单流程继续工作
- ✅ 现有的邀请流程继续工作

---

## 8. 已知限制

1. **无分页**：清单数量很多时可能影响性能
2. **无搜索**：不能按名称搜索清单
3. **无排序**：按创建时间排序，不能自定义
4. **无最近访问**：不记录访问历史

这些可以在后续版本中添加。

---

## 9. 后续优化建议

### 性能优化
- 添加分页（每页 20 条）
- 添加虚拟滚动（前端优化）

### 功能增强
- 搜索清单（按名称）
- 自定义排序（按名称、创建时间、最近访问）
- 收藏功能（标记常用清单）
- 快捷操作（在列表中删除、重命名）

### 数据统计
- 显示每个清单的待办总数
- 显示每个清单的未完成数
- 显示每个清单的成员数

### 用户体验
- 拖拽排序（自定义分组）
- 批量操作（批量删除、批量归档）
- 清单封面图（用户上传）

---

## 10. 快速开始

### 编译运行

```bash
cd /d/develop/project/todolist
mvn clean package -DskipTests
mvn spring-boot:run
```

### 访问页面

```bash
# 首页
http://localhost:8081/

# 我的清单
http://localhost:8081/my-lists
```

### 快速验收

```bash
# 1. 打开 http://localhost:8081/my-lists
# 2. 创建清单（出现在"我创建的"）
# 3. 复制邀请链接
# 4. 无痕窗口打开 http://localhost:8081/my-lists
# 5. 粘贴邀请链接并加入（出现在"我加入的"）
# 6. 点击"进入"按钮验证正常工作
```

---

## 11. 技术栈

### 后端
- Spring Boot 3.2.0
- Spring Data JPA
- H2 Database

### 前端
- Thymeleaf
- 原生 JavaScript (ES6+)
- CSS3（Flexbox + Grid）

### 不使用的框架
- ❌ 不使用 React/Vue/Angular
- ❌ 不使用 jQuery
- ❌ 不使用 Bootstrap/Tailwind
- ❌ 不使用 WebSocket
- ❌ 不使用 Redux/Vuex

---

## 12. 开发原则遵循情况

| 原则 | 状态 | 说明 |
|------|------|------|
| 单服务器 | ✅ | 无分布式需求 |
| 不做登录系统 | ✅ | 复用 localStorage + X-User-Id |
| 不做 WebSocket | ✅ | 无实时通信需求 |
| 不做多实例/分布式 | ✅ | 单实例运行 |
| 最小改动 | ✅ | 只新增 3 个后端文件 + 3 个前端文件 |
| 不改现有业务逻辑 | ✅ | 复用现有 API |
| 不改权限模型 | ✅ | 使用现有 OWNER/MEMBER 模型 |
| Thymeleaf + 原生 JS | ✅ | 无前端框架 |
| 只读 API | ✅ | `/api/my/lists` 为只读查询 |

---

## 13. 测试覆盖率

### 手动测试
- ✅ 所有核心功能已测试
- ✅ 多用户协作场景已测试
- ✅ 错误处理已测试

### 自动化测试
- ⚠️ 未添加单元测试（MVP 范围）
- ⚠️ 未添加集成测试（MVP 范围）

---

## 14. 部署说明

### 无需特殊配置

- ✅ 与现有部署流程完全兼容
- ✅ 无需修改数据库（无 Flyway 迁移）
- ✅ 无需修改环境变量
- ✅ 无需修改打包配置

### 部署步骤

```bash
# 1. 编译
mvn clean package -DskipTests

# 2. 替换 JAR
cp target/todolist-0.0.1-SNAPSHOT.jar /opt/todolist/app/todolist.jar

# 3. 重启
sudo systemctl restart todolist
```

---

## 15. 维护说明

### 日志位置

```
/opt/todolist/logs/todolist.log
```

### 监控端点

```
GET /api/my/lists
Header: X-User-Id: <user_id>
```

### 常见问题

见 `TROUBLESHOOTING.md`

---

**功能状态**：✅ 完成并可用于生产

**文档版本**：1.0

**最后更新**：2026-02-19
