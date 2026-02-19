# VISITOR Permission Model Implementation Summary

## Overview
Successfully implemented the VISITOR permission model for the TodoList application. Users not in the `list_member` table are now treated as VISITORs with read-only access.

## Changes Made

### 1. MemberService.java
**File**: `D:\develop\project\todolist\src\main\java\com\example\todolist\service\MemberService.java`

**Added Methods**:
```java
/**
 * 检查用户是否是访客 (不在 list_member 表中)
 */
@Transactional(readOnly = true)
public boolean isVisitor(TodoList list, User user) {
    // User is VISITOR if not in list_member
    return !existsByListAndUser(list, user);
}

/**
 * 检查用户是否存在于指定清单的成员中
 */
@Transactional(readOnly = true)
public boolean existsByListAndUser(TodoList list, User user) {
    return memberRepository.existsByListAndUser(list, user);
}
```

### 2. ItemService.java
**File**: `D:\develop\project\todolist\src\main\java\com\example\todolist\service\ItemService.java`

**Added Helper Methods**:
```java
@Transactional(readOnly = true)
public TodoList getListByToken(String token) {
    return listRepository.findByToken(token)
            .orElseThrow(() -> new NotFoundException("List not found"));
}

@Transactional(readOnly = true)
public User getUserById(Long userId) {
    return userRepository.findById(userId).orElse(null);
}

@Transactional(readOnly = true)
public TodoItem getItemById(Long id) {
    return itemRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Item not found"));
}
```

### 3. ItemController.java
**File**: `D:\develop\project\todolist\src\main\java\com\example\todolist\controller\ItemController.java`

**Modified**: POST `/api/lists/{token}/items`
- Added member permission check before creating item
- Throws `ForbiddenException("只有清单成员可以执行此操作")` if user is VISITOR
- VISITORs can still read items via GET endpoint (no change needed)

**Key Code**:
```java
@Autowired
private MemberService memberService;

@PostMapping
public ResponseEntity<ItemResponse> addItem(...) {
    // Get list and user
    TodoList list = itemService.getListByToken(token);

    // Check if user is member
    if (userId != null) {
        User user = itemService.getUserById(userId);
        if (user != null && !memberService.isMember(list, user)) {
            throw new ForbiddenException("只有清单成员可以执行此操作");
        }
    }

    // Proceed to create item...
}
```

### 4. ItemManagementController.java
**File**: `D:\develop\project\todolist\src\main\java\com\example\todolist\controller\ItemManagementController.java`

**Modified**: PATCH `/api/items/{id}` and DELETE `/api/items/{id}`
- Added member permission check before update/delete operations
- Throws `ForbiddenException("只有清单成员可以执行此操作")` if user is VISITOR

**Key Code**:
```java
@Autowired
private MemberService memberService;

@PatchMapping("/{id}")
public ResponseEntity<ItemResponse> updateItem(...) {
    // Get todo item and extract list
    TodoItem item = itemService.getItemById(id);
    TodoList list = item.getList();

    // Check if user is member
    if (userId != null) {
        User user = itemService.getUserById(userId);
        if (user != null && !memberService.isMember(list, user)) {
            throw new ForbiddenException("只有清单成员可以执行此操作");
        }
    }

    // Proceed to update item...
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteItem(...) {
    // Get todo item and extract list
    TodoItem item = itemService.getItemById(id);
    TodoList list = item.getList();

    // Check if user is member
    if (userId != null) {
        User user = itemService.getUserById(userId);
        if (user != null && !memberService.isMember(list, user)) {
            throw new ForbiddenException("只有清单成员可以执行此操作");
        }
    }

    // Proceed to delete item...
}
```

## API Behavior Summary

| Role | Read Items | Create Item | Update Item | Delete Item |
|------|-----------|-------------|-------------|-------------|
| VISITOR | 200 OK | 403 Forbidden | 403 Forbidden | 403 Forbidden |
| MEMBER | 200 OK | 201 Created | 200 OK | 204 No Content |
| OWNER | 200 OK | 201 Created | 200 OK | 204 No Content |

## Test Coverage

Created comprehensive test suite: `VisitorPermissionServiceTest.java`

**Test Cases**:
1. ✅ `testIsMemberMethod` - Verify member detection
2. ✅ `testIsVisitorMethod` - Verify visitor detection
3. ✅ `testOwnerCanCreateItem` - Owner can create items
4. ✅ `testVisitorCannotCreateItem_ThroughServiceLogic` - Visitor blocked from creating
5. ✅ `testMemberCanCreateAfterBeingAdded` - Added user becomes member
6. ✅ `testRemovedMemberBecomesVisitor` - Removed member becomes visitor
7. ✅ `testOwnerIsNotVisitor` - Owner is not a visitor
8. ✅ `testExistsByListAndUser` - Helper method works correctly
9. ✅ `testPermissionCheckLogic` - Permission check logic verification
10. ✅ `testItemOperationsWithPermissionChecks` - Update permissions
11. ✅ `testDeleteItemWithPermissionChecks` - Delete permissions

**All 11 tests passing ✅**

## Verification

Build Status: ✅ SUCCESS
```
mvn clean compile -DskipTests
BUILD SUCCESS
```

Test Status: ✅ SUCCESS
```
mvn test -Dtest=VisitorPermissionServiceTest
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Key Design Decisions

1. **Permission Check Location**: Permission checks are implemented in controllers, not services. This follows separation of concerns - services handle business logic, controllers handle access control.

2. **Null Safety**: Permission checks only apply when `userId` is provided in headers. If `userId` is null, no permission check is performed (backward compatibility).

3. **Visitor Detection**: A user is a VISITOR if they are NOT in the `list_member` table. This is checked using `!memberRepository.existsByListAndUser(list, user)`.

4. **Read Access**: VISITORs can read items (GET `/api/lists/{token}/items`) without any permission check - this is intentional as lists should be viewable by non-members.

## Next Steps

The VISITOR permission model is now fully implemented and tested. The following tasks remain:

- P0-2: 角色显示 UI (Role display UI)
- P0-3: 清单编辑删除权限 (List edit/delete permissions)
- P1-1: 轮询同步机制 (Polling sync mechanism)
- P2-1: JPA 乐观锁防并发冲突 (JPA optimistic locking)
