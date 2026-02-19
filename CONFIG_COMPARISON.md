# 配置改造对比（改造前 vs 改造后）

## 配置文件对比

### 改造前
```
src/main/resources/
└── application.properties  (单一配置文件)
```

### 改造后
```
src/main/resources/
├── application.yml        (主配置，默认 dev)
├── application-dev.yml    (开发环境配置)
└── application-prod.yml   (生产环境配置)
```

---

## 关键配置对比

| 配置项 | 改造前 | 改造后 (dev) | 改造后 (prod) |
|--------|--------|--------------|---------------|
| **文件类型** | .properties | .yml | .yml |
| **数据库** | H2 mem | H2 mem | H2 file |
| **数据库 URL** | `jdbc:h2:mem:todolist` | `jdbc:h2:mem:todolist;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` | `jdbc:h2:file:${TODOLIST_DB_DIR}/todolist;AUTO_SERVER=TRUE` |
| **数据持久化** | ❌ 否 | ❌ 否 | ✅ 是 |
| **H2 Console** | ✅ 开启 | ✅ 开启 | ❌ 关闭 |
| **JPA ddl-auto** | `validate` | `validate` | `validate` |
| **SQL 日志** | `true` | `true` | `false` |
| **Thymeleaf 缓存** | `false` | `false` | `true` |
| **日志级别** | DEBUG | DEBUG | INFO |
| **日志文件** | ❌ 无 | ❌ 无 | ✅ 有（可配置） |

---

## 启动方式对比

### 改造前
```bash
# 只有一种方式：开发模式
java -jar todolist.jar
# 数据存在内存中，重启丢失
```

### 改造后
```bash
# 开发模式（默认）
java -jar todolist.jar
# 或
java -jar todolist.jar --spring.profiles.active=dev

# 生产模式（数据持久化）
java -jar todolist.jar --spring.profiles.active=prod --TODOLIST_DB_DIR=/opt/todolist/data

# 或使用环境变量
export SPRING_PROFILES_ACTIVE=prod
java -jar todolist.jar
```

---

## 数据持久化对比

| 场景 | 改造前 | 改造后 (dev) | 改造后 (prod) |
|------|--------|--------------|---------------|
| **创建清单** | ✅ 成功 | ✅ 成功 | ✅ 成功 |
| **重启应用** | ❌ 数据丢失 | ❌ 数据丢失 | ✅ 数据保留 |
| **数据库文件** | ❌ 无 | ❌ 无 | ✅ todolist.mv.db |
| **适用场景** | 测试 | 开发调试 | 生产部署 |

---

## H2 Console 对比

| 环境 | 改造前 | 改造后 (dev) | 改造后 (prod) |
|------|--------|--------------|---------------|
| **启用状态** | ✅ 开启 | ✅ 开启 | ❌ 关闭 |
| **访问路径** | `/h2-console` | `/h2-console` | N/A |
| **适用场景** | 调试 | 开发调试 | 生产环境（安全） |

**安全性提升**：生产环境自动关闭 H2 Console，避免未授权访问。

---

## Flyway 行为对比

| 场景 | 改造前 | 改造后 (dev) | 改造后 (prod) |
|------|--------|--------------|---------------|
| **首次启动** | 建表 | 建表 | 建表 |
| **第二次启动** | 验证 | 验证 | 验证 |
| **数据库已存在** | N/A（内存） | N/A（内存） | ✅ 验证通过，不重复建表 |

---

## 日志对比

| 项目 | 改造前 | 改造后 (dev) | 改造后 (prod) |
|------|--------|--------------|---------------|
| **日志级别** | DEBUG | DEBUG | INFO |
| **SQL 日志** | ✅ 显示 | ✅ 显示 | ❌ 隐藏 |
| **日志文件** | ❌ 无 | ❌ 无 | ✅ todolist.log |
| **日志轮转** | ❌ 无 | ❌ 无 | ✅ 100MB/30天 |

**生产优化**：
- 降低日志级别（减少性能开销）
- 关闭 SQL 日志（安全）
- 支持日志文件（便于排查问题）
- 日志轮转（避免磁盘占满）

---

## 性能对比

| 项目 | 改造前 | 改造后 (dev) | 改造后 (prod) |
|------|--------|--------------|---------------|
| **数据库性能** | 内存（快） | 内存（快） | 文件（稍慢） |
| **Thymeleaf 缓存** | ❌ 关闭（热更新） | ❌ 关闭（热更新） | ✅ 开启（性能） |
| **SQL 日志开销** | ✅ 有（调试） | ✅ 有（调试） | ❌ 无（性能） |
| **适用场景** | 开发 | 开发调试 | 生产环境 |

---

## 部署复杂度对比

| 项目 | 改造前 | 改造后 (dev) | 改造后 (prod) |
|------|--------|--------------|---------------|
| **配置复杂度** | 简单 | 简单 | 中等 |
| **目录要求** | 无 | 无 | data/, logs/ |
| **环境变量** | 不需要 | 不需要 | 推荐 |
| **适用部署** | ❌ 不适合 | ❌ 不适合 | ✅ 适合 |

---

## 迁移路径

### 从改造前迁移到改造后（dev 模式）
**步骤**：无需操作
- 默认 profile 是 `dev`
- 行为与改造前完全一致
- 可以无缝切换

### 从改造前迁移到改造后（prod 模式）
**步骤**：
1. 替换配置文件（新配置覆盖旧配置）
2. 重新打包：`mvn clean package`
3. 使用 `--spring.profiles.active=prod` 启动
4. 验证数据持久化

---

## 向后兼容性

✅ **完全兼容**
- dev 模式行为与改造前一致
- API 接口未改动
- 数据库结构未改动
- Flyway migrations 未改动

✅ **平滑升级**
- 可以保留旧配置（application.properties）
- 新旧配置可以共存（优先级：yml > properties）
- 建议删除旧配置避免混淆

---

## 测试验证

### 改造前测试
```bash
# 启动
java -jar todolist.jar

# 创建数据
curl -X POST http://localhost:8081/api/users

# 重启
# Ctrl + C, 然后重新启动

# 验证：数据丢失 ❌
```

### 改造后测试（prod 模式）
```bash
# 启动
java -jar todolist.jar \
  --spring.profiles.active=prod \
  --TODOLIST_DB_DIR=/opt/todolist/data

# 创建数据
curl -X POST http://localhost:8081/api/users

# 重启
# Ctrl + C, 然后重新启动

# 验证：数据保留 ✅
```

---

## 文件变更统计

| 类型 | 数量 | 说明 |
|------|------|------|
| **删除文件** | 1 | application.properties |
| **新增配置** | 3 | application.yml + dev + prod |
| **新增文档** | 5 | 部署指南 + 验收测试 + 排查 + 参考 + 交付 |
| **代码改动** | 0 | ✅ 零业务代码改动 |
| **依赖改动** | 0 | ✅ 零依赖改动 |

---

## 总结

**改造原则**：最小改动、不引入新框架、不改业务逻辑

**改造成果**：
- ✅ 支持开发/生产双环境
- ✅ 生产数据持久化
- ✅ 验收测试通过
- ✅ 完整文档交付

**适用场景**：
- dev：本地开发、调试
- prod：单服务器部署、数据持久化

**不可用场景**：
- 多实例/分布式（需要专业数据库）
- 高并发写入（H2 限制）
- 大数据量（建议 MySQL/Postgres）
