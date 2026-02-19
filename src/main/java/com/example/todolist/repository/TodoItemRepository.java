package com.example.todolist.repository;

import com.example.todolist.entity.TodoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TodoItemRepository extends JpaRepository<TodoItem, Long> {

    @Query("SELECT ti FROM TodoItem ti WHERE ti.list.id = :listId " +
           "ORDER BY ti.completed ASC, " +
           "CASE ti.priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 ELSE 4 END ASC, " +
           "ti.dueDate ASC NULLS LAST, " +
           "ti.updatedAt DESC, " +
           "ti.id DESC")
    List<TodoItem> findByListId(@Param("listId") Long listId);
}
