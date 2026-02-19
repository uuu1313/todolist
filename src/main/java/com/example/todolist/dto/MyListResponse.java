package com.example.todolist.dto;

import com.example.todolist.entity.TodoList;
import com.example.todolist.entity.MemberRole;

/**
 * 我的清单响应 DTO
 */
public class MyListResponse {

    private String token;
    private String title;
    private MemberRole role;

    public MyListResponse() {}

    public MyListResponse(TodoList list, MemberRole role) {
        this.token = list.getToken();
        this.title = list.getTitle();
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public MemberRole getRole() {
        return role;
    }

    public void setRole(MemberRole role) {
        this.role = role;
    }
}
