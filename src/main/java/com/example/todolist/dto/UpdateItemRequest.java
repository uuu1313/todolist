package com.example.todolist.dto;

import jakarta.validation.constraints.Size;

public class UpdateItemRequest {

    @Size(max = 200, message = "标题长度不能超过 200 个字符")
    private String title;
    private Boolean completed;

    public UpdateItemRequest() {}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }
}
