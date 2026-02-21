# 部署前审计报告

- 审计时间: 2026-02-21
- 审计范围: 当前工作树（含未提交改动）
- 审计方式: 静态检查 + 测试执行（`./mvnw -q test`）
- 结论: `NO-GO`（当前不建议发布）

## 一、发布结论

当前版本存在测试失败和权限边界缺口，未达到可发布状态。

## 二、阻断项（Blockers）

1. 自动化测试未通过（阻断发布）
- 证据: `target/surefire-reports/com.example.todolist.VisitorPermissionTest.txt`
- 结果: `Tests run: 31, Failures: 3, Errors: 2`
- 失败/错误明细:
  - `testVisitorCanReadItems`: 期望 200，实际 404
  - `testVisitorCannotCreateItem`: 期望 403，实际 404
  - `testVisitorCannotDeleteItem`: 期望 403，实际 404
  - `testVisitorCannotUpdateItem`: `Invalid HTTP method: PATCH`（测试客户端能力问题）
  - `testOwnerCanCreateItem`: `ItemResponse` 反序列化失败（缺少可反序列化构造）

2. 清单更新/删除接口仍可在无用户头场景下绕过权限校验
- 证据:
  - `src/main/java/com/example/todolist/controller/ListController.java:76`
  - `src/main/java/com/example/todolist/controller/ListController.java:85`
  - `src/main/java/com/example/todolist/controller/ListController.java:101`
  - `src/main/java/com/example/todolist/controller/ListController.java:109`
- 说明: 当前仅在 `userId != null` 时校验 OWNER，缺失 `X-User-Id` 时会跳过校验。

3. 邀请链接创建接口未做权限校验
- 证据:
  - `src/main/java/com/example/todolist/controller/InviteController.java:21`
  - `src/main/java/com/example/todolist/controller/InviteController.java:26`
- 说明: 代码注释明确“暂不验证权限”，存在非 OWNER 生成邀请风险。

## 三、高风险（非阻断但需尽快处理）

1. 用户信息修改接口缺少身份绑定
- 证据:
  - `src/main/java/com/example/todolist/controller/UserController.java:44`
  - `src/main/java/com/example/todolist/controller/UserController.java:47`
- 说明: `PATCH /api/users/{id}` 未绑定当前用户身份，存在水平越权风险。

2. 运行产物可能被误提交
- 证据:
  - `git status --short` 显示 `?? data/`
  - `.gitignore` 未包含 `data/`（仅忽略 `target/`、`*.log` 等）
- 说明: 文件型 H2 数据库目录未忽略，可能将本地数据带入仓库。

## 四、中风险与可维护性问题

1. 异常处理存在潜在二次异常
- 证据: `src/main/java/com/example/todolist/exception/GlobalExceptionHandler.java:58`
- 说明: 直接调用 `e.getMostSpecificCause().getMessage()`，当 cause 为空时存在 NPE 风险。

2. 文档与源码存在历史不一致
- 证据: 多个文档仍描述 `dev` 为内存库（例如 `README.md`）
- 现状: 源码中 `dev/prod` 均为文件型 H2。

## 五、正向项（已具备）

1. 孤儿清单问题已在创建接口增加约束
- 证据:
  - `src/main/java/com/example/todolist/controller/ListController.java:38`
  - `src/main/java/com/example/todolist/controller/ListController.java:43`
- 说明: 创建清单已要求 `X-User-Id` 且校验用户存在。

2. 同级事项“旧在上新在下”排序已落地到仓储查询
- 证据: `src/main/java/com/example/todolist/repository/TodoItemRepository.java`
- 说明: 同分组内按 `createdAt ASC, id ASC`。

3. Flyway 迁移链完整
- 证据:
  - `src/main/resources/db/migration/V1__create_initial_tables.sql`
  - `src/main/resources/db/migration/V2__add_collaboration.sql`
  - `src/main/resources/db/migration/V3__add_indexes.sql`

## 六、发布前必须完成清单

1. 修复并通过所有测试（至少 `VisitorPermissionTest` 全绿）。
2. 收紧写接口权限边界（至少清单更新/删除、邀请创建）。
3. 为 `data/` 添加忽略策略，避免本地数据库入库。
4. 回归验证:
- `POST /api/lists`（缺失/非法/合法 `X-User-Id`）
- `PATCH/DELETE /api/lists/{token}` 权限路径
- `POST /api/lists/{token}/invites` 仅 OWNER 可用

## 七、审计命令记录

- `git status --short`
- `./mvnw -q test`
- `Get-Content target/surefire-reports/com.example.todolist.VisitorPermissionTest.txt`
- 配置/控制器/仓储只读扫描命令（`Get-Content` / `rg` / `Select-String`）
