package com.example.todolist.controller;

import com.example.todolist.dto.ItemResponse;
import com.example.todolist.dto.UpdateItemRequest;
import com.example.todolist.entity.TodoItem;
import com.example.todolist.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/items")
public class ItemManagementController {

    @Autowired
    private ItemService itemService;

    @PatchMapping("/{id}")
    public ResponseEntity<ItemResponse> updateItem(
            @PathVariable Long id,
            @RequestParam String token,
            @Valid @RequestBody UpdateItemRequest request
    ) {
        TodoItem item = itemService.updateItem(id, token, request.getTitle(), request.getCompleted());
        return ResponseEntity.ok(new ItemResponse(item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long id,
            @RequestParam String token
    ) {
        itemService.deleteItem(id, token);
        return ResponseEntity.noContent().build();
    }
}
