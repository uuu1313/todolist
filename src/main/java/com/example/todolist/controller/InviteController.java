package com.example.todolist.controller;

import com.example.todolist.dto.InviteResponse;
import com.example.todolist.dto.JoinListRequest;
import com.example.todolist.dto.JoinResponse;
import com.example.todolist.entity.InviteToken;
import com.example.todolist.entity.TodoList;
import com.example.todolist.entity.User;
import com.example.todolist.service.InviteService;
import com.example.todolist.service.ListService;
import com.example.todolist.service.MemberService;
import com.example.todolist.repository.UserRepository;
import com.example.todolist.exception.ForbiddenException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lists")
public class InviteController {

    @Autowired
    private InviteService inviteService;

    @Autowired
    private ListService listService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/{token}/invites")
    public ResponseEntity<InviteResponse> createInvite(
            @PathVariable String token,
            @RequestHeader(value = "X-User-Id", required = false) Long creatorId
    ) {
        // 强制校验：必须提供用户 ID
        if (creatorId == null) {
            throw new IllegalArgumentException("X-User-Id is required");
        }

        // 验证用户是否存在
        User user = userRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 获取清单并验证用户是 OWNER
        TodoList list = listService.getListByToken(token);
        if (!memberService.isOwner(list, user)) {
            throw new ForbiddenException("Only OWNER can create invites");
        }

        InviteToken invite = inviteService.createInvite(token);
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
