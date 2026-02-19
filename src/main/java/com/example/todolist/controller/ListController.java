package com.example.todolist.controller;

import com.example.todolist.dto.ListResponse;
import com.example.todolist.dto.UpdateListRequest;
import com.example.todolist.entity.TodoList;
import com.example.todolist.entity.MemberRole;
import com.example.todolist.entity.User;
import com.example.todolist.service.ListService;
import com.example.todolist.service.MemberService;
import com.example.todolist.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lists")
public class ListController {

    @Autowired
    private ListService listService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    @Transactional
    public ResponseEntity<ListResponse> createList(
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        TodoList list = listService.createList();

        // V2-B: 创建清单时自动添加创建者为 OWNER
        if (userId != null) {
            memberService.addMember(list.getToken(), userId, MemberRole.OWNER);
        }

        return ResponseEntity.status(201).body(new ListResponse(list));
    }

    @GetMapping("/{token}")
    public ResponseEntity<ListResponse> getList(@PathVariable String token) {
        TodoList list = listService.getListByToken(token);
        return ResponseEntity.ok(new ListResponse(list));
    }

    /**
     * 更新清单标题 (V2-A 新增)
     * PATCH /api/lists/{token}
     * P0-3: 仅 OWNER 可修改
     */
    @PatchMapping("/{token}")
    public ResponseEntity<ListResponse> updateList(
            @PathVariable String token,
            @RequestBody @Valid UpdateListRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        TodoList list = listService.getListByToken(token);

        // P0-3: 权限检查 - 只有 OWNER 可以修改
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && !memberService.isOwner(list, user)) {
                return ResponseEntity.status(403).build();
            }
        }

        TodoList updated = listService.updateListTitle(token, request.getTitle());
        return ResponseEntity.ok(new ListResponse(updated));
    }

    /**
     * 删除清单 (V2-A 新增)
     * DELETE /api/lists/{token}
     * P0-3: 仅 OWNER 可删除
     */
    @DeleteMapping("/{token}")
    public ResponseEntity<Void> deleteList(
            @PathVariable String token,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        TodoList list = listService.getListByToken(token);

        // P0-3: 权限检查 - 只有 OWNER 可以删除
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && !memberService.isOwner(list, user)) {
                return ResponseEntity.status(403).build();
            }
        }

        listService.deleteList(token);
        return ResponseEntity.noContent().build();
    }
}
