package com.example.todolist.repository;

import com.example.todolist.entity.ListMember;
import com.example.todolist.entity.TodoList;
import com.example.todolist.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListMemberRepository extends JpaRepository<ListMember, Long> {

    /**
     * 查询清单的所有成员
     */
    List<ListMember> findByListId(Long listId);

    /**
     * 查询清单的所有成员（通过实体）
     */
    List<ListMember> findByList(TodoList list);

    /**
     * 查询用户加入的所有清单
     */
    List<ListMember> findByUserId(Long userId);

    /**
     * 查询用户在指定清单中的成员关系
     */
    Optional<ListMember> findByListAndUser(TodoList list, User user);

    /**
     * 检查用户是否是指定清单的成员
     */
    boolean existsByListAndUser(TodoList list, User user);

    /**
     * 检查用户是否是指定清单的所有者
     * 返回 Boolean（非 primitive）以允许 null 值
     */
    @Query("SELECT CASE WHEN lm.role = 'OWNER' THEN true ELSE false END " +
           "FROM ListMember lm WHERE lm.list = :list AND lm.user = :user")
    Boolean isOwner(@Param("list") TodoList list, @Param("user") User user);

    /**
     * 删除清单的所有成员
     */
    void deleteByListId(Long listId);
}
