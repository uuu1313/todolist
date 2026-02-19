# 网站图标（Favicon）配置说明

## 更新时间
2026-02-19

## 图片文件位置

### 移动前（项目根目录）
```
/d/develop/project/todolist/16.ico
/d/develop/project/todolist/32.ico
```

### 移动后（静态资源目录）
```
src/main/resources/static/icons/
├── favicon-16.ico  (544 字节，16x16 图标)
└── favicon-32.ico  (9.5K 字节，32x32 图标)
```

---

## 文件重命名说明

| 原文件名 | 新文件名 | 说明 |
|---------|---------|------|
| `16.ico` | `favicon-16.ico` | 16x16 favicon |
| `32.ico` | `favicon-32.ico` | 32x32 favicon |

**重命名原因**：
- 使用标准的命名规范（favicon-{size}.ico）
- 清晰标识图标尺寸
- 避免文件名冲突

---

## HTML 引用配置

### 修改的文件（3个）

所有 HTML 模板的 `<head>` 部分都添加了：

```html
<link rel="icon" type="image/x-icon" sizes="16x16" href="/icons/favicon-16.ico">
<link rel="icon" type="image/x-icon" sizes="32x32" href="/icons/favicon-32.ico">
```

### 修改的文件列表

1. **index.html**（首页）
```html
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>共享待办清单 - 简单快速的协作工具</title>
    <link rel="stylesheet" href="/css/style.css">
    <link rel="icon" type="image/x-icon" sizes="16x16" href="/icons/favicon-16.ico">
    <link rel="icon" type="image/x-icon" sizes="32x32" href="/icons/favicon-32.ico">
</head>
```

2. **list.html**（清单详情页）
```html
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>待办清单详情</title>
    <link rel="stylesheet" href="/css/style.css">
    <link rel="icon" type="image/x-icon" sizes="16x16" href="/icons/favicon-16.ico">
    <link rel="icon" type="image/x-icon" sizes="32x32" href="/icons/favicon-32.ico">
    ...
</head>
```

3. **my-lists.html**（我的清单页）
```html
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>我的清单 - 共享待办清单</title>
    <link rel="stylesheet" href="/css/style.css">
    <link rel="icon" type="image/x-icon" sizes="16x16" href="/icons/favicon-16.ico">
    <link rel="icon" type="image/x-icon" sizes="32x32" href="/icons/favicon-32.ico">
    ...
</head>
```

---

## 访问路径

### 静态资源映射

Spring Boot 自动将 `src/main/resources/static/` 目录映射到网站根路径：

```
src/main/resources/static/icons/favicon-16.ico
    ↓
http://localhost:8081/icons/favicon-16.ico

src/main/resources/static/icons/favicon-32.ico
    ↓
http://localhost:8081/icons/favicon-32.ico
```

### 浏览器如何使用图标

1. **16x16 图标**：用于浏览器标签页
2. **32x32 图标**：用于书签、历史记录等

浏览器会根据尺寸自动选择最合适的图标。

---

## 验证方法

### 1. 检查文件是否存在

```bash
ls -lh /d/develop/project/todolist/src/main/resources/static/icons/
```

**预期输出**：
```
-rw-r--r-- 1 user group  544 Feb 19 20:51 favicon-16.ico
-rw-r--r-- 1 user group 9.5K Feb 19 20:50 favicon-32.ico
```

### 2. 启动应用并验证

```bash
# 启动应用
cd /d/develop/project/todolist
mvn spring-boot:run

# 打开浏览器
open http://localhost:8081/

# 验证：
# 1. 查看浏览器标签页 - 应该显示自定义图标
# 2. 添加书签 - 书签图标应该显示 32x32 版本
# 3. 查看历史记录 - 历史记录图标应该显示 32x32 版本
```

### 3. 直接访问图标 URL

```bash
# 测试 16x16 图标
curl http://localhost:8081/icons/favicon-16.ico --output test16.ico

# 测试 32x32 图标
curl http://localhost:8081/icons/favicon-32.ico --output test32.ico

# 验证文件下载成功
ls -lh test*.ico
```

---

## 目录结构（更新后）

```
src/main/resources/
├── static/
│   ├── css/
│   │   └── style.css
│   └── icons/
│       ├── favicon-16.ico  ← 新增
│       └── favicon-32.ico  ← 新增
├── templates/
│   ├── index.html         ← 已修改
│   ├── list.html          ← 已修改
│   └── my-lists.html      ← 已修改
└── db/
    └── migration/
```

---

## 浏览器兼容性

### 支持的浏览器

| 浏览器 | 版本 | 支持情况 |
|--------|------|----------|
| Chrome | 所有版本 | ✅ 完全支持 |
| Firefox | 所有版本 | ✅ 完全支持 |
| Safari | 所有版本 | ✅ 完全支持 |
| Edge | 所有版本 | ✅ 完全支持 |

### 现代浏览器的 Favicon 支持

现代浏览器会自动：
1. 从 `<link rel="icon">` 标签读取图标
2. 根据页面选择最合适的尺寸
3. 缓存图标以提升性能

---

## 常见问题

### Q1: 图标没有显示在浏览器标签页

**可能原因**：
- 图标文件路径错误
- 浏览器缓存未更新
- 图标文件损坏

**解决方法**：
```bash
# 1. 清除浏览器缓存
# Chrome: Ctrl+Shift+Delete (Windows/Linux) 或 Cmd+Shift+Delete (Mac)

# 2. 强制刷新页面
# Chrome: Ctrl+F5 (Windows/Linux) 或 Cmd+Shift+R (Mac)

# 3. 验证文件路径
curl http://localhost:8081/icons/favicon-16.ico --output test.ico
```

### Q2: 想要替换图标

**步骤**：
1. 准备新的图标文件（16x16 和 32x32）
2. 重命名为 `favicon-16.ico` 和 `favicon-32.ico`
3. 替换 `src/main/resources/static/icons/` 中的文件
4. 清除浏览器缓存
5. 刷新页面

### Q3: 使用 PNG 格式代替 ICO

**当前实现**：使用 ICO 格式（推荐）
- ✅ 一个文件可以包含多个尺寸
- ✅ 所有浏览器都支持

**如果想改用 PNG**：
```html
<!-- 替换为 PNG -->
<link rel="icon" type="image/png" sizes="16x16" href="/icons/favicon-16.png">
<link rel="icon" type="image/png" sizes="32x32" href="/icons/favicon-32.png">
```

**文件命名**：
```
favicon-16.png (16x16 PNG)
favicon-32.png (32x32 PNG)
```

---

## 技术说明

### Spring Boot 静态资源映射

Spring Boot Boot 自动将 `src/main/resources/static/` 目录映射到网站根路径：

```
src/main/resources/static/icons/favicon.ico
    ↓
http://localhost:8081/icons/favicon.ico
```

### MIME 类型

`.ico` 文件的 MIME 类型：
```
image/x-icon
```

这是 favicon 的标准 MIME 类型，所有浏览器都支持。

---

## 文件清理

原文件已移动，项目根目录不再有 `16.ico` 和 `32.ico`：

```bash
# 验证原文件已删除
ls /d/develop/project/todolist/*.{ico,ICO} 2>/dev/null
# （应该没有输出）

# 验证新文件存在
ls /d/develop/project/todolist/src/main/resources/static/icons/
# （应该显示两个文件）
```

---

## 总结

**操作完成**：✅ 网站图标配置完成

**文件位置**：
- `/src/main/resources/static/icons/favicon-16.ico`
- `/src/main/resources/static/icons/favicon-32.ico`

**引用方式**：
- 所有 HTML 页面的 `<head>` 部分都添加了 `<link>` 标签
- 使用相对路径：`/icons/favicon-16.ico` 和 `/icons/favicon-32.ico`

**下一步**：
1. 编译项目：`mvn clean compile`
2. 启动应用：`mvn spring-boot:run`
3. 访问 http://localhost:8081/ 查看图标
4. 清除浏览器缓存并刷新以更新图标
