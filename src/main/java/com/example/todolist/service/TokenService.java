package com.example.todolist.service;

import com.example.todolist.repository.TodoListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class TokenService {

    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TOKEN_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_RETRIES = 10;

    @Autowired
    private TodoListRepository listRepository;

    public String generateUniqueToken() {
        for (int i = 0; i < MAX_RETRIES; i++) {
            String token = generate();
            if (!listRepository.existsByToken(token)) {
                return token;
            }
        }
        throw new IllegalStateException("Failed to generate unique token after " + MAX_RETRIES + " retries");
    }

    private static String generate() {
        StringBuilder token = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            token.append(CHAR_POOL.charAt(RANDOM.nextInt(CHAR_POOL.length())));
        }
        return token.toString();
    }
}
