package com.example.todolist.dto;

public class JoinResponse {
    private String listToken;
    private String role;
    private String message;

    public JoinResponse(String listToken, String role) {
        this.listToken = listToken;
        this.role = role;
        this.message = "成功加入清单";
    }

    public String getListToken() {
        return listToken;
    }

    public String getRole() {
        return role;
    }

    public String getMessage() {
        return message;
    }
}
