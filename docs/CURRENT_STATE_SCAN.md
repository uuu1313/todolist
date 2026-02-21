# 一、项目基本信息
- 项目名称：todolist（`pom.xml` 中 `artifactId/name`）
- 技术栈：Java 17、Spring Boot 3.2.0、Spring Web、Spring Data JPA、Thymeleaf、H2、Flyway、Jakarta Validation
- 运行环境：JDK 17，默认端口 `8081`（`src/main/resources/application.yml:17`）
- 构建方式：Maven / Maven Wrapper（`mvnw`、`mvnw.cmd`，Spring Boot Maven Plugin）
- 部署方式：jar 运行（Spring Boot 内嵌容器），通过 `spring.profiles.active` 切换环境
- 数据库类型：H2
- 当前数据库模式（mem/file/remote）：mem（默认 dev，`jdbc:h2:mem:todolist`，`src/main/resources/application-dev.yml:7`）
- 是否有生产配置：有（`src/main/resources/application-prod.yml`）
- 日志配置情况：dev 控制台日志；prod 控制台 + 文件日志 `./logs/todolist.log`（可由 `TODOLIST_LOG_DIR` 覆盖，`src/main/resources/application-prod.yml:49`）

# 二、功能完成度扫描
列出：
- 已完成模块：清单创建/查询/更新/删除、任务增删改查、成员列表与移除、邀请加入、用户创建/查询/改名、我的清单聚合页、4个前端页面（`index/list/list-settings/my-lists`）
- 半完成模块：权限模型（大量接口把 `X-User-Id` 设为可选，未形成强制鉴权闭环）；邀请创建权限（代码注释写明“后续版本添加”，`src/main/java/com/example/todolist/controller/InviteController.java:26`）；前端“新建清单标题”已提交但后端创建接口未接收
- 未完成但存在代码的模块：并发冲突控制仅前端轮询/快照提示，后端无乐观锁版本校验；角色状态在多个页面各自计算，未统一
- 规划中未开发模块：代码内明确可见的仅 1 处“邀请权限后续补齐”（`src/main/java/com/example/todolist/controller/InviteController.java:26`）；未发现更多明确 TODO/FIXME 规划项

# 三、接口扫描
列出所有 Controller：
- `POST /api/lists`
- 路径：`/api/lists`
- 方法：POST
- 是否有权限控制：弱（`X-User-Id` 可缺省）
- 是否有参数校验：无
- 是否可能为空指针：低
- 是否可能越权：是（匿名可创建）

- `GET /api/lists/{token}`
- 路径：`/api/lists/{token}`
- 方法：GET
- 是否有权限控制：无
- 是否有参数校验：无
- 是否可能为空指针：低
- 是否可能越权：是（任意 token 可读）

- `GET /api/lists/{token}/version`
- 路径：`/api/lists/{token}/version`
- 方法：GET
- 是否有权限控制：无
- 是否有参数校验：无
- 是否可能为空指针：低
- 是否可能越权：是（任意 token 可读）

- `PATCH /api/lists/{token}`
- 路径：`/api/lists/{token}`
- 方法：PATCH
- 是否有权限控制：有但不完整（仅 `userId != null` 才校验 OWNER，`src/main/java/com/example/todolist/controller/ListController.java:79`）
- 是否有参数校验：有（`@Valid UpdateListRequest`）
- 是否可能为空指针：低
- 是否可能越权：是（缺失/伪造头可绕过）

- `DELETE /api/lists/{token}`
- 路径：`/api/lists/{token}`
- 方法：DELETE
- 是否有权限控制：有但不完整（同上，`src/main/java/com/example/todolist/controller/ListController.java:103`）
- 是否有参数校验：无
- 是否可能为空指针：低
- 是否可能越权：是

- `POST /api/lists/{token}/items`
- 路径：`/api/lists/{token}/items`
- 方法：POST
- 是否有权限控制：有但不完整（仅 `userId != null` 才校验成员，`src/main/java/com/example/todolist/controller/ItemController.java:42`）
- 是否有参数校验：有（`@Valid CreateItemRequest`）
- 是否可能为空指针：低
- 是否可能越权：是

- `GET /api/lists/{token}/items`
- 路径：`/api/lists/{token}/items`
- 方法：GET
- 是否有权限控制：无
- 是否有参数校验：无
- 是否可能为空指针：低
- 是否可能越权：是（任意 token 可读）

- `PATCH /api/items/{id}?token=...`
- 路径：`/api/items/{id}`
- 方法：PATCH
- 是否有权限控制：有但不完整（`src/main/java/com/example/todolist/controller/ItemManagementController.java:47`）
- 是否有参数校验：有（`@Valid` + `request.isValid()`）
- 是否可能为空指针：低
- 是否可能越权：是

- `DELETE /api/items/{id}?token=...`
- 路径：`/api/items/{id}`
- 方法：DELETE
- 是否有权限控制：有但不完整（`src/main/java/com/example/todolist/controller/ItemManagementController.java:78`）
- 是否有参数校验：无
- 是否可能为空指针：低
- 是否可能越权：是

- `POST /api/lists/{token}/invites`
- 路径：`/api/lists/{token}/invites`
- 方法：POST
- 是否有权限控制：基本无（注释明确暂不校验，`src/main/java/com/example/todolist/controller/InviteController.java:26`）
- 是否有参数校验：无
- 是否可能为空指针：低
- 是否可能越权：是

- `POST /api/lists/join`
- 路径：`/api/lists/join`
- 方法：POST
- 是否有权限控制：要求 `X-User-Id` 非空
- 是否有参数校验：有（`@Valid JoinListRequest`）
- 是否可能为空指针：低
- 是否可能越权：中（可用任意有效 inviteToken 加入）

- `GET /api/lists/{token}/members`
- 路径：`/api/lists/{token}/members`
- 方法：GET
- 是否有权限控制：无
- 是否有参数校验：无
- 是否可能为空指针：低
- 是否可能越权：是（成员信息可枚举）

- `DELETE /api/lists/{token}/members/{userId}`
- 路径：`/api/lists/{token}/members/{userId}`
- 方法：DELETE
- 是否有权限控制：有（operator 必须存在且 OWNER）
- 是否有参数校验：无
- 是否可能为空指针：低
- 是否可能越权：低（该接口相对完整）

- `GET /api/my/lists`
- 路径：`/api/my/lists`
- 方法：GET
- 是否有权限控制：弱（仅依赖可伪造头）
- 是否有参数校验：无
- 是否可能为空指针：低
- 是否可能越权：是（伪造 `X-User-Id` 可读他人清单摘要）

- `POST /api/users`
- 路径：`/api/users`
- 方法：POST
- 是否有权限控制：无
- 是否有参数校验：无（`CreateUserRequest` 无注解）
- 是否可能为空指针：低
- 是否可能越权：中（可批量创建匿名用户）

- `GET /api/users/{id}`
- 路径：`/api/users/{id}`
- 方法：GET
- 是否有权限控制：无
- 是否有参数校验：无
- 是否可能为空指针：低
- 是否可能越权：是（可探测用户存在）

- `PATCH /api/users/{id}`
- 路径：`/api/users/{id}`
- 方法：PATCH
- 是否有权限控制：无
- 是否有参数校验：无（未 `@Valid`）
- 是否可能为空指针：是（`request` 可能为 null）
- 是否可能越权：是（可改任意用户）

- `GET /`（WebController）
- 路径：`/`
- 方法：GET
- 是否有权限控制：无
- 是否有参数校验：无
- 是否可能为空指针：低
- 是否可能越权：否

- `GET /lists/{token}`（页面）
- 路径：`/lists/{token}`
- 方法：GET
- 是否有权限控制：无
- 是否有参数校验：无
- 是否可能为空指针：低
- 是否可能越权：页面可访问，数据权限由后端 API 决定

- `GET /lists/{token}/settings`（页面）
- 路径：`/lists/{token}/settings`
- 方法：GET
- 是否有权限控制：无
- 是否有参数校验：无
- 是否可能为空指针：低
- 是否可能越权：同上

- `GET /my-lists`（页面）
- 路径：`/my-lists`
- 方法：GET
- 是否有权限控制：无
- 是否有参数校验：无
- 是否可能为空指针：低
- 是否可能越权：同上

# 四、实体与数据库扫描
- 实体数量：5 个实体类（`TodoList`、`TodoItem`、`User`、`ListMember`、`InviteToken`）+ 2 个枚举（`Priority`、`MemberRole`）
- 是否存在循环依赖：实体关系上存在双向关联（`TodoList` ↔ `TodoItem`），但通过 DTO 和 `@JsonIgnore` 基本避免直接序列化循环
- 是否存在懒加载风险：有（多处 `LAZY`，DTO 构造时访问关联对象）
- 是否存在 N+1 查询：有潜在风险（`MyListsController`、`MemberService#getMembers`、`ItemResponse` 访问 `createdBy/updatedBy`）
- Flyway 是否完整：当前到 `V3`（`V1` 建表、`V2` 协作字段、`V3` 索引），迁移文件齐全
- 是否存在结构漂移：未见明显漂移（实体字段与 `V1~V3` 基本对齐）

# 五、安全风险扫描
- 是否存在未校验 X-User-Id：是（大量接口 `required=false`，且未验证“请求人=操作者”）
- 是否存在越权修改：是（清单/任务写接口在缺少头时可绕过成员或 OWNER 校验）
- 是否存在水平越权：是（`PATCH /api/users/{id}` 可改任意用户；`/api/my/lists` 可通过伪造头读取他人摘要）
- 是否存在CSRF风险：中（当前主要问题是“无鉴权/弱鉴权”；若未来引入 cookie 会进一步放大 CSRF面）
- 是否存在任意数据读取：是（按 token 可读取清单、任务、成员；无强身份边界）

# 六、前端结构扫描
- 页面数量：4（`index.html`、`list.html`、`list-settings.html`、`my-lists.html`）
- 是否逻辑耦合严重：是（`list.html` 1872 行，页面内聚合大量状态与接口逻辑）
- 是否存在重复 JS：是（用户初始化、`window.fetch` 拦截、角色计算、toast、轮询在多个页面重复）
- 是否存在刷新机制混乱：是（本地操作 `reload` + 远端轮询 banner 并存，不同页面策略不一致）
- 是否存在角色状态不一致风险：是（角色在不同页面独立计算并依赖 `localStorage`，存在显示与后端权限不一致窗口）

# 七、部署风险扫描
- 文件路径硬编码情况：中（生产库与日志默认相对路径 `./data`、`./logs`，依赖启动目录）
- H2 file路径问题：有风险（`jdbc:h2:file:${TODOLIST_DB_DIR:./data}/todolist`，目录权限/挂载不当会失败）
- 日志路径问题：有风险（`TODOLIST_LOG_DIR` 未配置时写 `./logs`，容器只读文件系统会报错）
- jar 打包是否包含资源：是（标准 Spring Boot 打包，`src/main/resources` 会进入 jar）
- 生产环境 profile 是否正确：配置存在且内容完整，但默认激活 `dev`，发布时若未显式设 `prod` 会误用内存库

# 八、代码质量评估
- 重复代码位置：`src/main/resources/templates/index.html`、`src/main/resources/templates/list.html`、`src/main/resources/templates/list-settings.html`、`src/main/resources/templates/my-lists.html`（用户初始化与 fetch 拦截逻辑重复）
- 巨大类：`src/main/resources/templates/list.html`（1872 行），`src/main/resources/templates/list-settings.html`（635 行）
- 巨大方法：`src/main/java/com/example/todolist/service/ItemService.java` 的 `updateItem(...)` 逻辑较长且职责混合；前端 `list.html` 多个超长函数
- 魔法字符串：`"X-User-Id"`、角色字符串 `"OWNER"/"MEMBER"/"VISITOR"`、错误消息字符串在多处硬编码
- 事务边界问题：Service 层统一 `@Transactional` 基本可用，但权限判断散落在 Controller，未统一到鉴权层，导致边界不一致
- 潜在 Bug 列举，写一份md文件：`docs/POTENTIAL_BUGS_SCAN.md`
