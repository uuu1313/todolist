# 部署上线前代码审查 - 待确认问题

## 审查时间
2026-02-19

---

## 已修复的阻塞问题 ✅

### 1. 邀请链接生成硬编码本地地址 - 已修复
**修复文件**: `src/main/java/com/example/todolist/controller/InviteController.java`
```java
// 修复前
String inviteUrl = "http://localhost:8080/join?invite=" + invite.getToken();

// 修复后
String inviteUrl = "/lists/" + token + "?invite=" + invite.getToken();
```

### 2. 缺少必要的数据库索引 - 已修复
**新增文件**: `src/main/resources/db/migration/V3__add_indexes.sql`
```sql
CREATE INDEX idx_list_member_user_id ON list_member(user_id);
CREATE INDEX idx_todo_item_list_id ON todo_item(list_id);
CREATE INDEX idx_invite_token_token ON invite_token(token);
```

### 3. H2 数据库 AUTO_SERVER 参数多余 - 已修复
**修复文件**: `src/main/resources/application-prod.yml`
```yaml
# 修复前
url: jdbc:h2:file:${TODOLIST_DB_DIR:./data}/todolist;AUTO_SERVER=TRUE

# 修复后
url: jdbc:h2:file:${TODOLIST_DB_DIR:./data}/todolist
```

### 4. 删除清单的 OWNER 检查 - 无需修复
**分析**: 当前的删除逻辑是合理的：
- 已经检查了操作者是否是 OWNER
- 删除最后一个 OWNER 是正常业务逻辑（清单不再需要）
- 级联删除会自动清理所有相关数据

---

## 重要但可后置（上线后尽快修复）

### 1. ✅ 编辑冲突友好提示 - 已修复
**优先级**: P0（高）
**修复文件**: `src/main/resources/templates/list.html`

**实现方案**:
- **进入编辑时**: 记录快照并提示 "编辑中，请尽快保存，避免与他人修改冲突"
- **保存时检测冲突**: 对比服务器最新数据与快照，如果不同则提示 "保存前内容可能已被他人修改，本次保存可能覆盖对方改动"
- **策略**: 后写覆盖（仍然保存，只是警告用户）

---

### 2. ✅ 前端初始化流程存在潜在竞态条件 - 已修复
**优先级**: P1（中）
**修复文件**: `list.html`, `index.html`, `my-lists.html`

**实现方案**: 在所有 DOMContentLoaded 事件开头添加防重复初始化检查
```javascript
if (window.__initialized) return;
window.__initialized = true;
```

---

### 3. 缺少 CSRF 保护
**优先级**: P1（中）
**现状**: 项目没有 CSRF token 机制
**影响**: 潜在的跨站请求伪造风险
**缓解因素**: 用户 ID 在 localStorage 中，相对安全

**建议**:
1. 短期：添加 SameSite Cookie 属性
2. 长期：实现 Spring Security CSRF（需要引入依赖）

**预计工作量**: 2-4小时（完整实现）

---

### 4. 缺少安全响应头
**优先级**: P2（低）
**现状**: 静态资源没有设置安全响应头

**建议**: 在 `WebMvcConfigurer` 中添加
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Bean
    public FilterRegistrationBean<Filter> securityHeadersFilter() {
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                           HttpServletResponse response,
                                           FilterChain filterChain) throws ServletException, IOException {
                response.setHeader("X-Content-Type-Options", "nosniff");
                response.setHeader("X-Frame-Options", "DENY");
                response.setHeader("X-XSS-Protection", "1; mode=block");
                response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                filterChain.doFilter(request, response);
            }
        });
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }
}
```

**预计工作量**: 30分钟

---

### 5. 日志配置不完整
**优先级**: P1（中）
**现状**:
- 缺少 HTTP 请求日志
- 缺少慢查询日志
- 错误日志没有单独配置

**建议**: 在 `application-prod.yml` 中添加
```yaml
logging:
  level:
    com.example.todolist: INFO
    org.springframework.web: INFO  # 记录 HTTP 请求
    org.hibernate.SQL: WARN  # 记录慢查询
    org.hibernate.type.descriptor.sql.BasicBinder: WARN  # 记录 SQL 参数
  file:
    name: ${TODOLIST_LOG_DIR:./logs}/todolist.log
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30
      total-size-cap: 3GB
```

**预计工作量**: 15分钟

---

### 6. 缺少健康检查端点
**优先级**: P1（中）
**现状**: 没有 `/actuator/health` 或自定义健康检查
**影响**: 部署后无法快速检测服务是否正常运行

**建议**:
1. **方案 A（简单）**: 添加自定义端点
   ```java
   @RestController
   public class HealthController {
       @GetMapping("/health")
       public ResponseEntity<Map<String, String>> health() {
           Map<String, String> status = new HashMap<>();
           status.put("status", "UP");
           status.put("timestamp", LocalDateTime.now().toString());
           return ResponseEntity.ok(status);
       }
   }
   ```

2. **方案 B（标准）**: 引入 Spring Boot Actuator
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-actuator</artifactId>
   </dependency>
   ```
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,info
     endpoint:
       health:
         show-details: when-authorized
   ```

**预计工作量**: 方案 A（5分钟）/ 方案 B（30分钟）

---

## 建议优化（不阻塞上线）

### 1. 前端 fetch 拦截器可以封装为独立模块
**优先级**: P2（低）
**现状**: 每个 HTML 文件都重复实现 fetch 拦截逻辑
**建议**: 提取为 `src/main/resources/static/js/fetch-interceptor.js`
**预计工作量**: 30分钟

---

### 2. 邀请 Token 可设置过期时间
**优先级**: P2（低）
**现状**: 邀请链接永久有效
**建议**:
1. 在 `invite_token` 表添加 `expires_at` 字段
2. 创建定时任务清理过期邀请（每日凌晨）
3. 加入时检查是否过期
**预计工作量**: 2小时

---

### 3. 清单标题验证逻辑重复
**优先级**: P3（低）
**现状**: `list.html:1260-1273` 和后端 DTO 都有验证
**建议**: 统一到后端验证，前端只做基本 UX 检查
**预计工作量**: 15分钟

---

### 4. 错误提示可以更友好
**优先级**: P3（低）
**现状**: 部分错误消息直接返回技术术语（如 "Resource not found"）
**建议**: 使用用户友好的中文消息
**预计工作量**: 30分钟

---

### 5. 移动端响应式可以进一步优化
**优先级**: P3（低）
**现状**: 移动端样式已存在，但部分交互可以更流畅
**建议**: 优化编辑表单的移动端体验
**预计工作量**: 2小时

---

### 6. 添加清单描述字段
**优先级**: P3（低）
**现状**: 清单只有标题
**建议**:
1. 在 `todo_list` 表添加 `description` 字段
2. 前端显示可选的描述信息
**预计工作量**: 3小时

---

## 无法确认的点（需要用户决策）

### 1. ❓ 并发场景的预期行为
**问题**: 如果两个用户同时编辑同一个事项的标题，最终保存哪个？
**现状**: 后续提交会覆盖前一个（后写覆盖）
**确认**: 是否需要乐观锁（`@Version`）？
**影响范围**: `todo_item`, `todo_list` 表
**预计工作量**: 1小时

---

### 2. ❓ 数据备份策略
**问题**: H2 file 数据库备份频率是多少？
**建议方案**:
1. 每日自动备份到 `${TODOLIST_DB_DIR}/backups/`
2. 保留最近 7 天的备份
3. 使用 Shell 脚本或 Cron 任务
```bash
#!/bin/bash
# backup.sh
BACKUP_DIR="${TODOLIST_DB_DIR:-./data}/backups"
mkdir -p "$BACKUP_DIR"
cp "${TODOLIST_DB_DIR:-./data}/todolist.mv.db" "$BACKUP_DIR/todolist_$(date +%Y%m%d_%H%M%S).db"
# 保留最近7天
find "$BACKUP_DIR" -name "todolist_*.db" -mtime +7 -delete
```
**确认**: 是否需要自动备份脚本？

---

### 3. ❓ 用户数据隐私
**问题**: localStorage 中的 `todolist_user_id` 会被广告脚本读取吗？
**现状**: 使用 localStorage 存储
**建议方案**: 使用 Cookie 替代 localStorage
```java
// 创建用户时返回 Cookie
Cookie cookie = new Cookie("todolist_user_id", String.valueOf(user.getId()));
cookie.setHttpOnly(true);
cookie.setSecure(true); // 仅 HTTPS
cookie.setPath("/");
cookie.setMaxAge(365 * 24 * 60 * 60); // 1年
response.addCookie(cookie);
```
**确认**: 是否需要迁移到 Cookie？

---

### 4. ❓ 邀请链接的有效期
**问题**: 邀请链接是否应该有过期时间？
**现状**: 永久有效
**建议**: 添加过期机制（如 7 天）
**确认**: 7天是否合适？还是永久有效？

---

### 5. ❓ 清单删除的二次确认
**问题**: 删除清单时的确认提示是否足够？
**现状**: `confirm('确定要删除这个清单吗？删除后无法恢复，所有事项也将被删除。')`
**建议**: 要求输入清单标题确认
**确认**: 是否需要更强的确认机制？

---

### 6. ❓ 数据库目录权限
**问题**: `${TODOLIST_DB_DIR}` 目录是否需要特殊权限？
**建议**:
```bash
# 创建数据目录并设置权限
mkdir -p ./data
chmod 750 ./data  # 仅所有者和组可读写
```
**确认**: 部署用户是否有读写权限？

---

### 7. ❓ 日志保留策略
**问题**: `${TODOLIST_LOG_DIR}` 目录下的日志文件是否会自动清理？
**现状**: 已配置 `max-history: 30`，但需要确认 logback 是否生效
**建议**: 添加日志轮转配置
**确认**: 30天是否合适？

---

## 上线前验收清单

### 配置与环境
- [x] `application.yml` 默认 profile 为 dev（安全）
- [x] `application-prod.yml` 正确配置 H2 file 数据库
- [x] `ddl-auto=validate` 防止与 Flyway 冲突
- [x] H2 console 在生产环境禁用
- [x] Thymeleaf cache 在生产环境开启
- [x] 日志文件路径可配置
- [x] 邀请链接硬编码已修复
- [x] 数据库索引已添加（V3 迁移）
- [x] AUTO_SERVER 参数已移除
- [ ] **TODO: 测试从 dev 切换到 prod 的数据迁移**

### 数据与迁移
- [x] Flyway `baseline-on-migrate=true`（允许已有数据）
- [x] V1、V2、V3 迁移脚本语法正确
- [x] 外键约束正确设置（ON DELETE CASCADE）
- [x] UNIQUE 约束防止重复成员
- [ ] **TODO: 验证 V3 迁移在生产环境正确执行**

### API 与权限
- [x] OWNER/MEMBER/VISITOR 角色正确实现
- [x] `X-User-Id` header 自动注入
- [x] 创建事项时检查 MEMBER 权限
- [x] 编辑/删除清单时检查 OWNER 权限
- [x] 移除成员时检查 OWNER 权限
- [x] 访客（VISITOR）不能添加/编辑/删除事项
- [x] 访客可以查看事项
- [ ] **TODO: 测试各种权限边界情况**

### 前端初始化与权限
- [x] 用户自愈机制（后端清库后自动重建）
- [x] 初始化顺序：ensureUser → loadMembers → loadUserRole → renderInviteSection → loadItems
- [x] 邀请链接处理不再使用 `return` 阻断后续初始化
- [x] 移除按钮逻辑已修复（检查当前用户角色而非目标用户角色）
- [x] 全局 `myUserRole` 变量正确存储
- [x] 邀请区域根据角色显示/隐藏
- [ ] **TODO: 测试快速刷新页面的竞态条件**

### 安全基础
- [x] `@Valid` 验证请求数据
- [x] `escapeHtml` 防止 XSS
- [x] `@JsonIgnore` 防止敏感字段序列化
- [x] 长度限制（title 200, list title 100, token 8-12）
- [ ] **TODO: 添加 HTTPS 配置文档**
- [ ] **TODO: 添加速率限制防止暴力创建用户**
- [ ] **TODO: 添加安全响应头**

### 错误处理
- [x] GlobalExceptionHandler 正确处理异常
- [x] HTTP 状态码语义正确（404/403/409/400）
- [x] 前端错误提示不静默（都显示 toast）
- [x] IllegalStateException("已是成员") 返回 409

---

## 总结

### 已修复 ✅
1. 邀请链接硬编码 URL
2. 数据库索引缺失
3. H2 AUTO_SERVER 参数多余
4. 编辑冲突友好提示
5. 前端初始化竞态条件

### 上线后优先处理（按优先级排序）
1. **P1**: 健康检查端点 - 部署运维需要
2. **P1**: 日志配置优化 - 问题排查需要
3. **P1**: CSRF 保护 - 安全增强
4. **P2**: 安全响应头 - 安全增强

### 建议优化（可迭代）
1. 封装 fetch 拦截器
2. 邀请 Token 过期机制
3. 清单标题验证统一
4. 错误提示友好化
5. 移动端体验优化
6. 清单描述字段

### 需要用户确认
1. 并发场景是否需要乐观锁
2. 是否需要自动备份脚本
3. 是否需要迁移到 Cookie
4. 邀请链接有效期（7天/永久）
5. 删除确认是否需要更强机制
6. 数据库目录权限确认
7. 日志保留策略确认（30天）

---

## 修复记录

| 日期 | 修复内容 | 修复文件 | 状态 |
|------|---------|---------|------|
| 2026-02-19 | 邀请链接硬编码 URL | InviteController.java | ✅ |
| 2026-02-19 | 数据库索引缺失 | V3__add_indexes.sql | ✅ |
| 2026-02-19 | AUTO_SERVER 参数多余 | application-prod.yml | ✅ |
| 2026-02-19 | 编辑冲突友好提示 | list.html, style.css | ✅ |
| 2026-02-19 | 前端初始化竞态条件 | list.html, index.html, my-lists.html | ✅ |

---

**审查人**: Claude Code
**审查日期**: 2026-02-19
**项目**: 共享待办清单
