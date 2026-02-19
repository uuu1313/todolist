package com.example.todolist.repository;

import com.example.todolist.entity.InviteToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InviteTokenRepository extends JpaRepository<InviteToken, Long> {

    /**
     * 根据令牌查询邀请
     */
    Optional<InviteToken> findByToken(String token);

    /**
     * 检查令牌是否存在
     */
    boolean existsByToken(String token);

    /**
     * 查询清单的所有邀请令牌
     */
    List<InviteToken> findByListId(Long listId);

    /**
     * 删除清单的所有邀请令牌
     */
    void deleteByListId(Long listId);
}
