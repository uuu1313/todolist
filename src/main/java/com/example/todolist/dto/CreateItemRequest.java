package com.example.todolist.dto;

import com.example.todolist.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class CreateItemRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过 200 个字符")
    private String title;

    private Priority priority = Priority.MEDIUM;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为 yyyy-MM-dd")
    private String dueDate;

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
}
