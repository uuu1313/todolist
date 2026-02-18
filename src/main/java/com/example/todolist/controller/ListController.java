package com.example.todolist.controller;

import com.example.todolist.dto.ListResponse;
import com.example.todolist.entity.TodoList;
import com.example.todolist.service.ListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lists")
public class ListController {

    @Autowired
    private ListService listService;

    @PostMapping
    public ResponseEntity<ListResponse> createList() {
        TodoList list = listService.createList();
        return ResponseEntity.status(201).body(new ListResponse(list));
    }

    @GetMapping("/{token}")
    public ResponseEntity<ListResponse> getList(@PathVariable String token) {
        TodoList list = listService.getListByToken(token);
        return ResponseEntity.ok(new ListResponse(list));
    }
}
