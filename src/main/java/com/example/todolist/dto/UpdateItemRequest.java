package com.example.todolist.dto;

import com.example.todolist.entity.Priority;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class UpdateItemRequest {

    @Size(max = 200, message = "标题长度不能超过 200 个字符")
    private String title;
    private Boolean completed;
    private Priority priority;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为 yyyy-MM-dd")
    private String dueDate;

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

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * 将 dueDate 字符串转换为 LocalDate
     * @return LocalDate 或 null
     */
    public LocalDate getDueDateAsLocalDate() {
        if (dueDate == null || dueDate.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dueDate);
        } catch (Exception e) {
            throw new IllegalArgumentException("日期格式错误,必须为 yyyy-MM-dd");
        }
    }

    /**
     * 验证请求体至少包含一个字段
     * @return true 如果请求体有效
     */
    public boolean isValid() {
        return title != null || completed != null || priority != null || dueDate != null;
    }
}
