package com.example.todolist.service;

import com.example.todolist.entity.TodoList;
import com.example.todolist.exception.NotFoundException;
import com.example.todolist.repository.TodoListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@Transactional
public class ListService {

    @Autowired
    private TodoListRepository listRepository;

    @Autowired
    private TokenService tokenService;

    public TodoList createList() {
        String token = tokenService.generateUniqueToken();
        TodoList list = new TodoList(token);
        return listRepository.save(list);
    }

    @Transactional(readOnly = true)
    public TodoList getListByToken(String token) {
        return listRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("List not found"));
    }

    /**
     * 更新清单标题 (V2-A 新增)
     */
    public TodoList updateListTitle(String token, String title) {
        TodoList list = listRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("List not found"));

        // 验证标题
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        title = title.trim();
        if (title.length() > 100) {
            throw new IllegalArgumentException("标题长度不能超过 100 个字符");
        }

        list.setTitle(title);
        return listRepository.save(list);
    }

    /**
     * 删除清单 (V2-A 新增)
     * 级联删除由 JPA 自动处理
     */
    public void deleteList(String token) {
        TodoList list = listRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("List not found"));
        listRepository.delete(list);
    }
}
