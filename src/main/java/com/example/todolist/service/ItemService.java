package com.example.todolist.service;

import com.example.todolist.entity.Priority;
import com.example.todolist.entity.TodoItem;
import com.example.todolist.entity.TodoList;
import com.example.todolist.exception.NotFoundException;
import com.example.todolist.repository.TodoItemRepository;
import com.example.todolist.repository.TodoListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@Transactional
public class ItemService {

    @Autowired
    private TodoListRepository listRepository;

    @Autowired
    private TodoItemRepository itemRepository;

    public TodoItem addItem(String token, String title) {
        return addItem(token, title, null, null);
    }

    /**
     * 添加事项 (V2-A 扩展)
     * @param token 清单 token
     * @param title 事项标题
     * @param priority 优先级 (可为 null,默认 MEDIUM)
     * @param dueDate 截止日期 (可为 null)
     */
    public TodoItem addItem(String token, String title, Priority priority, LocalDate dueDate) {
        // Validate title
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        title = title.trim();
        if (title.length() > 200) {
            throw new IllegalArgumentException("标题长度不能超过 200 个字符");
        }

        // Find list
        TodoList list = listRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("List not found"));

        // Create item
        TodoItem item = new TodoItem(list, title);
        item.setPriority(priority != null ? priority : Priority.MEDIUM);
        item.setDueDate(dueDate);

        return itemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<TodoItem> getItemsByToken(String token) {
        TodoList list = listRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("List not found"));
        return itemRepository.findByListIdOrderByCompletedAscAndCreatedAtAsc(list.getId());
    }

    public TodoItem updateItem(Long id, String token, String title, Boolean completed) {
        return updateItem(id, token, title, completed, null, null);
    }

    /**
     * 更新事项 (V2-A 扩展)
     * @param id 事项ID
     * @param token 清单 token
     * @param title 标题 (可为 null)
     * @param completed 完成状态 (可为 null)
     * @param priority 优先级 (可为 null)
     * @param dueDateStr 截止日期字符串 (可为 null)
     */
    public TodoItem updateItem(Long id, String token, String title, Boolean completed, Priority priority, String dueDateStr) {
        TodoItem item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found"));

        // Verify item belongs to the list with given token
        TodoList list = listRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("List not found"));
        if (!item.getList().getId().equals(list.getId())) {
            throw new NotFoundException("Item not found");
        }

        // 更新标题
        if (title != null) {
            title = title.trim();
            if (title.isEmpty()) {
                throw new IllegalArgumentException("标题不能为空");
            }
            if (title.length() > 200) {
                throw new IllegalArgumentException("标题长度不能超过 200 个字符");
            }
            item.setTitle(title);
        }

        // 更新完成状态
        if (completed != null) {
            item.setCompleted(completed);
        }

        // 更新优先级
        if (priority != null) {
            item.setPriority(priority);
        }

        // 更新截止日期
        if (dueDateStr != null) {
            if (dueDateStr.isEmpty()) {
                item.setDueDate(null);
            } else {
                LocalDate dueDate = parseDate(dueDateStr);
                item.setDueDate(dueDate);
            }
        }

        return itemRepository.save(item);
    }

    public void deleteItem(Long id, String token) {
        TodoItem item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found"));

        // Verify item belongs to the list with given token
        TodoList list = listRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("List not found"));
        if (!item.getList().getId().equals(list.getId())) {
            throw new NotFoundException("Item not found");
        }

        itemRepository.delete(item);
    }

    /**
     * 解析日期字符串
     * @param dateStr 日期字符串 (yyyy-MM-dd)
     * @return LocalDate
     * @throws IllegalArgumentException 如果日期格式无效
     */
    private LocalDate parseDate(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            // 验证日期合理性 (如拒绝 2026-02-30)
            if (date.getYear() < 1900 || date.getYear() > 2100) {
                throw new IllegalArgumentException("日期年份必须在 1900-2100 之间");
            }
            return date;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日期格式错误,必须为 yyyy-MM-dd");
        }
    }
}
