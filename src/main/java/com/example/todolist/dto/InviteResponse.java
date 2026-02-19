package com.example.todolist.dto;

import com.example.todolist.entity.InviteToken;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class InviteResponse {
    private String inviteToken;
    private String inviteUrl;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public InviteResponse(InviteToken invite, String inviteUrl) {
        this.inviteToken = invite.getToken();
        this.inviteUrl = inviteUrl;
        this.createdAt = invite.getCreatedAt();
    }

    public String getInviteToken() {
        return inviteToken;
    }

    public String getInviteUrl() {
        return inviteUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
