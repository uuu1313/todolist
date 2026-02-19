package com.example.todolist.service;

import com.example.todolist.entity.InviteToken;
import com.example.todolist.entity.MemberRole;
import com.example.todolist.entity.TodoList;
import com.example.todolist.repository.InviteTokenRepository;
import com.example.todolist.repository.TodoListRepository;
import com.example.todolist.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@Transactional
public class InviteService {

    @Autowired
    private InviteTokenRepository inviteTokenRepository;

    @Autowired
    private TodoListRepository listRepository;

    @Autowired
    private MemberService memberService;

    /**
     * 创建邀请令牌
     */
    public InviteToken createInvite(String listToken) {
        TodoList list = listRepository.findByToken(listToken)
                .orElseThrow(() -> new NotFoundException("清单不存在"));

        String token = generateInviteToken();
        InviteToken invite = new InviteToken(list, token);
        return inviteTokenRepository.save(invite);
    }

    /**
     * 通过邀请令牌加入清单
     */
    public TodoList joinList(String inviteToken, Long userId) {
        InviteToken invite = inviteTokenRepository.findByToken(inviteToken)
                .orElseThrow(() -> new NotFoundException("邀请令牌无效"));

        TodoList list = invite.getList();
        memberService.addMember(list.getToken(), userId, MemberRole.MEMBER);
        return list;
    }

    /**
     * 生成唯一的邀请令牌
     * 格式: 12位随机字符
     */
    private String generateInviteToken() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        String token;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            token = sb.toString();
        } while (inviteTokenRepository.existsByToken(token));
        return token;
    }
}
