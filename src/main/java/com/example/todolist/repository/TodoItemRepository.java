package com.example.todolist.repository;

import com.example.todolist.entity.TodoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TodoItemRepository extends JpaRepository<TodoItem, Long> {

    @Query("SELECT ti FROM TodoItem ti WHERE ti.list.id = :listId ORDER BY ti.completed ASC, ti.createdAt ASC")
    List<TodoItem> findByListIdOrderByCompletedAscAndCreatedAtAsc(@Param("listId") Long listId);
}
