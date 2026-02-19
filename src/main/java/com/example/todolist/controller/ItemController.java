package com.example.todolist.controller;

import com.example.todolist.dto.CreateItemRequest;
import com.example.todolist.dto.ItemResponse;
import com.example.todolist.entity.TodoItem;
import com.example.todolist.entity.TodoList;
import com.example.todolist.entity.User;
import com.example.todolist.service.ItemService;
import com.example.todolist.service.MemberService;
import com.example.todolist.exception.ForbiddenException;
import com.example.todolist.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/lists/{token}/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    @Autowired
    private MemberService memberService;

    @PostMapping
    public ResponseEntity<ItemResponse> addItem(
            @PathVariable String token,
            @Valid @RequestBody CreateItemRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        // Get list and user
        TodoList list = itemService.getListByToken(token);

        // Check if user is member
        if (userId != null) {
            User user = itemService.getUserById(userId);
            if (user != null && !memberService.isMember(list, user)) {
                throw new ForbiddenException("只有清单成员可以执行此操作");
            }
        }

        var priority = request.getPriority();
        var dueDate = request.getDueDateAsLocalDate();

        TodoItem item = itemService.addItem(token, request.getTitle(), priority, dueDate, userId);
        return ResponseEntity.status(201).body(new ItemResponse(item));
    }

    @GetMapping
    public ResponseEntity<List<ItemResponse>> getItems(@PathVariable String token) {
        List<TodoItem> items = itemService.getItemsByToken(token);
        List<ItemResponse> responses = items.stream()
                .map(ItemResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
