# 前端状态机修复 - 协作初始化流程

## 修复时间

2026-02-19

## 问题描述

用户报告了 3 个严重的协作初始化问题：

1. **现象 1**：直接复制清单分享链接，在无痕浏览器打开后，当前用户被判定为 OWNER（不合理，应为 VISITOR）
2. **现象 2**：使用邀请链接在无痕浏览器加入后，提示"成员已在列表中"，但"当前角色"一直加载中，待办列表也不显示
3. **现象 3**：主浏览器原本为 OWNER，但随后邀请按钮（invite-section）消失，即使新建清单也不显示

## 根本原因

### 1. HTML 写死隐藏
第 665 行：`<section id="invite-section" style="display:none;">` - 邀请区域默认隐藏，即使计算后也不显示

### 2. 初始化时序错误
```javascript
// 错误的时序
loadItems();          // 先加载待办
await loadMembers();  // 后加载成员
await loadUserRole(); // 后加载角色
checkOwnerAndShowInvite(); // 最后检查 owner（重复请求 members）
```

### 3. checkOwnerAndShowInvite() 逻辑错误
- 第 1395 行：`m.id === parseInt(currentUserId)` - 应该是 `m.userId`，不是 `m.id`
- 缺少 else 分支隐藏邀请区
- 重复请求 `/api/lists/{token}/members`

### 4. join 流程不完整
- join 请求没有显式传递 `X-User-Id`
- join 失败后继续执行初始化（应该停止）
- join 成功后重定向，但 URL 仍可能带 `?invite=xxx`

### 5. 错误处理静默
- `loadMembers()` 失败时只 `console.error`，用户看不到
- `loadUserRole()` 失败时只 `console.error`，角色一直显示"加载中"

## 修复方案

### A) 邀请区域显示控制
**修改文件**: `list.html`

**变更**:
```html
<!-- 修复前 -->
<section id="invite-section" style="display:none;">

<!-- 修复后 -->
<section id="invite-section">
```

**新增函数** `renderInviteSection()`:
```javascript
function renderInviteSection() {
    const inviteSection = document.getElementById('invite-section');
    if (!inviteSection) return;

    // 根据全局变量 myUserRole 决定是否显示邀请区域
    if (myUserRole === 'OWNER') {
        inviteSection.style.display = 'block';
    } else {
        inviteSection.style.display = 'none';
    }
}
```

**删除**: `checkOwnerAndShowInvite()` 函数（重复请求 + 逻辑错误）

---

### B) 初始化时序统一
**修改文件**: `list.html` DOMContentLoaded

**修复前的时序**:
```javascript
await ensureUser();
if (inviteToken) { /* join 处理 */ return; }
loadItems();              // ❌ 时序错误
await loadMembers();      // ❌ 在 loadItems 后
await loadUserRole();     // ❌ 在 loadMembers 后
checkOwnerAndShowInvite();// ❌ 重复请求 members
```

**修复后的时序**:
```javascript
// B) 初始化时序统一：
// ensureUser() -> loadMembers() -> loadUserRole() -> renderInviteSection() -> loadItems()

if (inviteToken) {
    // C) join 流程修复：先 ensureUser，再 join，再重定向到纯净 URL
    await ensureUser();
    // ... join 处理 ...
    return;  // 重定向后不继续执行
}

await ensureUser();
// 设置分享链接
await loadMembers();    // ✅ 先加载成员
await loadUserRole();   // ✅ 再加载角色
renderInviteSection();  // ✅ 根据角色渲染 UI
await loadItems();      // ✅ 最后加载待办（权限已确定）
```

---

### C) join 流程修复
**修改文件**: `list.html` DOMContentLoaded

**关键修复**:
```javascript
if (inviteToken) {
    await ensureUser();  // ✅ 先确保用户存在

    const shouldJoin = confirm('是否加入此清单？');
    if (shouldJoin) {
        const response = await fetch('/api/lists/join', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-User-Id': currentUserId  // ✅ 显式传递 user id
            },
            body: JSON.stringify({ inviteToken: inviteToken })
        });

        if (response.ok) {
            const data = await response.json();
            // ✅ 重定向到纯净 URL（移除 ?invite=xxx）
            window.location.href = `/lists/${data.listToken}`;
            return;
        } else {
            showToast(error.message || '加入清单失败', 'error');
            return;  // ✅ join 失败，停止初始化
        }
    } else {
        return;  // ✅ 用户取消，停止初始化
    }
}
```

---

### D) 访客权限
**修改文件**: `list.html` loadUserRole()

**逻辑**:
```javascript
// D) 访客权限：未加入成员（list_member 中无当前 userId）视为 VISITOR
const myMember = members.find(m => m.userId == currentUserId);

myUserRole = 'VISITOR';  // ✅ 默认为访客

if (myMember) {
    if (myMember.role === 'OWNER') {
        myUserRole = 'OWNER';
    } else if (myMember.role === 'MEMBER') {
        myUserRole = 'MEMBER';
    }
}
```

**后端已有检查**:
- `ItemController.addItem()` - VISITOR 返回 403
- `ItemManagementController.updateItem()` - VISITOR 返回 403
- `ItemManagementController.deleteItem()` - VISITOR 返回 403

---

### E) 错误处理增强
**修改文件**: `list.html` loadMembers() / loadUserRole()

**loadMembers() 修复**:
```javascript
if (!response.ok) {
    console.error('Failed to load members');
    showToast('加载成员列表失败', 'error');  // ✅ 显示错误给用户
    throw new Error('Failed to load members');  // ✅ 抛出错误停止后续流程
}
```

**loadUserRole() 修复**:
```javascript
if (!response.ok) {
    console.error('Failed to load members for role');
    showToast('加载用户角色失败', 'error');  // ✅ 显示错误给用户
    throw new Error('Failed to load members for role');  // ✅ 抛出错误
}

// catch 块
catch (error) {
    console.error('Error loading user role:', error);
    roleElement.textContent = '未知';
    showToast('加载用户角色失败，请刷新页面重试', 'error');  // ✅ 提示用户刷新
    throw error;  // ✅ 重新抛出错误
}
```

**loadItems() 增强**:
```javascript
else if (response.status === 403) {
    // D) 访客权限：VISITOR 尝试写操作时后端会返回 403
    showToast('您没有权限访问此清单', 'error');
}
```

---

### F) Bug 修复
**修改文件**: `list.html` loadMembers()

**Bug**: 成员列表渲染时移除按钮的判断逻辑错误
```javascript
// 修复前
${m.role === 'OWNER' && m.id !== parseInt(currentUserId) ?  // ❌ m.id 是成员关系 id，不是用户 id

// 修复后
${m.role === 'OWNER' && m.userId != currentUserId ?  // ✅ 正确比较用户 id
```

---

## 验收标准（两窗口测试）

### 测试 1: OWNER 创建清单
**主浏览器**:
1. 打开 http://localhost:8081/
2. 点击"创建新清单"
3. 验证：
   - ✅ 角色显示为"所有者"（金色）
   - ✅ members 显示 1 个 OWNER（我）
   - ✅ invite-section 可见
   - ✅ 待办列表正常加载

### 测试 2: VISITOR 访问分享链接
**无痕浏览器**:
1. 复制主浏览器的清单链接（如 http://localhost:8081/lists/abc123）
2. 在无痕窗口打开
3. 验证：
   - ✅ 角色显示为"访客"（灰色）
   - ✅ members 显示 1 个 OWNER（不是"我"）
   - ✅ invite-section 不可见
   - ✅ "编辑"和"删除清单"按钮不显示
   - ✅ 待办列表正常加载（只读）

### 测试 3: MEMBER 加入清单
**无痕浏览器**:
1. 主浏览器生成邀请链接
2. 无痕窗口打开邀请链接（http://localhost:8081/lists/abc123?invite=xyz789）
3. 点击"确定"加入
4. 验证：
   - ✅ 重定向到纯净 URL（无 ?invite=xxx）
   - ✅ 角色显示为"成员"（蓝色）
   - ✅ members 显示 2 人：OWNER + MEMBER（我）
   - ✅ invite-section 不可见
   - ✅ 待办列表正常加载（可写）

### 测试 4: 刷新稳定性
**主浏览器和无痕浏览器**:
1. 刷新页面
2. 验证：
   - ✅ 角色保持不变
   - ✅ invite-section 显示状态保持不变
   - ✅ 待办列表正常加载
   - ✅ 不再卡"加载中"

### 测试 5: 权限隔离
**无痕浏览器（MEMBER 或 VISITOR）**:
1. 尝试点击"编辑"按钮 - ✅ 按钮不存在
2. 尝试添加 todo - ✅ 前端禁用或后端返回 403
3. 尝试完成/编辑/删除 todo - ✅ 前端禁用或后端返回 403

---

## 代码变更汇总

### 修改的文件
- `src/main/resources/templates/list.html`

### 修改的代码块
1. **第 665 行**：移除 `invite-section` 的 `style="display:none;"`
2. **第 738-790 行**：重写 DOMContentLoaded 初始化逻辑
3. **第 1284-1317 行**：重写 `loadMembers()` 函数
4. **第 1320-1367 行**：重写 `loadUserRole()` 函数
5. **第 1385-1403 行**：删除 `checkOwnerAndShowInvite()`，替换为 `renderInviteSection()`
6. **第 793-824 行**：增强 `loadItems()` 错误处理

### 未修改的部分
- ❌ 后端逻辑（未改动）
- ❌ CSS 样式（未改动）
- ❌ 其他 HTML 结构（未改动）

---

## 后续建议

虽然本次修复解决了协作初始化的核心问题，但以下功能可作为后续优化：

1. **P1: 轮询同步机制** - 5-10 秒自动刷新 members/items
2. **P2: JPA 乐观锁** - 防止并发编辑冲突
3. **错误恢复** - members 加载失败时提供重试按钮
4. **加载状态** - 添加骨架屏或 loading 动画
5. **离线检测** - 网络断开时提示用户
