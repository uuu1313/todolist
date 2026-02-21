package com.example.todolist.controller;

import com.example.todolist.dto.ListResponse;
import com.example.todolist.dto.UpdateListRequest;
import com.example.todolist.dto.VersionResponse;
import com.example.todolist.entity.TodoList;
import com.example.todolist.entity.MemberRole;
import com.example.todolist.entity.User;
import com.example.todolist.service.ListService;
import com.example.todolist.service.MemberService;
import com.example.todolist.repository.UserRepository;
import com.example.todolist.exception.ForbiddenException;
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
        // 强制校验：必须提供用户 ID
        if (userId == null) {
            throw new IllegalArgumentException("X-User-Id is required");
        }

        // 验证用户是否存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 创建清单并立即建立 OWNER 关系（同一事务）
        TodoList list = listService.createList();
        memberService.addMember(list.getToken(), userId, MemberRole.OWNER);

        return ResponseEntity.status(201).body(new ListResponse(list));
    }

    @GetMapping("/{token}")
    public ResponseEntity<ListResponse> getList(@PathVariable String token) {
        TodoList list = listService.getListByToken(token);
        return ResponseEntity.ok(new ListResponse(list));
    }

    /**
     * 获取清单版本号
     * 用于前端轮询检测清单是否有更新
     * GET /api/lists/{token}/version
     */
    @GetMapping("/{token}/version")
    public ResponseEntity<VersionResponse> getVersion(@PathVariable String token) {
        TodoList list = listService.getListByToken(token);
        // 使用 updatedAt 的毫秒时间戳作为版本号
        long version = list.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        return ResponseEntity.ok(new VersionResponse(version));
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
        // 强制校验：必须提供用户 ID
        if (userId == null) {
            throw new IllegalArgumentException("X-User-Id is required");
        }

        TodoList list = listService.getListByToken(token);

        // 验证用户是否存在并检查权限
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!memberService.isOwner(list, user)) {
            throw new ForbiddenException("Only OWNER can update the list");
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
        // 强制校验：必须提供用户 ID
        if (userId == null) {
            throw new IllegalArgumentException("X-User-Id is required");
        }

        TodoList list = listService.getListByToken(token);

        // 验证用户是否存在并检查权限
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!memberService.isOwner(list, user)) {
            throw new ForbiddenException("Only OWNER can delete the list");
        }

        listService.deleteList(token);
        return ResponseEntity.noContent().build();
    }
}
