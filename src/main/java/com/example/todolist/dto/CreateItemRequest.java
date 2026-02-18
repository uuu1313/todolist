package com.example.todolist.dto;

public class CreateItemRequest {

    private String title;

    public CreateItemRequest() {}

    public CreateItemRequest(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
