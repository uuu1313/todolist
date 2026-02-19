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

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request
    ) {
        try {
            User user = userService.updateUser(id, request.getUsername());
            return ResponseEntity.ok(new UserResponse(user));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
