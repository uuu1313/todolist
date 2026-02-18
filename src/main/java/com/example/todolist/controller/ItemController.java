package com.example.todolist.controller;

import com.example.todolist.dto.CreateItemRequest;
import com.example.todolist.dto.ItemResponse;
import com.example.todolist.entity.TodoItem;
import com.example.todolist.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lists/{token}/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    @PostMapping
    public ResponseEntity<ItemResponse> addItem(
            @PathVariable String token,
            @Valid @RequestBody CreateItemRequest request
    ) {
        TodoItem item = itemService.addItem(token, request.getTitle());
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
