package com.example.todolist.controller;

import com.example.todolist.dto.MemberResponse;
import com.example.todolist.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lists")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @GetMapping("/{token}/members")
    public ResponseEntity<List<MemberResponse>> getMembers(@PathVariable String token) {
        List<MemberResponse> members = memberService.getMembers(token);
        return ResponseEntity.ok(members);
    }

    @DeleteMapping("/{token}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable String token,
            @PathVariable Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long operatorId
    ) {
        if (operatorId == null) {
            return ResponseEntity.status(403).build();
        }
        memberService.removeMember(token, userId, operatorId);
        return ResponseEntity.noContent().build();
    }
}
