package com.example.todolist.service;

import com.example.todolist.entity.TodoItem;
import com.example.todolist.entity.TodoList;
import com.example.todolist.exception.NotFoundException;
import com.example.todolist.repository.TodoItemRepository;
import com.example.todolist.repository.TodoListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ItemService {

    @Autowired
    private TodoListRepository listRepository;

    @Autowired
    private TodoItemRepository itemRepository;

    public TodoItem addItem(String token, String title) {
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
        return itemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<TodoItem> getItemsByToken(String token) {
        TodoList list = listRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("List not found"));
        return itemRepository.findByListIdOrderByCompletedAscAndCreatedAtAsc(list.getId());
    }

    public TodoItem updateItem(Long id, String title, Boolean completed) {
        TodoItem item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found"));

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

        if (completed != null) {
            item.setCompleted(completed);
        }

        return itemRepository.save(item);
    }

    public void deleteItem(Long id) {
        TodoItem item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found"));
        itemRepository.delete(item);
    }
}
