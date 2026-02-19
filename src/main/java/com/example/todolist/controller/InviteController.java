package com.example.todolist.controller;

import com.example.todolist.dto.InviteResponse;
import com.example.todolist.dto.JoinListRequest;
import com.example.todolist.dto.JoinResponse;
import com.example.todolist.entity.InviteToken;
import com.example.todolist.entity.TodoList;
import com.example.todolist.service.InviteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lists")
public class InviteController {

    @Autowired
    private InviteService inviteService;

    @PostMapping("/{token}/invites")
    public ResponseEntity<InviteResponse> createInvite(
            @PathVariable String token,
            @RequestHeader(value = "X-User-Id", required = false) Long creatorId
    ) {
        // V2-B 暂不验证权限，后续版本添加
        InviteToken invite = inviteService.createInvite(token);
        // 使用相对路径，前端会自动补全完整 URL
        String inviteUrl = "/lists/" + token + "?invite=" + invite.getToken();
        return ResponseEntity.status(201).body(new InviteResponse(invite, inviteUrl));
    }

    @PostMapping("/join")
    public ResponseEntity<JoinResponse> joinList(
            @RequestBody @Valid JoinListRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        TodoList list = inviteService.joinList(request.getInviteToken(), userId);
        return ResponseEntity.ok(new JoinResponse(list.getToken(), "MEMBER"));
    }
}
