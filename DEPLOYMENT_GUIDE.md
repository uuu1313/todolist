# 生产环境部署指南

## 目录结构（推荐）

```
/opt/todolist/              # 应用根目录
├── app/                    # 应用程序目录
│   └── todolist.jar       # Spring Boot JAR 包
├── data/                   # 数据库文件目录（自动创建）
│   └── todolist.mv.db     # H2 数据库文件
├── logs/                   # 日志目录（自动创建）
│   └── todolist.log       # 应用日志
├── scripts/                # 启动停止脚本（可选）
│   ├── start.sh
│   └── stop.sh
└── README.md              # 本文件
```

---

## 方式 1: 使用命令行参数（推荐用于测试）

### 基础启动
```bash
# 进入应用目录
cd /opt/todolist/app

# 启动应用（指定数据库目录）
java -jar todolist.jar \
  --spring.profiles.active=prod \
  --TODOLIST_DB_DIR=/opt/todolist/data
```

### 后台运行 + 日志重定向
```bash
nohup java -jar todolist.jar \
  --spring.profiles.active=prod \
  --TODOLIST_DB_DIR=/opt/todolist/data \
  --TODOLIST_LOG_DIR=/opt/todolist/logs \
  > /opt/todolist/logs/console.log 2>&1 &

# 记录 PID
echo $! > /opt/todolist/app/app.pid
```

### 指定端口
```bash
java -jar todolist.jar \
  --spring.profiles.active=prod \
  --TODOLIST_DB_DIR=/opt/todolist/data \
  --server.port=8080
```

---

## 方式 2: 使用环境变量（推荐用于生产）

### 设置环境变量并启动
```bash
# 设置环境变量
export SPRING_PROFILES_ACTIVE=prod
export TODOLIST_DB_DIR=/opt/todolist/data
export TODOLIST_LOG_DIR=/opt/todolist/logs

# 启动应用
java -jar /opt/todolist/app/todolist.jar
```

### 写入系统环境变量（持久化）
```bash
# 添加到 /etc/profile 或 /etc/environment
echo 'export SPRING_PROFILES_ACTIVE=prod' >> /etc/profile
echo 'export TODOLIST_DB_DIR=/opt/todolist/data' >> /etc/profile
echo 'export TODOLIST_LOG_DIR=/opt/todolist/logs' >> /etc/profile

# 重新加载配置
source /etc/profile

# 启动应用
java -jar /opt/todolist/app/todolist.jar
```

---

## 方式 3: 使用 systemd 服务（推荐用于正式生产）

### 创建服务文件
```bash
sudo nano /etc/systemd/system/todolist.service
```

内容如下：
```ini
[Unit]
Description=Todolist Application
After=network.target

[Service]
Type=simple
User=todolist
Group=todolist
WorkingDirectory=/opt/todolist/app

# 环境变量
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="TODOLIST_DB_DIR=/opt/todolist/data"
Environment="TODOLIST_LOG_DIR=/opt/todolist/logs"

# JVM 参数
Environment="JAVA_OPTS=-Xms512m -Xmx1024m"

# 启动命令
ExecStart=/usr/bin/java $JAVA_OPTS -jar todolist.jar

# 重启策略
Restart=on-failure
RestartSec=10

# 日志
StandardOutput=append:/opt/todolist/logs/systemd.log
StandardError=append:/opt/todolist/logs/systemd-error.log

[Install]
WantedBy=multi-user.target
```

### 启动和管理服务
```bash
# 创建专用用户（可选）
sudo useradd -r -s /bin/false todolist
sudo chown -R todolist:todolist /opt/todolist

# 重载 systemd 配置
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start todolist

# 设置开机自启
sudo systemctl enable todolist

# 查看状态
sudo systemctl status todolist

# 查看日志
sudo journalctl -u todolist -f

# 停止服务
sudo systemctl stop todolist

# 重启服务
sudo systemctl restart todolist
```

---

## 部署步骤

### 1. 准备环境
```bash
# 安装 Java 17+
sudo apt update
sudo apt install -y openjdk-17-jre

# 验证 Java 版本
java -version
```

### 2. 创建目录结构
```bash
# 创建应用根目录
sudo mkdir -p /opt/todolist/{app,data,logs,scripts}

# 创建专用用户（可选）
sudo useradd -r -s /bin/false todolist

# 设置权限
sudo chown -R todolist:todolist /opt/todolist
```

### 3. 打包并部署 JAR
```bash
# 在开发机器上打包
cd /d/develop/project/todolist
mvn clean package -DskipTests

# JAR 文件位置：target/todolist-0.0.1-SNAPSHOT.jar

# 上传到服务器（示例使用 scp）
scp target/todolist-0.0.1-SNAPSHOT.jar user@server:/opt/todolist/app/todolist.jar

# 在服务器上设置权限
sudo chown todolist:todolist /opt/todolist/app/todolist.jar
chmod +x /opt/todolist/app/todolist.jar
```

### 4. 启动应用
```bash
# 方式 1: 命令行参数
cd /opt/todolist/app
sudo -u todolist java -jar todolist.jar \
  --spring.profiles.active=prod \
  --TODOLIST_DB_DIR=/opt/todolist/data \
  --TODOLIST_LOG_DIR=/opt/todolist/logs

# 方式 2: systemd 服务
sudo systemctl start todolist
sudo systemctl status todolist
```

### 5. 验证部署
```bash
# 检查端口监听
sudo netstat -tlnp | grep 8081

# 测试 HTTP 访问
curl http://localhost:8081/

# 查看日志
tail -f /opt/todolist/logs/todolist.log
```

---

## 配置说明

### 环境变量

| 变量名 | 必填 | 默认值 | 说明 |
|--------|------|--------|------|
| `SPRING_PROFILES_ACTIVE` | 否 | `dev` | 激活的 profile：dev 或 prod |
| `TODOLIST_DB_DIR` | 否 | `./data` | 数据库文件目录 |
| `TODOLIST_LOG_DIR` | 否 | `./logs` | 日志文件目录 |

### 数据库文件

生产模式下，数据库文件路径为：
```
${TODOLIST_DB_DIR}/todolist.mv.db
```

例如：
- 设置 `TODOLIST_DB_DIR=/opt/todolist/data`
- 数据库文件为 `/opt/todolist/data/todolist.mv.db`

---

## 备份与恢复

### 备份数据库
```bash
# 停止应用（推荐）
sudo systemctl stop todolist

# 备份数据库文件
cp /opt/todolist/data/todolist.mv.db /backup/todolist-$(date +%Y%m%d).mv.db

# 重启应用
sudo systemctl start todolist
```

### 恢复数据库
```bash
# 停止应用
sudo systemctl stop todolist

# 恢复数据库文件
cp /backup/todolist-20250219.mv.db /opt/todolist/data/todolist.mv.db

# 重启应用
sudo systemctl start todolist
```

---

## 升级部署

### 滚动升级（零停机）
```bash
# 1. 备份数据库
cp /opt/todolist/data/todolist.mv.db /backup/todolist-pre-upgrade.mv.db

# 2. 上传新版本 JAR
scp target/todolist-0.0.1-SNAPSHOT.jar user@server:/opt/todolist/app/todolist-new.jar

# 3. 停止旧版本
sudo systemctl stop todolist

# 4. 替换 JAR
mv /opt/todolist/app/todolist.jar /opt/todolist/app/todolist-old.jar
mv /opt/todolist/app/todolist-new.jar /opt/todolist/app/todolist.jar

# 5. 启动新版本
sudo systemctl start todolist

# 6. 验证
curl http://localhost:8081/

# 7. 回滚（如果需要）
sudo systemctl stop todolist
mv /opt/todolist/app/todolist.jar /opt/todolist/app/todolist-failed.jar
mv /opt/todolist/app/todolist-old.jar /opt/todolist/app/todolist.jar
sudo systemctl start todolist
```

---

## 监控与维护

### 查看应用状态
```bash
# systemd 服务状态
sudo systemctl status todolist

# 查看 CPU/内存
top -p $(pgrep -f todolist.jar)

# 查看磁盘使用
du -sh /opt/todolist/data/
du -sh /opt/todolist/logs/
```

### 日志轮转（可选）
创建 `/etc/logrotate.d/todolist`：
```
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

---

## 防火墙配置

```bash
# UFW（Ubuntu）
sudo ufw allow 8081/tcp
sudo ufw reload

# firewalld（CentOS）
sudo firewall-cmd --permanent --add-port=8081/tcp
sudo firewall-cmd --reload
```

---

## 反向代理（可选）

### 使用 Nginx
```nginx
server {
    listen 80;
    server_name todolist.example.com;

    location / {
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 常见问题排查

详见 `TROUBLESHOOTING.md`
