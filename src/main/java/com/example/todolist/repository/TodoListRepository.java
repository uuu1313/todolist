package com.example.todolist.repository;

import com.example.todolist.entity.TodoList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TodoListRepository extends JpaRepository<TodoList, Long> {

    Optional<TodoList> findByToken(String token);

    boolean existsByToken(String token);
}
