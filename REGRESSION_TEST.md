# 共享待办清单 V1 - 回归测试清单

**文档版本**: 1.0
**创建日期**: 2026-02-18
**测试负责人**: QA
**项目位置**: `/d/develop/project/todolist`

---

## 测试范围说明

本文档用于验证 V1 功能在代码加固后仍然正常工作，仅覆盖 PRD 中定义的 V1 核心功能，不包含超出范围的功能测试。

### 测试环境
- **Base URL**: `http://localhost:8080`
- **数据库**: H2 (开发环境) 或 MySQL (生产环境)
- **浏览器**: Chrome/Firefox/Edge 最新版

### 测试优先级定义
- **P0**: 核心功能，必须通过
- **P1**: 重要功能，应该通过

---

## 1. 核心功能测试用例 (P0)

### TC-01: 创建待办清单
**优先级**: P0
**前置条件**: 应用已启动

**测试步骤**:
1. 访问首页 `http://localhost:8080/`
2. 点击"创建新清单"按钮
3. 观察页面跳转和响应

**预期结果**:
- 页面重定向到清单详情页，URL 格式为 `/list/{token}`
- 响应状态码为 201 Created
- 返回 JSON 包含: `id`, `token`, `createdAt`, `items=[]`
- token 长度为 8 字符，仅包含大小写字母和数字
- 清单详情页正常显示

**验证点**:
- [ ] 创建成功
- [ ] Token 格式正确
- [ ] 页面正确跳转

---

### TC-02: 添加待办事项
**优先级**: P0
**前置条件**: 已有有效的清单 token

**测试步骤**:
1. 访问清单详情页 `/list/{token}`
2. 在输入框中输入标题"学习 Spring Boot"
3. 点击"添加"按钮或按回车键

**预期结果**:
- 响应状态码为 201 Created
- 返回 JSON 包含: `id`, `title="学习 Spring Boot"`, `completed=false`, `createdAt`, `updatedAt`
- 待办事项立即显示在列表中
- 新事项默认为"未完成"状态
- 输入框清空

**验证点**:
- [ ] 事项添加成功
- [ ] 默认状态为未完成
- [ ] UI 实时更新

---

### TC-03: 获取待办清单详情
**优先级**: P0
**前置条件**: 已创建清单并添加了多个事项

**测试步骤**:
1. 访问 `GET /api/lists/{token}`
2. 观察响应数据

**预期结果**:
- 响应状态码为 200 OK
- 返回清单完整信息，包含所有待办事项
- 每个事项包含完整字段: `id`, `title`, `completed`, `createdAt`, `updatedAt`
- 统计信息正确（总数、已完成数）

**验证点**:
- [ ] 响应包含所有数据
- [ ] 数据结构符合 API_CONTRACT
- [ ] 统计信息准确

---

### TC-04: 标记待办事项完成/未完成
**优先级**: P0
**前置条件**: 已有未完成的待办事项

**测试步骤**:
1. 点击事项的复选框
2. 观察状态变化
3. 再次点击复选框取消完成

**预期结果**:
- 响应状态码为 200 OK
- `completed` 字段正确切换（true/false）
- `updatedAt` 字段更新
- UI 显示删除线效果（已完成）
- 再次点击后删除线消失

**验证点**:
- [ ] 状态正确切换
- [ ] UI 视觉反馈正确
- [ ] updatedAt 时间戳更新

---

### TC-05: 编辑待办事项标题
**优先级**: P0
**前置条件**: 已有待办事项

**测试步骤**:
1. 点击事项的"编辑"按钮
2. 修改标题为"深入学习 Spring Boot"
3. 保存修改

**预期结果**:
- 响应状态码为 200 OK
- `title` 字段更新为新值
- `updatedAt` 字段更新
- UI 显示新标题
- 输入框关闭

**验证点**:
- [ ] 标题更新成功
- [ ] UI 实时更新
- [ ] 时间戳正确更新

---

### TC-06: 删除待办事项
**优先级**: P0
**前置条件**: 已有待办事项

**测试步骤**:
1. 点击事项的"删除"按钮
2. 在确认对话框中点击"确定"

**预期结果**:
- 响应状态码为 204 No Content
- 事项从列表中移除
- 数据库中该事项被删除
- 统计信息更新

**验证点**:
- [ ] 事项删除成功
- [ ] UI 实时更新
- [ ] 统计信息正确

---

### TC-07: 通过分享链接访问清单
**优先级**: P0
**前置条件**: 已创建清单并添加事项

**测试步骤**:
1. 复制清单分享链接 `http://localhost:8080/list/{token}`
2. 在无痕模式/新浏览器中打开链接
3. 观察清单内容

**预期结果**:
- 页面正常加载
- 显示完整的待办清单
- 可以查看所有事项
- 可以进行添加、编辑、删除操作

**验证点**:
- [ ] 链接可访问
- [ ] 数据完整显示
- [ ] 可正常操作

---

### TC-08: 复制分享链接
**优先级**: P0
**前置条件**: 在清单详情页

**测试步骤**:
1. 点击"复制链接"按钮
2. 粘贴到文本编辑器

**预期结果**:
- URL 复制到剪贴板
- 显示"已复制"提示
- 链接格式为 `http://localhost:8080/list/{token}`
- token 与当前清单匹配

**验证点**:
- [ ] 复制功能正常
- [ ] 提示显示
- [ ] 链接正确

---

### TC-09: 刷新页面数据保留
**优先级**: P0
**前置条件**: 已有包含事项的清单

**测试步骤**:
1. 创建清单并添加 3 个事项
2. 标记 1 个为完成
3. 刷新浏览器页面（F5）

**预期结果**:
- 所有事项仍然存在
- 完成状态保持
- 统计信息准确
- 无数据丢失

**验证点**:
- [ ] 数据持久化正常
- [ ] 状态正确保留
- [ ] 无数据丢失

---

### TC-10: 获取待办事项列表
**优先级**: P0
**前置条件**: 已有清单和多个事项

**测试步骤**:
1. 访问 `GET /api/lists/{token}/items`
2. 观察响应数据

**预期结果**:
- 响应状态码为 200 OK
- 返回事项数组（非清单对象）
- 每个事项包含完整字段
- 事项顺序按创建时间排列

**验证点**:
- [ ] 响应为数组
- [ ] 字段完整
- [ ] 顺序正确

---

## 2. 关键边界用例 (P1)

### BC-01: 空标题验证
**优先级**: P1

**测试步骤**:
1. 在添加事项输入框中输入空格或留空
2. 点击"添加"按钮

**预期结果**:
- 请求被拒绝，响应状态码 400 Bad Request
- 错误信息: "Title cannot be empty"
- 事项未添加到列表

**验证点**:
- [ ] 前端验证生效
- [ ] 后端验证生效
- [ ] 错误提示友好

---

### BC-02: 超长标题验证
**优先级**: P1

**测试步骤**:
1. 输入超过 200 字符的标题
2. 点击"添加"按钮

**预期结果**:
- 响应状态码 400 Bad Request
- 错误信息包含长度限制说明
- 事项未添加

**验证点**:
- [ ] 前端长度限制
- [ ] 后端长度验证
- [ ] 明确错误提示

---

### BC-03: 特殊字符标题
**优先级**: P1

**测试步骤**:
1. 输入包含特殊字符的标题: `<script>alert('xss')</script>`
2. 添加事项
3. 编辑标题为包含引号、换行符的内容

**预期结果**:
- 事项正常创建
- 特殊字符被正确转义或处理
- 不存在 XSS 注入风险
- 数据正确存储和显示

**验证点**:
- [ ] 特殊字符正确处理
- [ ] 无 XSS 漏洞
- [ ] 数据完整性

---

### BC-04: 无效 Token 访问
**优先级**: P1

**测试步骤**:
1. 访问不存在的 token: `/list/invalidtoken123`
2. 访问 `GET /api/lists/invalidtoken123`

**预期结果**:
- 响应状态码 404 Not Found
- 返回错误信息: "List not found with token: invalidtoken123"
- 显示友好的 404 页面
- 提供"创建新清单"按钮

**验证点**:
- [ ] 正确的 HTTP 状态码
- [ ] 错误信息清晰
- [ ] 404 页面友好

---

### BC-05: 不存在的事项 ID
**优先级**: P1

**测试步骤**:
1. 尝试更新不存在的事项: `PATCH /api/items/999999`
2. 尝试删除不存在的事项: `DELETE /api/items/999999`

**预期结果**:
- 响应状态码 404 Not Found
- 错误信息: "Item not found with id: 999999"
- 无数据库异常

**验证点**:
- [ ] 正确的 HTTP 状态码
- [ ] 明确错误信息
- [ ] 无服务器异常

---

### BC-06: 更新时代题时提供空标题
**优先级**: P1

**测试步骤**:
1. 编辑已有事项
2. 将标题修改为空字符串或仅空格
3. 保存

**预期结果**:
- 响应状态码 400 Bad Request
- 错误信息: "Title cannot be empty"
- 原标题保持不变

**验证点**:
- [ ] 验证生效
- [ ] 原数据未损坏
- [ ] 错误提示明确

---

### BC-07: 更新请求不包含任何字段
**优先级**: P1

**测试步骤**:
1. 发送空的 PATCH 请求: `PATCH /api/items/1` (空 JSON `{}`)

**预期结果**:
- 响应状态码 400 Bad Request
- 错误信息: "At least one field (title or completed) must be provided"
- 或业务逻辑允许，返回未修改的数据

**验证点**:
- [ ] 适当的验证
- [ ] 错误信息清晰

---

### BC-08: Token 格式边界
**优先级**: P1

**测试步骤**:
1. 访问包含 URL 编码字符的 token
2. 访问包含路径遍历字符的 token: `/list/../../../etc/passwd`
3. 访问超长 token (> 8 字符)

**预期结果**:
- 无效 token 返回 404
- 无路径遍历漏洞
- 无 SQL 注入风险

**验证点**:
- [ ] 安全性验证
- [ ] 无注入漏洞
- [ ] 正确处理异常输入

---

### BC-09: 并发创建清单
**优先级**: P1
**测试类型**: 并发测试（可选）

**测试步骤**:
1. 快速连续发送 10 个创建清单请求
2. 观察 token 唯一性

**预期结果**:
- 所有请求成功
- 所有 token 唯一
- 无数据库冲突错误

**验证点**:
- [ ] Token 唯一性保证
- [ ] 无并发冲突
- [ ] 系统稳定性

---

### BC-10: JSON 格式错误
**优先级**: P1

**测试步骤**:
1. 发送格式错误的 JSON 请求体
   - 缺少闭合括号
   - 使用单引号而非双引号
   - 包含注释

**预期结果**:
- 响应状态码 400 Bad Request
- 错误信息: "Invalid request" 或 JSON 解析错误
- 无服务器崩溃

**验证点**:
- [ ] 请求体验证
- [ ] 友好错误提示
- [ ] 系统稳定性

---

## 3. HTTP 状态码验证

### 状态码覆盖测试

| API 端点 | 成功状态码 | 失败状态码 | 测试要点 |
|---------|-----------|-----------|---------|
| POST /api/lists | 201 | 500 | 创建成功、Token 生成失败 |
| GET /api/lists/{token} | 200 | 404 | 有效 token、无效 token |
| POST /api/lists/{token}/items | 201 | 400, 404 | 空标题、无效 token |
| GET /api/lists/{token}/items | 200 | 404 | 有效 token、无效 token |
| PATCH /api/items/{id} | 200 | 400, 404 | 空标题、无效 ID |
| DELETE /api/items/{id} | 204 | 404 | 有效 ID、无效 ID |

---

## 4. Curl 测试示例

### 环境准备
```bash
# 设置 Base URL
export BASE_URL="http://localhost:8080"
```

### 4.1 创建待办清单
```bash
curl -X POST $BASE_URL/api/lists \
  -H "Content-Type: application/json" \
  -v

# 预期响应 (201 Created):
# {
#   "id": 1,
#   "token": "aB3xK9mP",
#   "createdAt": "2026-02-18T10:30:00",
#   "items": []
# }
```

### 4.2 添加待办事项
```bash
# 替换 {token} 为实际 token
curl -X POST $BASE_URL/api/lists/{token}/items \
  -H "Content-Type: application/json" \
  -d '{"title": "学习 Spring Boot"}' \
  -v

# 预期响应 (201 Created):
# {
#   "id": 1,
#   "title": "学习 Spring Boot",
#   "completed": false,
#   "createdAt": "2026-02-18T10:31:00",
#   "updatedAt": "2026-02-18T10:31:00"
# }
```

### 4.3 获取清单详情
```bash
curl -X GET $BASE_URL/api/lists/{token} \
  -H "Content-Type: application/json" \
  -v

# 预期响应 (200 OK):
# {
#   "id": 1,
#   "token": "aB3xK9mP",
#   "createdAt": "2026-02-18T10:30:00",
#   "items": [...]
# }
```

### 4.4 更新待办事项（标记完成）
```bash
# 替换 {id} 为实际事项 ID
curl -X PATCH $BASE_URL/api/items/{id} \
  -H "Content-Type: application/json" \
  -d '{"completed": true}' \
  -v

# 预期响应 (200 OK):
# {
#   "id": 1,
#   "title": "学习 Spring Boot",
#   "completed": true,
#   "createdAt": "2026-02-18T10:31:00",
#   "updatedAt": "2026-02-18T11:30:00"
# }
```

### 4.5 更新待办事项（修改标题）
```bash
curl -X PATCH $BASE_URL/api/items/{id} \
  -H "Content-Type: application/json" \
  -d '{"title": "深入学习 Spring Boot"}' \
  -v

# 预期响应 (200 OK):
# {
#   "id": 1,
#   "title": "深入学习 Spring Boot",
#   "completed": true,
#   "createdAt": "2026-02-18T10:31:00",
#   "updatedAt": "2026-02-18T11:35:00"
# }
```

### 4.6 删除待办事项
```bash
curl -X DELETE $BASE_URL/api/items/{id} \
  -v

# 预期响应: 204 No Content (无响应体)
```

### 4.7 边界测试 - 空标题
```bash
curl -X POST $BASE_URL/api/lists/{token}/items \
  -H "Content-Type: application/json" \
  -d '{"title": ""}' \
  -v

# 预期响应 (400 Bad Request):
# {
#   "error": "Invalid request",
#   "message": "Title cannot be empty"
# }
```

### 4.8 边界测试 - 无效 Token
```bash
curl -X GET $BASE_URL/api/lists/invalidtoken123 \
  -v

# 预期响应 (404 Not Found):
# {
#   "error": "Resource not found",
#   "message": "List not found with token: invalidtoken123"
# }
```

### 4.9 边界测试 - 不存在的事项 ID
```bash
curl -X PATCH $BASE_URL/api/items/999999 \
  -H "Content-Type: application/json" \
  -d '{"completed": true}' \
  -v

# 预期响应 (404 Not Found):
# {
#   "error": "Resource not found",
#   "message": "Item not found with id: 999999"
# }
```

### 4.10 边界测试 - JSON 格式错误
```bash
curl -X POST $BASE_URL/api/lists/{token}/items \
  -H "Content-Type: application/json" \
  -d '{"title": "invalid json}' \
  -v

# 预期响应 (400 Bad Request):
# {
#   "error": "Invalid request",
#   "message": "请求体格式错误: ..."
# }
```

---

## 5. 测试执行清单

### 执行说明
- 每个测试用例执行后，在对应复选框打勾
- 记录发现的 bug 和问题
- 对于失败的用例，标记为 FAIL 并记录详细信息

### 执行记录模板

```
日期: ________
执行人: ________
环境: ________

核心功能测试用例 (P0):
[ ] TC-01: 创建待办清单 - PASS / FAIL
[ ] TC-02: 添加待办事项 - PASS / FAIL
[ ] TC-03: 获取待办清单详情 - PASS / FAIL
[ ] TC-04: 标记待办事项完成/未完成 - PASS / FAIL
[ ] TC-05: 编辑待办事项标题 - PASS / FAIL
[ ] TC-06: 删除待办事项 - PASS / FAIL
[ ] TC-07: 通过分享链接访问清单 - PASS / FAIL
[ ] TC-08: 复制分享链接 - PASS / FAIL
[ ] TC-09: 刷新页面数据保留 - PASS / FAIL
[ ] TC-10: 获取待办事项列表 - PASS / FAIL

关键边界用例 (P1):
[ ] BC-01: 空标题验证 - PASS / FAIL
[ ] BC-02: 超长标题验证 - PASS / FAIL
[ ] BC-03: 特殊字符标题 - PASS / FAIL
[ ] BC-04: 无效 Token 访问 - PASS / FAIL
[ ] BC-05: 不存在的事项 ID - PASS / FAIL
[ ] BC-06: 更新时提供空标题 - PASS / FAIL
[ ] BC-07: 更新请求不包含任何字段 - PASS / FAIL
[ ] BC-08: Token 格式边界 - PASS / FAIL
[ ] BC-09: 并发创建清单 - PASS / FAIL / N/A
[ ] BC-10: JSON 格式错误 - PASS / FAIL

Bug 记录:
1. ________
2. ________
3. ________

备注:
________
```

---

## 6. 回归测试通过标准

### 必须满足的条件
- [ ] 所有 P0 用例 100% 通过
- [ ] P1 用例通过率 >= 90%
- [ ] 无阻塞性 bug
- [ ] 无数据丢失问题
- [ ] 无安全漏洞（XSS、SQL 注入、路径遍历）
- [ ] API 契约 100% 符合 API_CONTRACT.md

### 可接受的条件
- [ ] UI 细节问题可在后续修复
- [ ] 性能优化可在后续进行
- [ ] 非 P0 功能的边界情况可后续完善

---

## 7. 测试数据准备

### 推荐测试数据
- 正常标题: "买牛奶"、"学习 Spring Boot"、"完成 API 文档"
- 空标题: `""`、`"   "`、空格
- 特殊字符: `<script>alert('xss')</script>`、`"标题'带引号"`、`"换行\n测试"`
- 超长标题: 201 个字符的字符串
- 数字标题: `"123"`、`"000"`
- Unicode 标题: `"中文测试"`、`"日本語テスト"`、`"🎉Emoji测试"`

### Token 测试数据
- 有效 token: 从创建清单 API 获取
- 无效 token: `"invalidtoken123"`、`"../../../../etc/passwd"`、`"\" OR 1=1--"`
- 空值: `""`

---

## 8. 参考文档

- **PRD**: `/d/develop/project/todolist/PRD.md`
- **API_CONTRACT**: `/d/develop/project/todolist/API_CONTRACT.md`
- **Controller 层**: `/src/main/java/com/example/todolist/controller/`
- **DTO 层**: `/src/main/java/com/example/todolist/dto/`
- **异常处理**: `/src/main/java/com/example/todolist/exception/GlobalExceptionHandler.java`

---

## 9. 附录

### 9.1 常见问题排查

**问题 1: 创建清单失败**
- 检查数据库连接
- 检查 Token 生成逻辑
- 查看服务器日志

**问题 2: Token 访问 404**
- 确认 token 正确
- 检查数据库中是否存在
- 验证 URL 路由配置

**问题 3: 事项添加失败**
- 检查请求体格式
- 验证 title 非空
- 确认 token 有效

**问题 4: 前端未更新**
- 检查 API 响应
- 验证 JavaScript 逻辑
- 查看浏览器控制台错误

---

**文档版本**: V1.0
**最后更新**: 2026-02-18
**维护者**: QA Team
