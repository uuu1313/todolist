# 生产环境快速参考

## 配置文件清单

### 新增文件
```
src/main/resources/
├── application.yml           # 主配置文件（默认 dev）
├── application-dev.yml       # 开发环境配置（H2 内存数据库）
└── application-prod.yml      # 生产环境配置（H2 文件数据库）
```

### 删除文件
```
src/main/resources/application.properties  # ❌ 已删除
```

---

## 核心配置差异

| 配置项 | dev 模式 | prod 模式 |
|--------|----------|-----------|
| 数据库类型 | H2 内存 | H2 文件 |
| 数据库 URL | `jdbc:h2:mem:todolist` | `jdbc:h2:file:${TODOLIST_DB_DIR}/todolist` |
| 数据持久化 | ❌ 否 | ✅ 是 |
| H2 Console | ✅ 开启 | ❌ 关闭 |
| JPA ddl-auto | `validate` | `validate` |
| SQL 日志 | `true` | `false` |
| Thymeleaf 缓存 | `false` | `true` |
| 日志级别 | DEBUG | INFO |

---

## 启动命令速查

### 开发模式（默认）
```bash
# 方式 1: 不指定 profile（默认 dev）
java -jar todolist.jar

# 方式 2: 显式指定 dev
java -jar todolist.jar --spring.profiles.active=dev
```

### 生产模式
```bash
# 方式 1: 命令行参数
java -jar todolist.jar \
  --spring.profiles.active=prod \
  --TODOLIST_DB_DIR=/opt/todolist/data

# 方式 2: 环境变量
export SPRING_PROFILES_ACTIVE=prod
export TODOLIST_DB_DIR=/opt/todolist/data
java -jar todolist.jar

# 方式 3: 一行命令
SPRING_PROFILES_ACTIVE=prod TODOLIST_DB_DIR=/opt/todolist/data java -jar todolist.jar
```

---

## 环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `SPRING_PROFILES_ACTIVE` | `dev` | 激活的 profile |
| `TODOLIST_DB_DIR` | `./data` | 数据库文件目录 |
| `TODOLIST_LOG_DIR` | `./logs` | 日志文件目录 |

---

## 数据库文件路径

### 开发模式
- 无文件（内存数据库）

### 生产模式
```
${TODOLIST_DB_DIR}/todolist.mv.db
${TODOLIST_DB_DIR}/todolist.lock.db
```

示例：
- 环境变量 `TODOLIST_DB_DIR=/opt/todolist/data`
- 数据库文件：`/opt/todolist/data/todolist.mv.db`

---

## 推荐目录结构

### 生产环境
```
/opt/todolist/
├── app/
│   └── todolist.jar       # JAR 包
├── data/                   # 数据库文件目录
│   ├── todolist.mv.db     # H2 数据库文件
│   └── todolist.lock.db   # 锁文件
├── logs/                   # 日志目录
│   ├── todolist.log       # 应用日志
│   └── systemd.log        # systemd 日志（如果使用）
└── scripts/                # 启动脚本（可选）
    ├── start.sh
    └── stop.sh
```

### 权限设置
```bash
# 创建目录
sudo mkdir -p /opt/todolist/{app,data,logs}

# 创建专用用户
sudo useradd -r -s /bin/false todolist

# 设置权限
sudo chown -R todolist:todolist /opt/todolist
```

---

## systemd 服务配置

### 服务文件路径
```
/etc/systemd/system/todolist.service
```

### 关键配置
```ini
[Service]
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="TODOLIST_DB_DIR=/opt/todolist/data"
Environment="TODOLIST_LOG_DIR=/opt/todolist/logs"
ExecStart=/usr/bin/java -jar /opt/todolist/app/todolist.jar
```

### 管理命令
```bash
sudo systemctl start todolist   # 启动
sudo systemctl stop todolist    # 停止
sudo systemctl restart todolist  # 重启
sudo systemctl status todolist   # 状态
sudo systemctl enable todolist   # 开机自启
```

---

## 端口与网络

| 项目 | 值 |
|------|-----|
| 默认端口 | `8081` |
| 协议 | HTTP |
| 健康检查 | `curl http://localhost:8081/` |

### 防火墙配置
```bash
# Ubuntu (UFW)
sudo ufw allow 8081/tcp

# CentOS (firewalld)
sudo firewall-cmd --permanent --add-port=8081/tcp
sudo firewall-cmd --reload
```

---

## 备份与恢复

### 备份
```bash
# 停止应用
sudo systemctl stop todolist

# 备份数据库
cp /opt/todolist/data/todolist.mv.db /backup/todolist-$(date +%Y%m%d).mv.db

# 重启应用
sudo systemctl start todolist
```

### 恢复
```bash
# 停止应用
sudo systemctl stop todolist

# 恢复数据库
cp /backup/todolist-20250219.mv.db /opt/todolist/data/todolist.mv.db

# 重启应用
sudo systemctl start todolist
```

---

## 升级流程

```bash
# 1. 备份数据
sudo systemctl stop todolist
cp /opt/todolist/data/todolist.mv.db /backup/todolist-pre-upgrade.mv.db

# 2. 替换 JAR
cp todolist.jar /opt/todolist/app/todolist.jar.new
mv /opt/todolist/app/todolist.jar /opt/todolist/app/todolist.old
mv /opt/todolist/app/todolist.jar.new /opt/todolist/app/todolist.jar

# 3. 启动新版本
sudo systemctl start todolist

# 4. 验证
curl http://localhost:8081/

# 5. 回滚（如果需要）
sudo systemctl stop todolist
mv /opt/todolist/app/todolist.jar /opt/todolist/app/todolist-failed.jar
mv /opt/todolist/app/todolist.old /opt/todolist/app/todolist.jar
sudo systemctl start todolist
```

---

## JVM 参数调优

### 小型部署（< 100 用户）
```bash
java -Xms256m -Xmx512m -jar todolist.jar
```

### 中型部署（100-1000 用户）
```bash
java -Xms512m -Xmx1024m -server -jar todolist.jar
```

### 大型部署（> 1000 用户）
```bash
java -Xms1g -Xmx2g -server -XX:+UseG1GC -jar todolist.jar
```

---

## 常见问题速查

| 问题 | 解决方案 |
|------|----------|
| 应用启动失败 | 检查 `TODOLIST_DB_DIR` 是否存在且有写权限 |
| 数据丢失 | 确认使用 `prod` profile，而不是 `dev` |
| 端口冲突 | 使用 `--server.port=8082` 更换端口 |
| H2 Console 无法访问 | prod 模式下已禁用，这是正常的 |
| 数据库文件锁 | 使用 `AUTO_SERVER=TRUE` 参数（已配置） |
| Flyway 重复建表 | 确认 `ddl-auto=validate` |

---

## 监控命令

### 查看进程
```bash
ps aux | grep todolist.jar
```

### 查看端口
```bash
sudo netstat -tlnp | grep 8081
```

### 查看日志
```bash
tail -f /opt/todolist/logs/todolist.log
```

### 查看磁盘使用
```bash
du -sh /opt/todolist/data/
```

---

## 验证清单

部署后必须验证：

- [ ] 应用成功启动
- [ ] 日志显示 `profile: "prod"`
- [ ] 数据库文件 `todolist.mv.db` 已创建
- [ ] 可以访问 http://localhost:8081/
- [ ] 可以创建清单和待办
- [ ] 重启后数据仍然存在
- [ ] H2 Console 已关闭（访问 /h2-console 返回 404）
- [ ] 日志文件正常写入

---

## 技术支持

- 详细部署指南：`DEPLOYMENT_GUIDE.md`
- 验收测试步骤：`ACCEPTANCE_TEST.md`
- 常见问题排查：`TROUBLESHOOTING.md`
