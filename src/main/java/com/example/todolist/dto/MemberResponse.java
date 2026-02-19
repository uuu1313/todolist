package com.example.todolist.dto;

import com.example.todolist.entity.ListMember;
import com.example.todolist.entity.MemberRole;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class MemberResponse {
    private Long id;
    private Long userId;
    private String username;
    private String role;
    private String roleDisplay;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime joinedAt;

    public MemberResponse(ListMember member) {
        this.id = member.getId();
        this.userId = member.getUser().getId();
        this.username = member.getUser().getUsername();
        this.role = member.getRole().name();
        this.roleDisplay = member.getRole() == MemberRole.OWNER ? "所有者" : "成员";
        this.joinedAt = member.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getRoleDisplay() {
        return roleDisplay;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}
