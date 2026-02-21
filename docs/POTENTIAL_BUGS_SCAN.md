# 潜在 Bug 列举（现状扫描）

## 高风险

1. 匿名可修改/删除清单
- 位置: `src/main/java/com/example/todolist/controller/ListController.java:79`
- 现象: 仅在 `userId != null` 时进行 OWNER 校验；缺失 `X-User-Id` 时会直接放行更新与删除。
- 影响: 任意知道 `token` 的请求可越权修改或删除清单。

2. 匿名可新增/修改/删除任务
- 位置: `src/main/java/com/example/todolist/controller/ItemController.java:42`
- 位置: `src/main/java/com/example/todolist/controller/ItemManagementController.java:47`
- 现象: 成员校验同样在 `userId != null` 分支内执行；无用户头时放行。
- 影响: 访客可越权写入或破坏任务数据。

3. 邀请链接可由非 OWNER 生成
- 位置: `src/main/java/com/example/todolist/controller/InviteController.java:26`
- 现象: 代码注释明确“暂不验证权限”。
- 影响: 非授权成员可扩散邀请链接，导致权限边界失效。

4. 用户信息可被任意修改
- 位置: `src/main/java/com/example/todolist/controller/UserController.java:44`
- 现象: `PATCH /api/users/{id}` 未绑定 `X-User-Id`，也无鉴权层。
- 影响: 可水平越权修改任意用户昵称。

## 中风险

5. 异常处理器存在空指针风险
- 位置: `src/main/java/com/example/todolist/exception/GlobalExceptionHandler.java:58`
- 现象: 直接调用 `e.getMostSpecificCause().getMessage()`，若 cause 为空会触发二次异常。
- 影响: 原始 400 错误可能被覆盖为 500，增加排障难度。

6. 前端“新建清单标题”参数被后端忽略
- 位置: `src/main/resources/templates/my-lists.html:451`
- 位置: `src/main/java/com/example/todolist/controller/ListController.java:33`
- 现象: 前端提交 `title`，后端创建接口不接收 body，仅使用默认标题。
- 影响: 前后端行为不一致，用户输入被静默丢弃。

7. 文档/页面编码异常
- 位置: `README.md`
- 位置: `src/main/resources/templates/*.html`
- 现象: 多处中文显示为乱码。
- 影响: 可维护性下降，错误信息与文档理解偏差。

## 性能与可维护性风险

8. 潜在 N+1 查询
- 位置: `src/main/java/com/example/todolist/controller/MyListsController.java:52`
- 位置: `src/main/java/com/example/todolist/service/MemberService.java:84`
- 位置: `src/main/java/com/example/todolist/dto/ItemResponse.java:38`
- 现象: 聚合读取后按条访问关联对象（list/user）并序列化。
- 影响: 数据量增长时查询次数放大，接口抖动。

9. 前端页面脚本过大且逻辑重复
- 位置: `src/main/resources/templates/list.html`（1872 行）
- 位置: `src/main/resources/templates/list-settings.html`（635 行）
- 现象: 用户初始化、`window.fetch` 拦截、角色判断、轮询逻辑在多个页面重复实现。
- 影响: 修复成本高，容易出现状态不一致与行为回归。
