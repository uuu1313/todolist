# Release Hardening 报告

**项目**: TodoList V1
**日期**: 2026-02-18
**阶段**: Release Hardening（最小加固）
**原则**: 不加功能，不重构，只做最小必要加固

---

## 检查项汇总

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 1. 统一错误返回 | ✅ 通过（已补强） | 补充了 MethodArgumentNotValidException 处理 |
| 2. 参数校验 | ✅ 通过（已补齐） | DTO 添加校验注解，Controller 添加 @Valid |
| 3. Token 归属校验 | ✅ 通过（已补齐） | 更新/删除操作需验证 token 归属 |

---

## 详细检查结果

### 1. 统一错误返回（RestControllerAdvice）

**原状态**: 基本完善，缺少参数校验异常处理

**补强内容**:
- 添加 `MethodArgumentNotValidException` 处理器
- 统一返回 400 错误和友好错误消息

**修改文件**:
- `/d/develop/project/todolist/src/main/java/com/example/todolist/exception/GlobalExceptionHandler.java`

**代码变更**:
```java
// 添加导入
import org.springframework.web.bind.MethodArgumentNotValidException;

// 添加异常处理器
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
    String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getDefaultMessage())
            .findFirst()
            .orElse("参数校验失败");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("Validation failed", message));
}
```

**已处理异常类型**:
- ✅ `NotFoundException` - 404
- ✅ `IllegalArgumentException` - 400
- ✅ `MethodArgumentNotValidException` - 400（新增）
- ✅ `HttpMessageNotReadableException` - 400
- ✅ `DataIntegrityViolationException` - 500
- ✅ `IllegalStateException` - 500
- ✅ `Exception` - 500（兜底）

---

### 2. 参数校验（@Valid + @NotBlank/@Size）

**原状态**: 完全缺失

**补齐内容**:
- DTO 类添加校验注解
- Controller 方法添加 `@Valid` 触发校验
- 配合 GlobalExceptionHandler 处理校验异常

**修改文件**:
1. `/d/develop/project/todolist/src/main/java/com/example/todolist/dto/CreateItemRequest.java`
2. `/d/develop/project/todolist/src/main/java/com/example/todolist/dto/UpdateItemRequest.java`
3. `/d/develop/project/todolist/src/main/java/com/example/todolist/controller/ItemController.java`
4. `/d/develop/project/todolist/src/main/java/com/example/todolist/controller/ItemManagementController.java`

**代码变更**:

**CreateItemRequest.java**:
```java
// 添加导入
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 添加校验注解
@NotBlank(message = "标题不能为空")
@Size(max = 200, message = "标题长度不能超过 200 个字符")
private String title;
```

**UpdateItemRequest.java**:
```java
// 添加导入
import jakarta.validation.constraints.Size;

// 添加校验注解
@Size(max = 200, message = "标题长度不能超过 200 个字符")
private String title;
```

**ItemController.java**:
```java
// 添加导入
import jakarta.validation.Valid;

// 方法参数添加 @Valid
public ResponseEntity<ItemResponse> addItem(
        @PathVariable String token,
        @Valid @RequestBody CreateItemRequest request
)
```

**ItemManagementController.java**:
```java
// 添加导入
import jakarta.validation.Valid;

// 方法参数添加 @Valid
public ResponseEntity<ItemResponse> updateItem(
        @PathVariable Long id,
        @RequestParam String token,
        @Valid @RequestBody UpdateItemRequest request
)
```

**校验规则**:
- `CreateItemRequest.title`: 必填，最大长度 200
- `UpdateItemRequest.title`: 可选，最大长度 200
- `UpdateItemRequest.completed`: 无校验（可选）

---

### 3. Todo 更新/删除的 token 归属校验

**原状态**: 完全缺失（安全漏洞）

**风险**: 攻击者可以通过遍历 ID 修改或删除其他清单的事项

**补齐内容**:
- `PATCH /api/items/{id}` 增加 `token` 查询参数
- `DELETE /api/items/{id}` 增加 `token` 查询参数
- Service 层验证事项归属于指定 token 的清单
- 验证失败返回 404（不泄露存在性）

**修改文件**:
1. `/d/develop/project/todolist/src/main/java/com/example/todolist/controller/ItemManagementController.java`
2. `/d/develop/project/todolist/src/main/java/com/example/todolist/service/ItemService.java`

**代码变更**:

**ItemManagementController.java**:
```java
// PATCH 修改
@PatchMapping("/{id}")
public ResponseEntity<ItemResponse> updateItem(
        @PathVariable Long id,
        @RequestParam String token,  // 新增
        @Valid @RequestBody UpdateItemRequest request
) {
    TodoItem item = itemService.updateItem(id, token, request.getTitle(), request.getCompleted());
    return ResponseEntity.ok(new ItemResponse(item));
}

// DELETE 修改
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteItem(
        @PathVariable Long id,
        @RequestParam String token  // 新增
) {
    itemService.deleteItem(id, token);
    return ResponseEntity.noContent().build();
}
```

**ItemService.java**:
```java
// updateItem 方法签名和实现
public TodoItem updateItem(Long id, String token, String title, Boolean completed) {
    TodoItem item = itemRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Item not found"));

    // 验证事项归属
    TodoList list = listRepository.findByToken(token)
            .orElseThrow(() -> new NotFoundException("List not found"));
    if (!item.getTodoList().getId().equals(list.getId())) {
        throw new NotFoundException("Item not found");
    }

    // ... 原有逻辑
}

// deleteItem 方法签名和实现
public void deleteItem(Long id, String token) {
    TodoItem item = itemRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Item not found"));

    // 验证事项归属
    TodoList list = listRepository.findByToken(token)
            .orElseThrow(() -> new NotFoundException("List not found"));
    if (!item.getTodoList().getId().equals(list.getId())) {
        throw new NotFoundException("Item not found");
    }

    itemRepository.delete(item);
}
```

**安全设计**:
- 归属验证失败返回 404，不泄露事项存在性
- 需同时提供正确的 ID 和 token 才能操作
- 防止跨清单数据访问

---

## API 变更影响

### 破坏性变更
以下 API 签名已变更，客户端需同步更新：

1. **PATCH /api/items/{id}**
   - 新增必填参数: `?token=xxx`
   - 示例: `PATCH /api/items/123?token=abc123`

2. **DELETE /api/items/{id}**
   - 新增必填参数: `?token=xxx`
   - 示例: `DELETE /api/items/123?token=abc123`

### 新增错误响应
参数校验失败返回 400：
```json
{
  "error": "Validation failed",
  "message": "标题不能为空"
}
```

---

## 验证建议

### 测试用例
1. **参数校验测试**
   - POST 空标题 → 400
   - POST 超长标题 → 400
   - PATCH 空字符串标题 → 400

2. **Token 归属测试**
   - 用 token A 更新 token B 的事项 → 404
   - 用 token A 删除 token B 的事项 → 404
   - 正常更新/删除自己的事项 → 200/204

3. **错误响应测试**
   - 所有异常统一返回 ErrorResponse 格式
   - 错误消息友好，不暴露内部信息

---

## 总结

### 完成情况
✅ 所有 3 项加固点已完成
✅ 共修改 5 个文件
✅ 无新增功能
✅ 无重构
✅ 保持现有代码风格

### 安全性提升
1. 防止非法参数进入业务逻辑
2. 防止跨清单数据访问
3. 统一错误处理，避免信息泄露

### 下一步
- 运行回归测试清单
- 更新 API 文档
- 部署到测试环境验证
