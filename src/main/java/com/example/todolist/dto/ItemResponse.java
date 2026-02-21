package com.example.todolist.dto;

import com.example.todolist.entity.TodoItem;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ItemResponse {

    private Long id;
    private String title;
    private Boolean completed;
    private String priority;
    private String dueDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    private String createdBy;
    private String updatedBy;

    // 无参构造函数，支持 Jackson 反序列化
    public ItemResponse() {
    }

    public ItemResponse(TodoItem item) {
        this.id = item.getId();
        this.title = item.getTitle();
        this.completed = item.getCompleted();
        this.priority = item.getPriority().name();
        this.dueDate = item.getDueDate() != null
            ? item.getDueDate().toString()
            : null;
        this.createdAt = item.getCreatedAt();
        this.updatedAt = item.getUpdatedAt();

        // 新增：创建者和更新者用户名
        if (item.getCreatedBy() != null) {
            this.createdBy = item.getCreatedBy().getUsername();
        }
        if (item.getUpdatedBy() != null) {
            this.updatedBy = item.getUpdatedBy().getUsername();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
