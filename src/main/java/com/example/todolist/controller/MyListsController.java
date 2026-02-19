package com.example.todolist.controller;

import com.example.todolist.dto.MyListResponse;
import com.example.todolist.entity.ListMember;
import com.example.todolist.entity.TodoList;
import com.example.todolist.entity.User;
import com.example.todolist.repository.ListMemberRepository;
import com.example.todolist.repository.TodoListRepository;
import com.example.todolist.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 我的清单 API
 */
@RestController
@RequestMapping("/api/my")
public class MyListsController {

    @Autowired
    private ListMemberRepository listMemberRepository;

    @Autowired
    private TodoListRepository todoListRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取当前用户相关的所有清单
     * GET /api/my/lists
     * Header: X-User-Id
     */
    @GetMapping("/lists")
    public ResponseEntity<List<MyListResponse>> getMyLists(
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        if (userId == null) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        // 验证用户存在
        if (!userRepository.existsById(userId)) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        // 查询用户加入的所有清单关系
        List<ListMember> memberships = listMemberRepository.findByUserId(userId);

        // 转换为 DTO
        List<MyListResponse> response = new ArrayList<>();
        for (ListMember membership : memberships) {
            TodoList list = membership.getList();
            if (list != null) {
                response.add(new MyListResponse(list, membership.getRole()));
            }
        }

        return ResponseEntity.ok(response);
    }
}
