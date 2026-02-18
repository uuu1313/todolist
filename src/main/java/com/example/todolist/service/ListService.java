package com.example.todolist.service;

import com.example.todolist.entity.TodoList;
import com.example.todolist.exception.NotFoundException;
import com.example.todolist.repository.TodoListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
