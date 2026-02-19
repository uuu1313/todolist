# 本地验收测试步骤

## 目标
验证 prod 模式下数据持久化功能：
1. 启动应用并创建数据
2. 停止应用
3. 重新启动应用
4. 验证数据仍然存在

---

## 测试环境

- 操作系统：Windows/Linux/macOS 均可
- Java 版本：17+
- Maven：已安装

---

## 步骤 1: 编译打包

```bash
# 进入项目目录
cd /d/develop/project/todolist

# 清理并打包（跳过测试）
mvn clean package -DskipTests

# 验证 JAR 文件生成
ls -lh target/todolist-0.0.1-SNAPSHOT.jar
```

期望输出：
```
-rw-r--r-- 1 user group 50M Feb 19 18:00 target/todolist-0.0.1-SNAPSHOT.jar
```

---

## 步骤 2: 创建测试目录

```bash
# 创建测试目录结构
mkdir -p /tmp/todolist-test/{data,logs,app}

# 复制 JAR 到测试目录
cp target/todolist-0.0.1-SNAPSHOT.jar /tmp/todolist-test/app/todolist.jar

# 查看目录结构
tree /tmp/todolist-test
```

期望输出：
```
/tmp/todolist-test/
├── app/
│   └── todolist.jar
├── data/
└── logs/
```

---

## 步骤 3: 第一次启动（prod 模式）

### 方式 1: 命令行参数
```bash
cd /tmp/todolist-test/app

# 启动应用（prod 模式，数据库文件在 /tmp/todolist-test/data）
java -jar todolist.jar \
  --spring.profiles.active=prod \
  --TODOLIST_DB_DIR=/tmp/todolist-test/data \
  --TODOLIST_LOG_DIR=/tmp/todolist-test/logs \
  --server.port=8082
```

### 方式 2: 环境变量
```bash
export SPRING_PROFILES_ACTIVE=prod
export TODOLIST_DB_DIR=/tmp/todolist-test/data
export TODOLIST_LOG_DIR=/tmp/todolist-test/logs

cd /tmp/todolist-test/app
java -jar todolist.jar --server.port=8082
```

### 验证启动成功

**期望日志输出**：
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)

2026-02-19 18:00:00.123  INFO 12345 --- [           main] c.e.t.TodolistApplication   : The following 1 profile is active: "prod"
2026-02-19 18:00:01.456  INFO 12345 --- [           main] o.f.c.i.FlywayExecutor         : Successfully validated 2 migrations
2026-02-19 18:00:02.789  INFO 12345 --- [           main] o.s.b.w.e.TomcatWebServer  : Tomcat started on port 8082
```

**关键点检查**：
- ✅ `The following 1 profile is active: "prod"`
- ✅ `Successfully validated 2 migrations`（Flyway 验证通过）
- ✅ `Tomcat started on port 8082`

**验证 H2 Console 关闭**：
```bash
# 访问 http://localhost:8082/h2-console
# 期望：404 Not Found（prod 模式下 H2 Console 已关闭）
```

---

## 步骤 4: 创建测试数据

### 方式 1: 使用浏览器
1. 打开浏览器访问：http://localhost:8082/
2. 点击"创建新清单"
3. 创建几个待办事项

### 方式 2: 使用 cURL API
```bash
# 1. 创建用户
USER1_RESPONSE=$(curl -s -X POST http://localhost:8082/api/users)
USER1_ID=$(echo $USER1_RESPONSE | jq -r '.id')
echo "User ID: $USER1_ID"

# 2. 创建清单
LIST_RESPONSE=$(curl -s -X POST http://localhost:8082/api/lists \
  -H "X-User-Id: $USER1_ID")
LIST_TOKEN=$(echo $LIST_RESPONSE | jq -r '.token')
echo "List Token: $LIST_TOKEN"

# 3. 添加待办事项
curl -X POST http://localhost:8082/api/lists/$LIST_TOKEN/items \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER1_ID" \
  -d '{
    "title": "测试待办1",
    "priority": "HIGH",
    "dueDate": "2026-12-31"
  }'

curl -X POST http://localhost:8082/api/lists/$LIST_TOKEN/items \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER1_ID" \
  -d '{
    "title": "测试待办2",
    "priority": "MEDIUM"
  }'

# 4. 查询待办事项
curl -s http://localhost:8082/api/lists/$LIST_TOKEN/items \
  -H "X-User-Id: $USER1_ID" | jq '.'
```

**期望输出**：
```json
[
  {
    "id": 1,
    "title": "测试待办1",
    "completed": false,
    "priority": "HIGH",
    "dueDate": "2026-12-31"
  },
  {
    "id": 2,
    "title": "测试待办2",
    "completed": false,
    "priority": "MEDIUM",
    "dueDate": null
  }
]
```

---

## 步骤 5: 验证数据库文件已创建

```bash
# 查看数据库文件
ls -lh /tmp/todolist-test/data/

# 期望输出：
# -rw-r----- 1 user group 128K Feb 19 18:05 todolist.mv.db
# -rw-r----- 1 user group  256 Feb 19 18:05 todolist.lock.db

# 查看文件大小变化（应该 > 0）
du -sh /tmp/todolist-test/data/todolist.mv.db
```

**关键点检查**：
- ✅ `todolist.mv.db` 文件存在
- ✅ 文件大小 > 100KB
- ✅ 文件时间戳是最近的

---

## 步骤 6: 停止应用

```bash
# 方式 1: Ctrl + C（如果是前台运行）

# 方式 2: 查找进程并杀死
ps aux | grep todolist.jar
kill 12345

# 方式 3: 使用 pkill
pkill -f todolist.jar

# 验证进程已停止
ps aux | grep todolist.jar | grep -v grep
```

**期望输出**：空（没有进程）

---

## 步骤 7: 第二次启动（验证数据持久化）

```bash
cd /tmp/todolist-test/app

# 使用相同的参数重新启动
java -jar todolist.jar \
  --spring.profiles.active=prod \
  --TODOLIST_DB_DIR=/tmp/todolist-test/data \
  --TODOLIST_LOG_DIR=/tmp/todolist-test/logs \
  --server.port=8082
```

**期望日志输出**：
```
2026-02-19 18:10:00.123  INFO 12346 --- [           main] c.e.t.TodolistApplication   : The following 1 profile is active: "prod"
2026-02-19 18:10:01.456  INFO 12346 --- [           main] o.f.c.i.FlywayExecutor         : Schema "PUBLIC" version is 2 (out of 2)
2026-02-19 18:10:01.457  INFO 12346 --- [           main] o.f.c.i.FlywayExecutor         : Successfully validated 2 migrations
2026-02-19 18:10:02.789  INFO 12346 --- [           main] o.s.b.w.e.TomcatWebServer  : Tomcat started on port 8082
```

**关键点检查**：
- ✅ `Schema "PUBLIC" version is 2 (out of 2)` - 数据库版本正确
- ✅ `Successfully validated 2 migrations` - Flyway 验证通过
- ✅ 不会再次创建表（因为已存在）

---

## 步骤 8: 验证数据仍然存在

### 方式 1: 使用浏览器
1. 打开浏览器访问：http://localhost:8082/
2. 不创建新清单，直接访问旧清单：http://localhost:8082/lists/{LIST_TOKEN}
3. 验证之前创建的待办事项仍然存在

### 方式 2: 使用 cURL API
```bash
# 使用之前的用户 ID 和清单 Token
USER1_ID=1
LIST_TOKEN=<之前的 token>

# 查询待办事项
curl -s http://localhost:8082/api/lists/$LIST_TOKEN/items \
  -H "X-User-Id: $USER1_ID" | jq '.'
```

**期望输出**（与步骤 4 的数据完全一致）：
```json
[
  {
    "id": 1,
    "title": "测试待办1",
    "completed": false,
    "priority": "HIGH",
    "dueDate": "2026-12-31"
  },
  {
    "id": 2,
    "title": "测试待办2",
    "completed": false,
    "priority": "MEDIUM",
    "dueDate": null
  }
]
```

---

## 步骤 9: 多次重启测试

```bash
# 停止应用
pkill -f todolist.jar

# 等待 2 秒
sleep 2

# 启动应用
cd /tmp/todolist-test/app
java -jar todolist.jar \
  --spring.profiles.active=prod \
  --TODOLIST_DB_DIR=/tmp/todolist-test/data \
  --TODOLIST_LOG_DIR=/tmp/todolist-test/logs \
  --server.port=8082 &

# 等待启动完成
sleep 5

# 验证数据
curl -s http://localhost:8082/api/lists/$LIST_TOKEN/items \
  -H "X-User-Id: $USER1_ID" | jq '.[0].title'

# 期望输出："测试待办1"
```

---

## 步骤 10: 清理测试环境

```bash
# 停止应用
pkill -f todolist.jar

# 删除测试目录
rm -rf /tmp/todolist-test

# 验证清理完成
ls /tmp/todolist-test
# 期望输出：ls: cannot access '/tmp/todolist-test': No such file or directory
```

---

## 验收检查清单

- [ ] JAR 文件成功打包
- [ ] prod profile 成功激活
- [ ] H2 Console 在 prod 模式下关闭（访问 /h2-console 返回 404）
- [ ] 数据库文件 todolist.mv.db 成功创建
- [ ] 第一次启动可以创建清单和待办
- [ ] 停止应用后数据库文件仍然存在
- [ ] 第二次启动不重复建表（Flyway validate 通过）
- [ ] 第二次启动后数据完全保留
- [ ] 多次重启后数据一致
- [ ] 日志文件正常写入

---

## 常见错误与解决

### 错误 1: profile 未激活
```
日志显示：The following 1 profile is active: "dev"
```
**解决**：确保添加 `--spring.profiles.active=prod`

### 错误 2: 数据库文件未创建
```
ls: cannot access '/tmp/todolist-test/data/todolist.mv.db': No such file or directory
```
**解决**：
1. 检查 `TODOLIST_DB_DIR` 路径是否正确
2. 检查目录权限
3. 查看日志中的错误信息

### 错误 3: Flyway 重复建表失败
```
ERROR: Schema "PUBLIC" already exists
```
**解决**：
1. 检查 `spring.jpa.hibernate.ddl-auto=validate`
2. 不要使用 `create` 或 `create-drop`

### 错误 4: 第二次启动数据丢失
```
重启后查询返回 []
```
**解决**：
1. 确认使用的是 prod 模式
2. 确认使用的是同一个 `TODOLIST_DB_DIR`
3. 检查数据库文件时间戳

---

## 预期测试时间

- 编译打包：1 分钟
- 第一次启动：10 秒
- 创建测试数据：1 分钟
- 停止应用：5 秒
- 第二次启动：10 秒
- 验证数据：1 分钟

**总计**：约 3-5 分钟

---

## 测试通过标准

所有步骤成功完成，且：
1. ✅ 数据库文件持久化到磁盘
2. ✅ 重启后数据完全保留
3. ✅ Flyway 不会重复建表
4. ✅ 日志正常写入
5. ✅ H2 Console 在 prod 模式下关闭

如果所有检查点都通过，说明数据持久化功能正常，可以部署到生产环境。
