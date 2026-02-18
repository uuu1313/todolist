# 排序规则回归测试用例

**测试范围**: SORTING_SPEC.md 中定义的排序规则
**测试日期**: 2026-02-18
**状态**: 待执行

---

## 测试准备

### 前置条件
1. 应用已重启并加载新代码
2. 创建测试清单
3. 准备测试数据

### 测试环境
- Base URL: http://localhost:8081
- 端点: /api/lists/{token}/items

---

## 测试用例

### TC-SORT-01: 完成状态优先

**目标**: 验证未完成事项在前，已完成事项在后

**步骤**:
1. 创建 3 个事项：
   - 事项 A: "未完成任务", priority=HIGH
   - 事项 B: "已完成任务", priority=HIGH, completed=true
   - 事项 C: "未完成任务", priority=MEDIUM
2. 调用 GET /api/lists/{token}/items

**预期结果**:
- 顺序: 事项 A (未完成 HIGH) → 事项 C (未完成 MEDIUM) → 事项 B (已完成 HIGH)
- 验证: 所有未完成事项在已完成事项之前

**验证点**:
- [ ] 未完成事项在前 2 个位置
- [ ] 已完成事项在最后
- [ ] 同完成状态下按优先级排序

---

### TC-SORT-02: 优先级排序

**目标**: 验证 HIGH > MEDIUM > LOW

**步骤**:
1. 创建 3 个未完成事项，无截止日期：
   - 事项 A: "低优先级", priority=LOW
   - 事项 B: "高优先级", priority=HIGH
   - 事项 C: "中优先级", priority=MEDIUM
2. 调用 GET /api/lists/{token}/items

**预期结果**:
- 顺序: 事项 B (HIGH) → 事项 C (MEDIUM) → 事项 A (LOW)

**验证点**:
- [ ] HIGH 优先级在第 1 位
- [ ] MEDIUM 优先级在第 2 位
- [ ] LOW 优先级在第 3 位

---

### TC-SORT-03: 截止日期排序

**目标**: 验证截止日期升序，null 在后

**步骤**:
1. 创建 3 个未完成、HIGH 优先级事项：
   - 事项 A: "截止 2026-02-25", dueDate=2026-02-25
   - 事项 B: "无截止日期", dueDate=null
   - 事项 C: "截止 2026-02-19", dueDate=2026-02-19
2. 调用 GET /api/lists/{token}/items

**预期结果**:
- 顺序: 事项 C (02-19) → 事项 A (02-25) → 事项 B (null)

**验证点**:
- [ ] 有截止日期的在前
- [ ] 截止日期早的在前
- [ ] null 截止日期在最后

---

### TC-SORT-04: 兜底规则 (updatedAt)

**目标**: 验证 updatedAt DESC 兜底

**步骤**:
1. 创建 2 个未完成、HIGH 优先级、无截止日期事项：
   - 事项 A: 先创建，后更新
   - 事项 B: 后创建，先更新
2. 调用 GET /api/lists/{token}/items

**预期结果**:
- 顺序: 事项 B (最新更新) → 事项 A (较早更新)

**验证点**:
- [ ] 最新更新的在前
- [ ] 较早更新的在后

---

### TC-SORT-05: 优先级 > 截止日期

**目标**: 验证优先级优先于截止日期

**步骤**:
1. 创建 2 个未完成事项：
   - 事项 A: "高优先级晚截止", priority=HIGH, dueDate=2026-02-25
   - 事项 B: "中优先级早截止", priority=MEDIUM, dueDate=2026-02-19
2. 调用 GET /api/lists/{token}/items

**预期结果**:
- 顺序: 事项 A (HIGH) → 事项 B (MEDIUM)

**验证点**:
- [ ] 高优先级在前（即使截止日期更晚）
- [ ] 优先级规则高于截止日期规则

---

### TC-SORT-06: 完成状态 > 优先级

**目标**: 验证完成状态优先于优先级

**步骤**:
1. 创建 2 个事项：
   - 事项 A: "低优先级未完成", priority=LOW, completed=false
   - 事项 B: "高优先级已完成", priority=HIGH, completed=true
2. 调用 GET /api/lists/{token}/items

**预期结果**:
- 顺序: 事项 A (未完成) → 事项 B (已完成)

**验证点**:
- [ ] 未完成在前（即使优先级低）
- [ ] 已完成沉底（即使优先级高）

---

### TC-SORT-07: 动态排序验证

**目标**: 验证事项完成后自动沉底

**步骤**:
1. 创建 3 个未完成事项：
   - 事项 A: "任务A", priority=HIGH
   - 事项 B: "任务B", priority=MEDIUM
   - 事项 C: "任务C", priority=LOW
2. 调用 GET /api/lists/{token}/items，记录初始顺序
3. 标记事项 B 为完成
4. 再次调用 GET /api/lists/{token}/items

**预期结果**:
- 初始顺序: A → B → C
- 完成后顺序: A → C → B (B 沉底)

**验证点**:
- [ ] 初始顺序正确
- [ ] 完成后事项自动沉底
- [ ] 其他未完成事项顺序保持

---

### TC-SORT-08: 多事项综合排序

**目标**: 验证复杂场景下的排序

**步骤**:
1. 按以下顺序创建 5 个事项：
   - 事项 1: LOW, null, 未完成
   - 事项 2: HIGH, 2026-02-25, 未完成
   - 事项 3: MEDIUM, 2026-02-20, 未完成
   - 事项 4: HIGH, null, 已完成
   - 事项 5: MEDIUM, 2026-02-15, 未完成
2. 调用 GET /api/lists/{token}/items

**预期结果**:
- 顺序: 事项 2 → 事项 3 → 事项 5 → 事项 1 → 事项 4

**排序逻辑**:
- 事项 2 (未完成, HIGH, 02-25)
- 事项 3 (未完成, MEDIUM, 02-20)
- 事项 5 (未完成, MEDIUM, 02-15)
- 事项 1 (未完成, LOW, null)
- 事项 4 (已完成, HIGH, 02-xx)

**验证点**:
- [ ] 未完成事项在前
- [ ] 未完成中按优先级分组
- [ ] 同优先级内按截止日期排序
- [ ] null 截止日期在同优先级最后
- [ ] 已完成事项在最后

---

## 测试执行清单

### 执行前检查
- [ ] 应用已重启
- [ ] 端口 8081 可访问
- [ ] 数据库已清空（可选）

### 执行记录

| 用例 ID | 状态 | 备注 |
|---------|------|------|
| TC-SORT-01 | ⬜ 待执行 | |
| TC-SORT-02 | ⬜ 待执行 | |
| TC-SORT-03 | ⬜ 待执行 | |
| TC-SORT-04 | ⬜ 待执行 | |
| TC-SORT-05 | ⬜ 待执行 | |
| TC-SORT-06 | ⬜ 待执行 | |
| TC-SORT-07 | ⬜ 待执行 | |
| TC-SORT-08 | ⬜ 待执行 | |

---

## 预期测试结果

### 通过标准

- ✅ 所有 8 个测试用例通过
- ✅ 无功能回退
- ✅ 排序规则完全符合 SORTING_SPEC.md

### 失败标准

- ❌ 任何一个用例失败
- ❌ 排序规则不符合规范
- ❌ 功能回退

---

## 附录：curl 测试命令

### 快速测试脚本

```bash
# 创建清单
TOKEN=$(curl -s -X POST http://localhost:8081/api/lists -H "Content-Type: application/json" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# 添加测试事项
curl -X POST http://localhost:8081/api/lists/$TOKEN/items -H "Content-Type: application/json" -d '{"title":"Low Priority","priority":"LOW"}'
curl -X POST http://localhost:8081/api/lists/$TOKEN/items -H "Content-Type: application/json" -d '{"title":"High Priority","priority":"HIGH"}'
curl -X POST http://localhost:8081/api/lists/$TOKEN/items -H "Content-Type: application/json" -d '{"title":"Medium Priority","priority":"MEDIUM","dueDate":"2026-02-25"}'

# 获取排序后的事项列表
curl -s http://localhost:8081/api/lists/$TOKEN/items | jq '.[] | {id, title, completed, priority, dueDate}'
```

---

**文档版本**: 1.0
**维护者**: QA Team
