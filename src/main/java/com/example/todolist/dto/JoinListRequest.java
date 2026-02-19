package com.example.todolist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class JoinListRequest {

    @NotBlank(message = "邀请令牌不能为空")
    @Size(min = 12, max = 12, message = "邀请令牌必须是12位")
    private String inviteToken;

    public String getInviteToken() {
        return inviteToken;
    }

    public void setInviteToken(String inviteToken) {
        this.inviteToken = inviteToken;
    }
}
