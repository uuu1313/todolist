package com.example.todolist.service;

import com.example.todolist.entity.User;
import com.example.todolist.repository.UserRepository;
import com.example.todolist.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * 创建匿名用户（自动生成用户名）
     */
    public User createAnonymousUser() {
        User user = new User();
        user.setUsername(generateUsername());
        return userRepository.save(user);
    }

    /**
     * 创建用户（指定用户名）
     */
    public User createUser(String username) {
        // 验证用户名唯一性
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User(username);
        return userRepository.save(user);
    }

    /**
     * 获取用户
     */
    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
    }

    /**
     * 更新用户名
     */
    public User updateUser(Long userId, String newUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        if (userRepository.existsByUsernameAndIdNot(newUsername, userId)) {
            throw new IllegalArgumentException("用户名已存在");
        }

        user.setUsername(newUsername);
        return userRepository.save(user);
    }

    /**
     * 生成随机用户名
     * 格式: "用户_" + 6位随机字符
     */
    private String generateUsername() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return "用户_" + sb.toString();
    }
}
