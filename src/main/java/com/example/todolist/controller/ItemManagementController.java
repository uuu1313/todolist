package com.example.todolist.controller;

import com.example.todolist.dto.ItemResponse;
import com.example.todolist.dto.UpdateItemRequest;
import com.example.todolist.entity.Priority;
import com.example.todolist.entity.TodoItem;
import com.example.todolist.entity.TodoList;
import com.example.todolist.entity.User;
import com.example.todolist.service.ItemService;
import com.example.todolist.service.MemberService;
import com.example.todolist.exception.ForbiddenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Objects;

@RestController
@RequestMapping("/api/items")
public class ItemManagementController {

    @Autowired
    private ItemService itemService;

    @Autowired
    private MemberService memberService;

    @PatchMapping("/{id}")
    public ResponseEntity<ItemResponse> updateItem(
            @PathVariable Long id,
            @RequestParam String token,
            @Valid @RequestBody UpdateItemRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        // 验证请求体至少包含一个字段
        if (!request.isValid()) {
            return ResponseEntity.badRequest().build();
        }

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

        var dueDateStr = request.getDueDate();
        TodoItem updatedItem = itemService.updateItem(
                id,
                token,
                request.getTitle(),
                request.getCompleted(),
                request.getPriority(),
                dueDateStr,
                userId
        );
        return ResponseEntity.ok(new ItemResponse(updatedItem));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long id,
            @RequestParam String token,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
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

        itemService.deleteItem(id, token);
        return ResponseEntity.noContent().build();
    }
}
