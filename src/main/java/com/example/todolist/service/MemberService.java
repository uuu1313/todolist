package com.example.todolist.service;

import com.example.todolist.entity.ListMember;
import com.example.todolist.entity.MemberRole;
import com.example.todolist.entity.TodoList;
import com.example.todolist.entity.User;
import com.example.todolist.repository.ListMemberRepository;
import com.example.todolist.repository.TodoListRepository;
import com.example.todolist.repository.UserRepository;
import com.example.todolist.dto.MemberResponse;
import com.example.todolist.exception.NotFoundException;
import com.example.todolist.exception.ForbiddenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MemberService {

    @Autowired
    private ListMemberRepository memberRepository;

    @Autowired
    private TodoListRepository listRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 添加成员
     */
    public ListMember addMember(String listToken, Long userId, MemberRole role) {
        TodoList list = listRepository.findByToken(listToken)
                .orElseThrow(() -> new NotFoundException("清单不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        // 检查是否已是成员
        if (memberRepository.existsByListAndUser(list, user)) {
            throw new IllegalStateException("用户已是成员");
        }

        ListMember member = new ListMember(list, user, role);
        return memberRepository.save(member);
    }

    /**
     * 移除成员
     */
    public void removeMember(String listToken, Long memberUserId, Long operatorUserId) {
        TodoList list = listRepository.findByToken(listToken)
                .orElseThrow(() -> new NotFoundException("清单不存在"));

        // 检查操作者是否是 OWNER
        User operator = userRepository.findById(operatorUserId)
                .orElseThrow(() -> new NotFoundException("操作者不存在"));

        if (!isOwner(list, operator)) {
            throw new ForbiddenException("只有清单所有者可以移除成员");
        }

        User memberUser = userRepository.findById(memberUserId)
                .orElseThrow(() -> new NotFoundException("成员不存在"));

        ListMember member = memberRepository.findByListAndUser(list, memberUser)
                .orElseThrow(() -> new NotFoundException("成员关系不存在"));

        memberRepository.delete(member);
    }

    /**
     * 获取成员列表
     */
    @Transactional(readOnly = true)
    public List<MemberResponse> getMembers(String listToken) {
        TodoList list = listRepository.findByToken(listToken)
                .orElseThrow(() -> new NotFoundException("清单不存在"));

        return memberRepository.findByList(list).stream()
                .map(MemberResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * 检查用户是否是所有者
     */
    @Transactional(readOnly = true)
    public boolean isOwner(TodoList list, User user) {
        Boolean result = memberRepository.isOwner(list, user);
        // null 表示用户不在清单中，返回 false
        return Boolean.TRUE.equals(result);
    }

    /**
     * 检查用户是否是成员
     */
    @Transactional(readOnly = true)
    public boolean isMember(TodoList list, User user) {
        return memberRepository.existsByListAndUser(list, user);
    }
}
