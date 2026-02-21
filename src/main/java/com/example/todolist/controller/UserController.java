package com.example.todolist.controller;

import com.example.todolist.dto.CreateUserRequest;
import com.example.todolist.dto.UpdateUserRequest;
import com.example.todolist.dto.UserResponse;
import com.example.todolist.entity.User;
import com.example.todolist.service.UserService;
import com.example.todolist.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @RequestBody(required = false) CreateUserRequest request
    ) {
        User user;
        if (request != null && request.getUsername() != null) {
            user = userService.createUser(request.getUsername());
        } else {
            user = userService.createAnonymousUser();
        }
        return ResponseEntity.status(201).body(new UserResponse(user));
    }

    /**
     * 获取用户信息（用于验证用户是否存在）
     * V2-B: 前端自愈机制 - 验证 localStorage 中的 user_id 是否有效
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(new UserResponse(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 更新用户名
     * P1-1: 防止水平越权 - 只允许用户修改自己的信息
     */
    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        // 强制校验：必须提供用户 ID
        if (userId == null) {
            throw new IllegalArgumentException("X-User-Id is required");
        }

        // 防止水平越权：只允许用户修改自己的信息
        if (!userId.equals(id)) {
            throw new com.example.todolist.exception.ForbiddenException("只能修改自己的用户信息");
        }

        try {
            User user = userService.updateUser(id, request.getUsername());
            return ResponseEntity.ok(new UserResponse(user));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
