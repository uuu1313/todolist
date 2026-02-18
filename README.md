# 共享待办清单 V1 - 后端实现

## 项目概述

这是一个基于 Spring Boot 3.x 的共享待办清单应用后端，实现了完整的 REST API。

## 技术栈

- Java 17
- Spring Boot 3.2.0
- Spring Data JPA
- H2 Database (开发环境)
- Thymeleaf
- Maven

## 项目结构

```
src/main/java/com/example/todolist/
├── TodolistApplication.java           # 启动类
├── controller/
│   ├── ListController.java            # 清单相关 API
│   ├── ItemController.java            # 事项 API (添加、查询)
│   ├── ItemManagementController.java  # 事项管理 API (更新、删除)
│   └── WebController.java             # Web 页面路由
├── service/
│   ├── ListService.java               # 清单业务逻辑
│   ├── ItemService.java               # 事项业务逻辑
│   └── TokenService.java              # Token 生成服务
├── repository/
│   ├── TodoListRepository.java        # 清单 DAO
│   └── TodoItemRepository.java        # 事项 DAO
├── entity/
│   ├── TodoList.java                  # 清单实体
│   └── TodoItem.java                  # 事项实体
├── dto/
│   ├── CreateItemRequest.java         # 创建事项请求
│   ├── UpdateItemRequest.java         # 更新事项请求
│   ├── ListResponse.java              # 清单响应
│   ├── ItemResponse.java              # 事项响应
│   └── ErrorResponse.java             # 错误响应
├── exception/
│   ├── NotFoundException.java         # 404 异常
│   └── GlobalExceptionHandler.java    # 全局异常处理
└── config/
    └── JacksonConfig.java             # JSON 配置

src/main/resources/
├── application.properties             # 配置文件
├── templates/
│   ├── index.html                     # 首页
│   └── list.html                      # 清单详情页
└── static/                            # 静态资源（可选）
```

## 启动说明

### 方法 1: 使用 Maven (推荐)

```bash
cd D:\develop\project\todolist
./mvnw spring-boot:run
```

### 方法 2: 使用 Maven Wrapper (Windows)

```bash
cd D:\develop\project\todolist
mvnw.cmd spring-boot:run
```

### 方法 3: 先打包再运行

```bash
cd D:\develop\project\todolist
./mvnw clean package
java -jar target/todolist-0.0.1-SNAPSHOT.jar
```

应用将在 `http://localhost:8080` 启动。

## API 测试

### API 1: 创建清单

```bash
curl -X POST http://localhost:8080/api/lists
```

响应示例：
```json
{
  "id": 1,
  "token": "aB3xK9mP",
  "createdAt": "2026-02-18T10:00:00"
}
```

### API 2: 获取清单详情 (包含事项列表)

```bash
# 替换 YOUR_TOKEN 为实际的 token
curl http://localhost:8080/api/lists/YOUR_TOKEN
```

响应示例：
```json
{
  "id": 1,
  "token": "aB3xK9mP",
  "createdAt": "2026-02-18T10:00:00",
  "items": [
    {
      "id": 1,
      "title": "Buy milk",
      "completed": false,
      "createdAt": "2026-02-18T10:01:00"
    }
  ]
}
```

### API 3: 获取清单的所有事项

```bash
curl http://localhost:8080/api/lists/YOUR_TOKEN/items
```

响应示例：
```json
{
  "items": [
    {
      "id": 1,
      "title": "Buy milk",
      "completed": false,
      "createdAt": "2026-02-18T10:01:00",
      "updatedAt": "2026-02-18T10:01:00"
    }
  ]
}
```

### API 4: 添加新事项

```bash
curl -X POST http://localhost:8080/api/lists/YOUR_TOKEN/items \
  -H "Content-Type: application/json; charset=UTF-8" \
  -d '{"title": "Buy milk"}'
```

响应示例：
```json
{
  "id": 1,
  "title": "Buy milk",
  "completed": false,
  "createdAt": "2026-02-18T10:01:00",
  "updatedAt": "2026-02-18T10:01:00"
}
```

### API 5: 更新事项

```bash
# 标记为完成
curl -X PATCH http://localhost:8080/api/items/1 \
  -H "Content-Type: application/json; charset=UTF-8" \
  -d '{"completed": true}'

# 更新标题
curl -X PATCH http://localhost:8080/api/items/1 \
  -H "Content-Type: application/json; charset=UTF-8" \
  -d '{"title": "Buy milk and bread"}'

# 同时更新标题和完成状态
curl -X PATCH http://localhost:8080/api/items/1 \
  -H "Content-Type: application/json; charset=UTF-8" \
  -d '{"title": "Buy milk and bread", "completed": true}'
```

响应示例：
```json
{
  "id": 1,
  "title": "Buy milk and bread",
  "completed": true,
  "updatedAt": "2026-02-18T10:05:00"
}
```

### API 6: 删除事项

```bash
curl -X DELETE http://localhost:8080/api/items/1
```

响应：HTTP 204 No Content

## Web 界面

访问以下地址使用简单的 Web 界面：

- 首页: http://localhost:8080/
- 清单详情: http://localhost:8080/lists/YOUR_TOKEN

## H2 数据库控制台

开发环境下可以访问 H2 数据库控制台：

- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:todolist`
- User Name: `sa`
- Password: (留空)

## 配置说明

主要配置在 `src/main/resources/application.properties`:

```properties
# 应用配置
spring.application.name=todolist
server.port=8080

# 数据库配置 (H2)
spring.datasource.url=jdbc:h2:mem:todolist
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA 配置
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# H2 控制台
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Thymeleaf
spring.thymeleaf.cache=false
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html

# 日志配置
logging.level.com.example.todolist=DEBUG
```

## 测试注意事项

**UTF-8 编码问题**:

在 Windows 上使用 curl 测试中文时，可能会遇到 UTF-8 编码问题。解决方案：

1. 使用 `charset=UTF-8` 头：
```bash
curl -X POST "http://localhost:8080/api/lists/YOUR_TOKEN/items" \
  -H "Content-Type: application/json; charset=UTF-8" \
  --data-raw "{\"title\": \"买牛奶\"}"
```

2. 或者使用英文文本进行测试：
```bash
curl -X POST "http://localhost:8080/api/lists/YOUR_TOKEN/items" \
  -H "Content-Type: application/json" \
  -d '{"title": "Buy milk"}'
```

3. 或者使用 Postman、Insomnia 等 GUI 工具进行测试。

## 错误处理

应用实现了完善的错误处理机制：

| HTTP 状态码 | 错误码 | 说明 |
|------------|--------|------|
| 400 | Invalid request | 请求体格式错误 |
| 400 | Invalid title | 标题为空或超长 |
| 404 | Resource not found | 清单或事项不存在 |
| 500 | Failed to generate unique token | Token 生成失败 |
| 500 | Internal server error | 服务器内部错误 |

错误响应格式：
```json
{
  "error": "ERROR_CODE",
  "message": "用户友好的错误提示"
}
```

## 技术设计文档

详细的技术设计文档请参考：[TECH_DESIGN.md](./TECH_DESIGN.md)

## 开发完成标准

- [x] 项目可以成功启动（`mvn spring-boot:run`）
- [x] 所有 6 个 API 端点可正常调用
- [x] 可以使用 curl 测试所有 API
- [x] 数据持久化正常工作
- [x] 错误处理正常工作

## 后续开发

后端开发已完成，下一步是：

1. 使用 uimax skill 美化 Thymeleaf 模板 (Frontend 阶段)
2. 进行完整的端到端测试 (QA 阶段)
