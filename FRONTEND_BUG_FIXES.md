# 前端 Bug 修复 - 移除按钮逻辑 + 邀请链接重复打开

## 修复时间
2026-02-19

## 问题 1：移除成员按钮显示逻辑反了

### 现象
- OWNER（所有者）页面里看不到"移除"按钮
- MEMBER（成员）页面里反而能看到"移除"按钮
- MEMBER 点击移除会失败（403），但 UI 不该显示

### 根本原因
**文件**: `list.html` 第 1320-1332 行（修复前）

```javascript
// 错误的逻辑
const currentUserId = localStorage.getItem('todolist_user_id');  // ❌ 字符串类型
list.innerHTML = members.map(m => {
    const isMe = m.userId == currentUserId;
    // ...
    // ❌ 错误：判断目标成员是不是OWNER，而不是当前用户是不是OWNER
    ${m.role === 'OWNER' && m.userId != currentUserId ?
        `<button onclick="removeMember(${m.id})">移除</button>` : ''}
});
```

**问题分析**:
1. 类型不统一：`currentUserId` 是字符串，`m.userId` 是数字
2. 逻辑错误：判断**目标成员**是不是 OWNER，而非**当前用户**是不是 OWNER
3. 结果：当前用户是 MEMBER 时，其他 OWNER 满足条件，反而显示了移除按钮

### 修复方案

**修改位置**: `list.html` `loadMembers()` 函数

```javascript
// 修复后
const currentUserId = Number(localStorage.getItem('todolist_user_id'));  // ✅ 统一为数字类型

// 计算当前用户角色
const myMember = members.find(m => m.userId == currentUserId);
const isOwner = myMember && myMember.role === 'OWNER';

list.innerHTML = members.map(m => {
    const isMe = m.userId == currentUserId;
    // ...
    // ✅ 正确：判断当前用户是不是OWNER + 不能移除自己 + 不能移除OWNER
    const canRemove = isOwner &&
                      m.userId != currentUserId &&
                      m.role !== 'OWNER';

    return `
        <li class="member-item ${isMe ? 'me' : ''}">
            ${escapeHtml(m.username)} (${roleDisplay})${marker}
            ${canRemove ?
                `<button onclick="removeMember(${m.id})">移除</button>` : ''}
        </li>
    `;
});
```

**修复要点**:
1. ✅ `Number()` 确保类型统一
2. ✅ 计算 `isOwner = myMember.role === 'OWNER'`
3. ✅ 移除按钮条件：`isOwner && target不是自己 && target不是OWNER`

---

## 问题 2：重复打开邀请链接导致卡"角色加载中"

### 现象
- 多次打开同一个邀请链接（同一浏览器/同一用户）
- 后端返回"已是列表成员"（join 幂等）
- 前端"我的角色"卡在"加载中"
- todo 列表不显示（初始化流程中断）

### 根本原因
**文件**: `list.html` 第 738-801 行（修复前）

```javascript
// 错误的join处理
if (inviteToken) {
    await ensureUser();
    const shouldJoin = confirm('是否加入此清单？');

    if (shouldJoin) {
        const response = await fetch('/api/lists/join', {...});

        if (response.ok) {
            // ❌ 问题：立即redirect，但如果返回"already member"也会redirect
            window.location.href = `/lists/${data.listToken}`;
            return;  // ❌ 中断后续初始化
        } else {
            // ❌ 问题：!response.ok时直接return，不继续初始化
            showToast('加入清单失败', 'error');
            return;  // ❌ 中断后续初始化
        }
    } else {
        // ❌ 问题：用户取消也直接return
        return;  // ❌ 中断后续初始化
    }
}

// 正常初始化流程（join分支走不到这里）
await ensureUser();
await loadMembers();
await loadUserRole();
renderInviteSection();
await loadItems();
```

**问题分析**:
1. join 成功后立即 redirect，用户可能已是成员
2. join 失败/取消时直接 return，不执行正常初始化
3. 结果：页面停留在 loading 状态，没有加载 members/items

### 修复方案

**修改位置**: `list.html` DOMContentLoaded

```javascript
// 修复后
if (inviteToken && !window.__joining) {  // ✅ 防止重复触发
    window.__joining = true;  // ✅ 设置全局flag

    await ensureUser();

    const shouldJoin = confirm('是否加入此清单？');
    if (!shouldJoin) {
        // ✅ 用户取消：移除invite参数，继续初始化
        const cleanUrl = window.location.pathname;
        window.history.replaceState({}, '', cleanUrl);
        window.__joining = false;
    } else {
        try {
            const response = await fetch('/api/lists/join', {...});

            if (response.ok) {
                // ✅ 成功：移除invite参数，继续初始化（不return）
                const cleanUrl = window.location.pathname;
                window.history.replaceState({}, '', cleanUrl);
            } else {
                // ✅ 失败：移除invite参数，仍然继续初始化
                const error = await response.json();
                showToast(error.message || '加入清单失败', 'error');
                const cleanUrl = window.location.pathname;
                window.history.replaceState({}, '', cleanUrl);
            }
        } catch (error) {
            // ✅ 异常：移除invite参数，仍然继续初始化
            console.error('Error joining list:', error);
            showToast('加入清单失败，请重试', 'error');
            const cleanUrl = window.location.pathname;
            window.history.replaceState({}, '', cleanUrl);
        }
    }
}

// ✅ join分支最终会走到这里，执行正常初始化流程
await ensureUser();
await loadMembers();
await loadUserRole();
renderInviteSection();
await loadItems();
```

**修复要点**:
1. ✅ 使用 `window.__joining` flag 防止重复触发
2. ✅ 使用 `history.replaceState()` 移除 query 参数（不刷新页面）
3. ✅ 无论成功/失败/取消，都继续执行正常初始化流程
4. ✅ 不再使用 `return` 中断初始化

---

## 代码 Diff

### 修改 1: loadMembers() - 移除按钮逻辑修复

```diff
  async function loadMembers() {
      try {
          const listToken = getTokenFromUrl();
          const response = await fetch(`/api/lists/${listToken}/members`);

          if (!response.ok) {
              console.error('Failed to load members');
              showToast('加载成员列表失败', 'error');
              throw new Error('Failed to load members');
          }

          const members = await response.json();

          const list = document.getElementById('members-list');
          if (!list) return;

-         const currentUserId = localStorage.getItem('todolist_user_id');
+         // 问题1修复: 确保类型统一
+         const currentUserId = Number(localStorage.getItem('todolist_user_id'));
+
+         // 计算当前用户角色
+         const myMember = members.find(m => m.userId == currentUserId);
+         const isOwner = myMember && myMember.role === 'OWNER';

          list.innerHTML = members.map(m => {
              const isMe = m.userId == currentUserId;
              const marker = isMe ? ' <span class="me-marker">（我）</span>' : '';
              const roleDisplay = m.role === 'OWNER' ? '所有者' : '成员';

+             // 问题1修复: 移除按钮显示条件
+             // 1) 当前用户必须是 OWNER
+             // 2) 不能移除自己
+             // 3) 不能移除 OWNER
+             const canRemove = isOwner &&
+                               m.userId != currentUserId &&
+                               m.role !== 'OWNER';

              return `
                  <li class="member-item ${isMe ? 'me' : ''}">
                      ${escapeHtml(m.username)} (${roleDisplay})${marker}
-                     ${m.role === 'OWNER' && m.userId != currentUserId ?
+                     ${canRemove ?
                          `<button onclick="removeMember(${m.id})" style="...">移除</button>` : ''}
                  </li>
              `;
          }).join('');

          return members;
      } catch (error) {
          console.error('Error loading members:', error);
          showToast('加载成员列表失败，请刷新页面重试', 'error');
          throw error;
      }
  }
```

---

### 修改 2: DOMContentLoaded - join流程修复

```diff
  document.addEventListener('DOMContentLoaded', async () => {
      const urlParams = new URLSearchParams(window.location.search);
      const inviteToken = urlParams.get('invite');

-     if (inviteToken) {
+     // 问题2修复: 使用全局flag防止重复join
+     if (inviteToken && !window.__joining) {
+         window.__joining = true;  // 设置flag防止重复触发

          await ensureUser();

          const shouldJoin = confirm('是否加入此清单？');
-         if (shouldJoin) {
+         if (!shouldJoin) {
+             // 用户取消，移除invite参数并刷新
+             const cleanUrl = window.location.pathname;
+             window.history.replaceState({}, '', cleanUrl);
+             window.__joining = false;
+             // 继续执行正常初始化流程
+         } else {
              try {
                  const response = await fetch('/api/lists/join', {
                      method: 'POST',
                      headers: {
                          'Content-Type': 'application/json',
                          'X-User-Id': currentUserId
                      },
                      body: JSON.stringify({
                          inviteToken: inviteToken
                      })
                  });

-                 if (response.ok) {
-                     const data = await response.json();
-                     // 重定向到清单页面，移除 invite 参数（干净的 URL）
-                     window.location.href = `/lists/${data.listToken}`;
-                     return;  // 重定向后不需要继续执行
-                 } else {
-                     const error = await response.json();
-                     showToast(error.message || '加入清单失败', 'error');
-                     // join 失败，跳过后续初始化
-                     return;
+                 // 问题2修复: 无论返回"joined"还是"already member"，都视为成功
+                 if (response.ok) {
+                     const data = await response.json();
+                     // 移除invite参数，使用replaceState避免页面刷新
+                     const cleanUrl = window.location.pathname;
+                     window.history.replaceState({}, '', cleanUrl);
+                     // 继续执行正常初始化流程（不return）
+                 } else {
+                     // join失败（如邀请码无效）
+                     const error = await response.json();
+                     showToast(error.message || '加入清单失败', 'error');
+                     // 移除invite参数
+                     const cleanUrl = window.location.pathname;
+                     window.history.replaceState({}, '', cleanUrl);
+                     // 仍然继续初始化（可能已是成员，可以查看）
                  }
              } catch (error) {
                  console.error('Error joining list:', error);
                  showToast('加入清单失败，请重试', 'error');
-                 // join 失败，跳过后续初始化
-                 return;
+                 // 移除invite参数
+                 const cleanUrl = window.location.pathname;
+                 window.history.replaceState({}, '', cleanUrl);
+                 // 仍然继续初始化
              }
-         } else {
-             // 用户取消加入，跳过后续初始化
-             return;
          }
      }

-     // B) 初始化时序统一：
+     // B) 统一初始化流程（join分支最终会走到这里）：
      // ensureUser() -> loadMembers() -> loadUserRole() -> renderInviteSection() -> loadItems()
      await ensureUser();

      // 设置分享链接
      const shareUrl = window.location.href;
      document.getElementById('shareUrl').value = shareUrl;

      // 先加载成员和角色（决定权限和 UI）
      await loadMembers();
      await loadUserRole();

      // 根据角色渲染邀请区域和其他 UI
      renderInviteSection();

      // 最后加载待办事项（权限已确定）
      await loadItems();
  });
```

---

## QA 验收标准（两窗口测试）

### 问题 1 验收：移除成员按钮

#### 测试 1.1: OWNER 移除 MEMBER
**前置条件**:
- 主浏览器创建清单（OWNER）
- 无痕浏览器加入清单（MEMBER）

**步骤**:
1. 主浏览器打开清单页面
2. 查看成员列表

**预期结果**:
- ✅ OWNER 能看到 MEMBER 的"移除"按钮
- ✅ 点击"移除"后 MEMBER 被移除
- ✅ OWNER 看不到自己的"移除"按钮
- ✅ 如果有多个 OWNER，看不到其他 OWNER 的"移除"按钮

---

#### 测试 1.2: MEMBER 不能移除
**前置条件**:
- 主浏览器创建清单（OWNER）
- 无痕浏览器加入清单（MEMBER）

**步骤**:
1. 无痕浏览器打开清单页面
2. 查看成员列表

**预期结果**:
- ✅ MEMBER 完全看不到任何"移除"按钮（包括自己、OWNER、其他 MEMBER）

---

### 问题 2 验收：重复打开邀请链接

#### 测试 2.1: 第一次打开邀请链接
**前置条件**:
- 主浏览器创建清单并生成邀请链接

**步骤**:
1. 无痕浏览器打开邀请链接（`/lists/xxx?invite=yyy`）
2. 点击"确定"加入

**预期结果**:
- ✅ 提示"加入成功"
- ✅ URL 变为 `/lists/xxx`（无 `?invite=yyy`）
- ✅ 角色显示为"成员"
- ✅ 待办列表正常显示
- ✅ invite-section 隐藏

---

#### 测试 2.2: 重复打开同一邀请链接
**前置条件**:
- 已通过测试 2.1 加入清单

**步骤**:
1. 再次打开同一邀请链接（`/lists/xxx?invite=yyy`）
2. 点击"确定"（可能提示"已是成员"）

**预期结果**:
- ✅ 不卡"加载中"
- ✅ 角色正常显示（仍是"成员"）
- ✅ 待办列表正常显示
- ✅ URL 变为 `/lists/xxx`
- ✅ invite-section 隐藏

---

#### 测试 2.3: 多次重复打开
**步骤**:
1. 重复打开邀请链接 5 次

**预期结果**:
- ✅ 每次都能正常显示
- ✅ 不卡 loading
- ✅ 角色和列表始终正常

---

#### 测试 2.4: 取消加入
**步骤**:
1. 打开邀请链接
2. 点击"取消"

**预期结果**:
- ✅ URL 变为 `/lists/xxx`
- ✅ 角色显示为"访客"
- ✅ 待办列表正常显示（只读）

---

#### 测试 2.5: 无效邀请码
**步骤**:
1. 打开无效邀请链接（`/lists/xxx?invite=invalid`）
2. 点击"确定"

**预期结果**:
- ✅ 显示错误提示
- ✅ URL 变为 `/lists/xxx`
- ✅ 不卡 loading
- ✅ 如果已是成员，正常显示内容
- ✅ 如果不是成员，显示为"访客"

---

## 修改的文件

- `src/main/resources/templates/list.html`
  - `loadMembers()` 函数（第 1302-1339 行）
  - `DOMContentLoaded` 事件监听器（第 738-801 行）

---

## 未修改的部分

- ❌ 后端接口（未改动）
- ❌ 其他前端逻辑（未改动）
- ❌ CSS 样式（未改动）

---

## 修复验证命令

```bash
# 停止旧进程
netstat -ano | grep :8081 | grep LISTENING
taskkill //F //PID <pid>

# 编译
cd /d/develop/project/todolist
mvn clean compile

# 启动
mvn spring-boot:run

# 测试
curl http://localhost:8081/
```
