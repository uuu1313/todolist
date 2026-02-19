# P0-3: 清单编辑删除权限 - 测试验证

## 实现内容

### 后端修改 (ListController.java)
1. 添加 `UserRepository` 依赖注入
2. `updateList()` 方法添加 OWNER 权限检查
   - 从 `X-User-Id` 头获取用户ID
   - 检查用户是否是 OWNER
   - 非 OWNER 返回 403 Forbidden
3. `deleteList()` 方法添加 OWNER 权限检查
   - 同样的权限检查逻辑

### 前端修改 (list.html)
1. 添加全局变量 `myUserRole` 存储当前用户角色
2. 修改 `loadUserRole()` 函数
   - 设置 `myUserRole` 全局变量
   - 调用 `updateListActionButtons()` 更新按钮显示
3. 新增 `updateListActionButtons()` 函数
   - 仅 OWNER 显示 "编辑" 按钮
   - 仅 OWNER 显示 "删除清单" 按钮

## 测试用例

### 用例 1: OWNER 可以编辑和删除清单
**前置条件**: 以创建者身份访问清单

**步骤**:
1. 打开首页 http://localhost:8081/
2. 点击"创建新清单"
3. 访问清单页面
4. 检查页面顶部是否显示 "我的角色：所有者"
5. 检查清单标题旁是否显示 "编辑" 按钮
6. 检查是否显示 "删除清单" 按钮

**预期结果**:
- 角色显示为 "所有者"（金色背景）
- 显示 "编辑" 按钮
- 显示 "删除清单" 按钮
- 点击 "编辑" 可以修改标题
- 点击 "删除清单" 可以删除清单

---

### 用例 2: MEMBER 不能编辑和删除清单
**前置条件**: 已有两个用户（OWNER 和 MEMBER）

**步骤**:
1. 用户 A 创建清单（自动成为 OWNER）
2. 用户 A 生成邀请链接
3. 复制邀请链接到新浏览器/隐身窗口
4. 以用户 B 身份加入清单（成为 MEMBER）
5. 用户 B 访问清单页面
6. 检查页面顶部是否显示 "我的角色：成员"
7. 检查清单标题旁是否**不**显示 "编辑" 按钮
8. 检查是否**不**显示 "删除清单" 按钮

**预期结果**:
- 角色显示为 "成员"（蓝色背景）
- **不显示** "编辑" 按钮
- **不显示** "删除清单" 按钮

---

### 用例 3: VISITOR 不能编辑和删除清单
**前置条件**: 清单已存在

**步骤**:
1. 用户 A 创建清单
2. 复制清单链接（非邀请链接）
3. 在新浏览器/隐身窗口打开链接（新用户 C）
4. 用户 C 访问清单页面（自动成为 VISITOR）
5. 检查页面顶部是否显示 "我的角色：访客"
6. 检查清单标题旁是否**不**显示 "编辑" 按钮
7. 检查是否**不**显示 "删除清单" 按钮

**预期结果**:
- 角色显示为 "访客"（灰色背景）
- **不显示** "编辑" 按钮
- **不显示** "删除清单" 按钮

---

### 用例 4: 后端 API 权限验证 - 编辑清单
**测试命令**:

```bash
# 1. 创建清单并获取 token
LIST_TOKEN=$(curl -s -X POST http://localhost:8081/api/lists -H "X-User-Id: 1" | jq -r '.token')

# 2. 创建第二个用户
USER2_ID=$(curl -s -X POST http://localhost:8081/api/users | jq -r '.id')

# 3. 将用户2添加为 MEMBER
curl -X POST http://localhost:8081/api/lists/$LIST_TOKEN/members \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{"userId": '$USER2_ID', "role": "MEMBER"}'

# 4. MEMBER 尝试修改清单标题（应该失败 403）
curl -i -X PATCH http://localhost:8081/api/lists/$LIST_TOKEN \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER2_ID" \
  -d '{"title": "尝试修改"}'

# 预期: HTTP/1.1 403 Forbidden

# 5. OWNER 修改清单标题（应该成功）
curl -i -X PATCH http://localhost:8081/api/lists/$LIST_TOKEN \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{"title": "OWNER修改的标题"}'

# 预期: HTTP/1.1 200 OK
```

---

### 用例 5: 后端 API 权限验证 - 删除清单
**测试命令**:

```bash
# 1. MEMBER 尝试删除清单（应该失败 403）
curl -i -X DELETE http://localhost:8081/api/lists/$LIST_TOKEN \
  -H "X-User-Id: $USER2_ID"

# 预期: HTTP/1.1 403 Forbidden

# 2. OWNER 删除清单（应该成功）
curl -i -X DELETE http://localhost:8081/api/lists/$LIST_TOKEN \
  -H "X-User-Id: 1"

# 预期: HTTP/1.1 204 No Content
```

---

## 手动测试步骤（快速验证）

### 测试 OWNER 权限
1. 浏览器访问 http://localhost:8081/
2. 创建清单
3. 验证显示 "编辑" 和 "删除清单" 按钮
4. 点击 "编辑" 修改标题，保存
5. 验证标题已修改

### 测试 MEMBER 权限
1. 打开隐身窗口
2. 访问清单页面（使用上面的清单链接）
3. 点击 "加入清单" 输入邀请码
4. 验证角色显示为 "成员"
5. 验证 **不显示** "编辑" 和 "删除清单" 按钮

### 测试 VISITOR 权限
1. 打开另一个隐身窗口
2. 直接访问清单链接（不加入）
3. 验证角色显示为 "访客"
4. 验证 **不显示** "编辑" 和 "删除清单" 按钮

---

## 验证清单

- [ ] 后端编译成功
- [ ] 应用启动成功
- [ ] OWNER 可以看到编辑和删除按钮
- [ ] MEMBER 看不到编辑和删除按钮
- [ ] VISITOR 看不到编辑和删除按钮
- [ ] 后端 API: MEMBER 尝试编辑返回 403
- [ ] 后端 API: VISITOR 尝试编辑返回 403
- [ ] 后端 API: MEMBER 尝试删除返回 403
- [ ] 后端 API: VISITOR 尝试删除返回 403
- [ ] 前端: 角色显示正确（所有者/成员/访客）
- [ ] 前端: 按钮显示/隐藏逻辑正确

---

## 代码变更文件

1. `src/main/java/com/example/todolist/controller/ListController.java`
   - 添加 `UserRepository` 注入
   - `updateList()` 添加权限检查
   - `deleteList()` 添加权限检查

2. `src/main/resources/templates/list.html`
   - 添加 `myUserRole` 全局变量
   - 修改 `loadUserRole()` 函数
   - 新增 `updateListActionButtons()` 函数
