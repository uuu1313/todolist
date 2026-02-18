# 排序规则实现报告

**实现日期**: 2026-02-18
**版本**: 1.0
**状态**: ✅ 代码已实现，待重启应用测试

---

## 实现内容

### 排序规则

按照 SORTING_SPEC.md 规范实现以下优先级：

1. **完成状态**: `completed ASC` - 未完成在前
2. **优先级**: `priority DESC` - HIGH > MEDIUM > LOW
3. **截止日期**: `dueDate ASC NULLS LAST` - 越早越前，null 最后
4. **兜底**: `updatedAt DESC`, `id DESC` - 最新在前

---

## 代码修改

### 1. Repository 层

**文件**: `src/main/java/com/example/todolist/repository/TodoItemRepository.java`

**修改内容**:
- 删除旧方法: `findByListIdOrderByCompletedAscAndCreatedAtAsc()`
- 新增方法: `findByListId()`，使用 JPQL @Query 实现排序

**新查询**:
```java
@Query("SELECT ti FROM TodoItem ti WHERE ti.list.id = :listId " +
       "ORDER BY ti.completed ASC, " +
       "ti.priority DESC, " +
       "ti.dueDate ASC NULLS LAST, " +
       "ti.updatedAt DESC, " +
       "ti.id DESC")
List<TodoItem> findByListId(@Param("listId") Long listId);
```

### 2. Service 层

**文件**: `src/main/java/com/example/todolist/service/ItemService.java`

**修改内容**:
- 更新 `getItemsByToken()` 方法调用新的 Repository 方法

**修改前**:
```java
return itemRepository.findByListIdOrderByCompletedAscAndCreatedAtAsc(list.getId());
```

**修改后**:
```java
return itemRepository.findByListId(list.getId());
```

---

## SQL 查询

### 生成的 SQL

```sql
SELECT ti.* FROM todo_item ti
WHERE ti.list_id = :listId
ORDER BY
  ti.completed ASC,           -- 未完成在前
  ti.priority DESC,            -- HIGH > MEDIUM > LOW
  ti.due_date ASC NULLS LAST,  -- 截止日期升序，null 最后
  ti.updated_at DESC,          -- 最新更新在前
  ti.id DESC                   -- 最新创建在前（兜底）
```

### 优先级映射

| 枚举值 | SQL 值 | DESC 排序顺序 |
|--------|--------|--------------|
| HIGH | HIGH | 1 (最前) |
| MEDIUM | MEDIUM | 2 |
| LOW | LOW | 3 (最后) |

---

## API 影响

### GET /api/lists/{token}/items

**变化**: 仅数组顺序改变，格式不变

**示例响应**:
```json
[
  {
    "id": 3,
    "title": "紧急任务",
    "completed": false,
    "priority": "HIGH",
    "dueDate": "2026-02-19",
    "createdAt": "2026-02-18T09:00:00",
    "updatedAt": "2026-02-18T10:00:00"
  },
  {
    "id": 1,
    "title": "普通任务",
    "completed": false,
    "priority": "MEDIUM",
    "dueDate": "2026-02-20",
    "createdAt": "2026-02-18T08:00:00",
    "updatedAt": "2026-02-18T09:00:00"
  },
  {
    "id": 2,
    "title": "低优先级任务",
    "completed": false,
    "priority": "LOW",
    "dueDate": null,
    "createdAt": "2026-02-18T07:00:00",
    "updatedAt": "2026-02-18T08:00:00"
  },
  {
    "id": 4,
    "title": "已完成任务",
    "completed": true,
    "priority": "HIGH",
    "dueDate": "2026-02-18",
    "createdAt": "2026-02-18T11:00:00",
    "updatedAt": "2026-02-18T12:00:00"
  }
]
```

**排序说明**:
1. 未完成事项（1-3）在前，已完成事项（4）在后
2. 未完成中按优先级排序（HIGH > MEDIUM > LOW）
3. 同优先级按截止日期排序（有日期在前，null 在后）

---

## 测试计划

### 单元测试场景

1. **完成状态优先**
   - 创建 3 个事项，1 个完成
   - 预期：未完成在前，已完成在后

2. **优先级排序**
   - 创建 3 个未完成事项，优先级不同
   - 预期：HIGH > MEDIUM > LOW

3. **截止日期排序**
   - 创建 3 个未完成、高优先级事项，截止日期不同
   - 预期：截止早的在前，null 在后

4. **兜底规则**
   - 创建 2 个未完成、高优先级、无截止日期事项
   - 预期：updatedAt 最新的在前

### 组合场景

5. **优先级 > 截止日期**
   - 高优先级 + 晚截止 vs 中优先级 + 早截止
   - 预期：高优先级在前

6. **完成状态 > 优先级**
   - 高优先级已完成 vs 低优先级未完成
   - 预期：未完成在前

---

## 向后兼容性

### ✅ 完全兼容

- API 端点不变
- 响应格式不变
- 仅改变数组顺序
- 前端无需修改

---

## 部署说明

### 需要重启应用

由于修改了 Repository 查询方法，需要重启 Spring Boot 应用才能生效：

```bash
cd /d/develop/project/todolist
# 停止旧进程
taskkill //F //IM java.exe

# 启动新应用
./mvnw spring-boot:run
```

### 验证步骤

1. 创建清单
2. 添加多个不同优先级和截止日期的事项
3. 标记某个事项为完成
4. 调用 `GET /api/lists/{token}/items`
5. 验证事项顺序符合规范

---

## 文件清单

**修改文件** (2 个):
- `TodoItemRepository.java` - 更新查询方法
- `ItemService.java` - 更新方法调用

**新增文档** (1 个):
- `SORTING_SPEC.md` - 排序规范文档

**实现报告**:
- `SORTING_IMPLEMENTATION.md` - 本文档

---

## 下一步

- [ ] 重启应用
- [ ] 执行测试用例
- [ ] QA 验证排序功能
- [ ] 更新 REGRESSION_TEST_V2A.md 添加排序测试

---

**实现状态**: ✅ 代码已完成
**测试状态**: ⏳ 待应用重启后验证
