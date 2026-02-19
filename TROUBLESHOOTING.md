# 常见问题排查清单

## 1. ddl-auto 与 Flyway 冲突

### 现象
```
ERROR: Schema "PUBLIC" already exists
Flyway: Schema history table "PUBLIC"."flyway_schema_history" already exists
```

### 原因
- `spring.jpa.hibernate.ddl-auto` 设置为 `create` 或 `create-drop`
- JPA 尝试自动建表，与 Flyway 冲突

### 解决
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # ✅ 正确：仅验证，不建表
    # ddl-auto: none     # ✅ 也可以：完全关闭 DDL
```

**禁止配置**：
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create        # ❌ 会删除表！
      ddl-auto: create-drop   # ❌ 会删除表！
      ddl-auto: update        # ❌ 与 Flyway 冲突
```

---

## 2. H2 file 路径问题

### 现象
```
ERROR: Unable to open database "jdbc:h2:file:./data/todolist"
FileNotFoundException: ./data/todolist.mv.db (Permission denied)
```

### 原因
- 目录不存在
- 权限不足
- 相对路径解析错误

### 排查步骤

#### 2.1 检查目录是否存在
```bash
ls -la /opt/todolist/data/
```

如果不存在，创建目录：
```bash
mkdir -p /opt/todolist/data
chown -R todolist:todolist /opt/todolist/data
```

#### 2.2 检查权限
```bash
# 查看目录权限
ls -ld /opt/todolist/data/

# 期望输出：drwxr-xr-x 2 todolist todolist 4096 ...

# 修改权限
chmod 755 /opt/todolist/data
chown todolist:todolist /opt/todolist/data
```

#### 2.3 使用绝对路径（推荐）
```bash
# ❌ 不推荐：相对路径
java -jar todolist.jar --TODOLIST_DB_DIR=./data

# ✅ 推荐：绝对路径
java -jar todolist.jar --TODOLIST_DB_DIR=/opt/todolist/data
```

#### 2.4 检查配置
```yaml
spring:
  datasource:
    # ✅ 正确配置
    url: jdbc:h2:file:${TODOLIST_DB_DIR:./data}/todolist;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=TRUE
```

**关键参数**：
- `DB_CLOSE_ON_EXIT=FALSE` - JVM 退出时不关闭数据库（避免文件锁）
- `AUTO_SERVER=TRUE` - 允许并发连接（多实例）

---

## 3. 文件锁/并发启动报错

### 现象
```
ERROR: The file is locked: nio:/path/to/todolist.mv.db
ERROR: The database is read-only
```

### 原因
- 多个进程同时访问同一个 H2 文件
- JVM 异常退出导致文件锁未释放
- 缺少 `AUTO_SERVER=TRUE` 参数

### 解决方案

#### 3.1 确保只有一个实例运行
```bash
# 查找 Java 进程
ps aux | grep todolist.jar

# 如果有多个，杀掉旧的
kill -9 <old-pid>
```

#### 3.2 添加 AUTO_SERVER 参数
```yaml
spring:
  datasource:
    url: jdbc:h2:file:${TODOLIST_DB_DIR:./data}/todolist;AUTO_SERVER=TRUE
```

#### 3.3 检查文件锁
```bash
# Linux: 使用 lsof
lsof /opt/todolist/data/todolist.mv.db

# 输出示例：
# java   12345 todolist   10u  REG  253,1  102400 123456 /opt/todolist/data/todolist.mv.db

# 如果没有进程占用但仍然报错，删除 .lock 文件
rm /opt/todolist/data/todolist.lock.db
```

#### 3.4 优雅停止应用
```bash
# 推荐使用 systemd
sudo systemctl stop todolist

# 或使用 SIGTERM（不要使用 kill -9）
kill $(cat /opt/todolist/app/app.pid)
```

---

## 4. Flyway schema history 表异常

### 现象
```
ERROR: Validate failed: Migration checksum mismatch for migration version 1
ERROR: Schema history table "PUBLIC"."flyway_schema_history" exists but has problems
```

### 原因
- 数据库文件已存在，但 Flyway 认为版本不匹配
- 手动修改了数据库表结构
- Flyway 版本升级

### 解决方案

#### 4.1 重新初始化（会丢失数据！）
```bash
# 停止应用
sudo systemctl stop todolist

# 删除旧数据库
rm /opt/todolist/data/todolist.mv.db

# 启动应用（Flyway 会自动创建新数据库）
sudo systemctl start todolist
```

#### 4.2 修复 schema history 表
```bash
# 连接 H2 数据库（仅 dev 模式）
java -jar todolist.jar --spring.profiles.active=dev

# 访问 H2 Console
# URL: http://localhost:8081/h2-console
# JDBC URL: jdbc:h2:mem:todolist
# User Name: sa
# Password: (留空)

# 执行 SQL
DELETE FROM flyway_schema_history WHERE version = '1';
```

#### 4.3 基线化已有数据库
```yaml
spring:
  flyway:
    baseline-on-migrate: true  # ✅ 已在配置中
    baseline-version: 0        # 设置基线版本
```

---

## 5. 数据未持久化

### 现象
- 重启应用后数据丢失

### 原因
- 未激活 `prod` profile
- 仍在使用 `dev` 模式（H2 内存数据库）

### 排查步骤

#### 5.1 检查激活的 profile
```bash
# 查看启动日志
grep "Active profiles" /opt/todolist/logs/todolist.log

# 期望输出：The following 1 profile is active: "prod"
```

#### 5.2 检查数据库文件是否存在
```bash
ls -lh /opt/todolist/data/todolist.mv.db

# 期望输出：-rw-r----- 1 todolist todolist 128K Feb 19 18:00 todolist.mv.db
```

#### 5.3 确认启动参数
```bash
# ✅ 正确
java -jar todolist.jar --spring.profiles.active=prod

# ❌ 错误（默认 dev）
java -jar todolist.jar
```

---

## 6. 日志文件过大

### 现象
- 日志文件占用大量磁盘空间

### 解决方案

#### 6.1 配置日志轮转
```yaml
logging:
  file:
    name: ${TODOLIST_LOG_DIR:./logs}/todolist.log
    max-size: 100MB      # ✅ 单文件最大 100MB
    max-history: 30      # ✅ 保留 30 天
```

#### 6.2 使用 logrotate
```bash
# 创建 /etc/logrotate.d/todolist
/opt/todolist/logs/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 0640 todolist todolist
}
```

#### 6.3 清理旧日志
```bash
# 删除 30 天前的日志
find /opt/todolist/logs/ -name "*.log" -mtime +30 -delete
```

---

## 7. 应用启动失败

### 现象
```
ERROR: Failed to initialize pool: Connection is closed
ERROR: Cannot create PoolableConnectionFactory
```

### 排查清单

#### 7.1 检查 Java 版本
```bash
java -version

# 期望：openjdk version "17.x.x" 或更高
```

#### 7.2 检查端口占用
```bash
sudo netstat -tlnp | grep 8081

# 如果被占用，更换端口
java -jar todolist.jar --server.port=8082
```

#### 7.3 检查磁盘空间
```bash
df -h /opt/todolist/

# 确保有足够空间（至少 1GB）
```

#### 7.4 检查内存
```bash
free -h

# 如果内存不足，调整 JVM 参数
java -Xms256m -Xmx512m -jar todolist.jar
```

#### 7.5 查看详细错误
```bash
# 启用调试模式
java -jar todolist.jar --spring.profiles.active=prod --debug

# 查看完整日志
tail -100 /opt/todolist/logs/todolist.log
```

---

## 8. 性能问题

### 现象
- 响应缓慢
- CPU/内存占用高

### 排查步骤

#### 8.1 查看 JVM 参数
```bash
# 查看进程内存
ps aux | grep todolist.jar

# 输出示例：
# todolist 12345 10.5 2.3 2345678 98765 ?  Sl   18:00  0:15 java -jar todolist.jar
#                    ^   ^
#                    CPU MEM
```

#### 8.2 调整 JVM 参数
```bash
# 减少内存占用
java -Xms256m -Xmx512m -jar todolist.jar

# 增加性能
java -Xms512m -Xmx1024m -server -jar todolist.jar
```

#### 8.3 启用 GC 日志
```bash
java -Xlog:gc*:file=/opt/todolist/logs/gc.log -jar todolist.jar
```

---

## 9. 网络访问问题

### 现象
- 无法从外部访问应用

### 排查步骤

#### 9.1 检查防火墙
```bash
# Ubuntu (UFW)
sudo ufw status
sudo ufw allow 8081/tcp

# CentOS (firewalld)
sudo firewall-cmd --list-all
sudo firewall-cmd --permanent --add-port=8081/tcp
sudo firewall-cmd --reload
```

#### 9.2 检查服务绑定
```bash
# 检查是否监听 0.0.0.0
sudo netstat -tlnp | grep 8081

# 期望输出：
# tcp  0  0.0.0.0:8081  0.0.0.0:*  LISTEN  12345/java
```

#### 9.3 检查 SELinux（CentOS）
```bash
# 临时关闭 SELinux
sudo setenforce 0

# 永久关闭（不推荐）
sudo nano /etc/selinux/config
# SELINUX=permissive
```

---

## 10. 数据损坏恢复

### 现象
```
ERROR: General error: "java.lang.Error: Internal error"
ERROR: Database may be corrupted
```

### 解决方案

#### 10.1 使用 H2 备份工具
```bash
# 停止应用
sudo systemctl stop todolist

# 使用 H2 工具恢复
java -cp ~/.m2/repository/com/h2database/h2/*/h2-*.jar org.h2.tools.Recover \
  -dir /opt/todolist/data/ -db todolist
```

#### 10.2 从备份恢复
```bash
# 停止应用
sudo systemctl stop todolist

# 恢复备份
cp /backup/todolist-20250219.mv.db /opt/todolist/data/todolist.mv.db

# 启动应用
sudo systemctl start todolist
```

---

## 快速诊断命令

```bash
# 一键健康检查
#!/bin/bash
echo "=== TodoList 健康检查 ==="
echo ""
echo "1. 进程状态："
ps aux | grep todolist.jar | grep -v grep
echo ""
echo "2. 端口监听："
sudo netstat -tlnp | grep 8081
echo ""
echo "3. 磁盘使用："
du -sh /opt/todolist/data/ /opt/todolist/logs/
echo ""
echo "4. 数据库文件："
ls -lh /opt/todolist/data/todolist.mv.db
echo ""
echo "5. 最近的错误日志："
tail -20 /opt/todolist/logs/todolist.log | grep -i error
echo ""
echo "6. 内存使用："
free -h
```

---

## 联系与支持

如果以上方案无法解决问题：
1. 查看完整日志：`tail -100 /opt/todolist/logs/todolist.log`
2. 启用调试模式：`--debug`
3. 查看 Spring Boot 官方文档
