# Todo List 排序规则规范

**版本**: 1.0
**日期**: 2026-02-18
**状态**: 生效

---

## 排序规则

### 优先级顺序（按顺序应用）

1. **完成状态** (Primary)
   - `completed = false` 在前
   - `completed = true` 在后（沉底）

2. **优先级** (Secondary)
   - 同完成状态下，按优先级排序：
   - `HIGH` > `MEDIUM` > `LOW`

3. **截止日期** (Tertiary)
   - 同优先级下，按截止日期升序（越早越前）
   - `dueDate = null` 放最后

4. **兜底规则** (Quaternary)
   - 同截止日期下（包括都为 null），按 `updatedAt` 降序（最新在前）
   - 如果 `updatedAt` 相同，按 `id` 降序（最新创建在前）

---

## SQL 实现

### Order By 子句

```sql
ORDER BY
  completed ASC,           -- 未完成在前
  priority DESC,            -- HIGH > MEDIUM > LOW
  due_date ASC NULLS LAST,  -- 截止日期升序，null 最后
  updated_at DESC,          -- 最新更新在前
  id DESC                   -- 最新创建在前（兜底）
```

### 优先级枚举值

```java
HIGH = 3   // 数值最大，排序时 DESC 让 HIGH 在前
MEDIUM = 2
LOW = 1
```

---

## 示例数据

### 场景 1: 不同完成状态

| ID | Title | Completed | Priority | DueDate | 顺序 | 说明 |
|----|-------|-----------|----------|---------|------|------|
| 1 | 任务A | false | HIGH | 2026-02-20 | 1 | 未完成 + 高优先级 + 有截止日期 |
| 2 | 任务B | true | HIGH | 2026-02-20 | 2 | 已完成（沉底） |

**排序**: 任务1 → 任务2（未完成在前）

### 场景 2: 同完成状态，不同优先级

| ID | Title | Completed | Priority | DueDate | 顺序 | 说明 |
|----|-------|-----------|----------|---------|------|------|
| 1 | 任务A | false | HIGH | 2026-02-25 | 1 | 高优先级 |
| 2 | 任务B | false | MEDIUM | 2026-02-20 | 2 | 中优先级（即使截止更早） |
| 3 | 任务C | false | LOW | 2026-02-15 | 3 | 低优先级 |

**排序**: 任务A → 任务B → 任务C（优先级优先）

### 场景 3: 同完成状态 + 同优先级，不同截止日期

| ID | Title | Completed | Priority | DueDate | 顺序 | 说明 |
|----|-------|-----------|----------|---------|------|------|
| 1 | 任务A | false | HIGH | 2026-02-15 | 1 | 截止最早 |
| 2 | 任务B | false | HIGH | 2026-02-20 | 2 | 截止较晚 |
| 3 | 任务C | false | HIGH | null | 3 | 无截止日期 |

**排序**: 任务A → 任务B → 任务C（截止日期升序，null 最后）

### 场景 4: 同完成状态 + 同优先级 + 同截止日期（都为 null）

| ID | Title | Completed | Priority | DueDate | UpdatedAt | 顺序 | 说明 |
|----|-------|-----------|----------|---------|-----------|------|------|
| 1 | 任务A | false | HIGH | null | 2026-02-18 10:05 | 2 | 较早更新 |
| 2 | 任务B | false | HIGH | null | 2026-02-18 10:10 | 1 | 最晚更新 |

**排序**: 任务B → 任务A（updatedAt 降序）

---

## Java 实现

### Repository 查询方法

```java
// TodoItemRepository.java
@Query("SELECT i FROM TodoItem i WHERE i.list.id = :listId " +
       "ORDER BY i.completed ASC, " +
       "i.priority DESC, " +
       "i.dueDate ASC NULLS LAST, " +
       "i.updatedAt DESC, " +
       "i.id DESC")
List<TodoItem> findByListIdOrderByPriority(@Param("listId") Long listId);
```

### Service 层调用

```java
// ItemService.java
@Transactional(readOnly = true)
public List<TodoItem> getItemsByToken(String token) {
    TodoList list = listRepository.findByToken(token)
            .orElseThrow(() -> new NotFoundException("List not found"));
    return itemRepository.findByListIdOrderByPriority(list.getId());
}
```

---

## API 响应

### GET /api/lists/{token}/items

响应数组已按规则排序：

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

**说明**:
1. 未完成事项在前（1-3），已完成事项在后（4）
2. 未完成中按优先级排序（HIGH > MEDIUM > LOW）
3. 同优先级按截止日期排序（有日期在前，null 在后）
4. 兜底规则未触发（上述规则已区分）

---

## 前端显示

### 无需额外排序

- 前端直接显示后端返回的数组顺序
- 不在前端进行排序操作
- 保持顺序一致性

### 动态更新

- 添加事项后重新获取列表
- 更新事项后重新获取列表
- 标记完成/未完成自动沉底/浮顶

---

## 测试用例

### 基本排序测试

1. **完成状态测试**
   - 创建 3 个事项，1 个完成
   - 验证：未完成在前，已完成在后

2. **优先级测试**
   - 创建 3 个未完成事项，优先级不同
   - 验证：HIGH > MEDIUM > LOW

3. **截止日期测试**
   - 创建 3 个未完成、高优先级事项，截止日期不同
   - 验证：截止早的在前，null 在后

4. **兜底规则测试**
   - 创建 2 个未完成、高优先级、无截止日期事项
   - 验证：updatedAt 最新的在前

### 组合场景测试

5. **跨优先级截止日期**
   - 高优先级 + 晚截止 vs 中优先级 + 早截止
   - 验证：高优先级在前（优先级 > 截止日期）

6. **已事项沉底**
   - 高优先级已完成 vs 低优先级未完成
   - 验证：未完成在前（完成状态 > 优先级）

---

## 向后兼容性

### API 兼容
- ✅ API 端点不变
- ✅ 响应格式不变
- ✅ 仅改变数组顺序

### 前端兼容
- ✅ 前端无需修改
- ✅ 直接显示后端返回顺序

---

## 维护说明

### 修改规则
如需调整排序规则，需：
1. 更新此文档
2. 修改 SQL Order By 子句
3. 更新测试用例
4. 通知前端团队（如有变化）

### 版本历史
- v1.0 (2026-02-18): 初始版本

---

**规范所有者**: TechLead
**审核状态**: ✅ 已批准
