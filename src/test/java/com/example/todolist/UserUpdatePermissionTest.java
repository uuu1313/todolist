package com.example.todolist;

import com.example.todolist.dto.UpdateUserRequest;
import com.example.todolist.entity.User;
import com.example.todolist.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户修改接口权限测试
 *
 * 验证 PATCH /api/users/{id} 的水平越权防护
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserUpdatePermissionTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        // 清理
        userRepository.deleteAll();

        // 创建两个测试用户
        user1 = userRepository.save(new User("user1"));
        user2 = userRepository.save(new User("user2"));
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    /**
     * 测试1：用户可以修改自己的用户名 - 应该成功
     */
    @Test
    @Order(1)
    void testUserCanUpdateOwnProfile_Success() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("new_username_1");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", user1.getId().toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UpdateUserRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/" + user1.getId(),
                HttpMethod.PATCH,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 验证用户名已更新
        User updated = userRepository.findById(user1.getId()).orElseThrow();
        assertThat(updated.getUsername()).isEqualTo("new_username_1");
    }

    /**
     * 测试2：用户不能修改其他用户的用户名 - 返回 403
     */
    @Test
    @Order(2)
    void testUserCannotUpdateOthers_Returns403() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("hacked_username");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", user1.getId().toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UpdateUserRequest> entity = new HttpEntity<>(request, headers);

        // 尝试修改 user2 的用户名，但使用 user1 的 X-User-Id
        ResponseEntity<com.example.todolist.dto.ErrorResponse> response = restTemplate.exchange(
                "/api/users/" + user2.getId(),
                HttpMethod.PATCH,
                entity,
                com.example.todolist.dto.ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage()).contains("只能修改自己的用户信息");

        // 验证 user2 的用户名未被修改
        User unchanged = userRepository.findById(user2.getId()).orElseThrow();
        assertThat(unchanged.getUsername()).isEqualTo("user2");
    }

    /**
     * 测试3：缺失 X-User-Id 不能修改用户名 - 返回 400
     */
    @Test
    @Order(3)
    void testMissingUserIdCannotUpdate_Returns400() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("anonymous_hack");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UpdateUserRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<com.example.todolist.dto.ErrorResponse> response = restTemplate.exchange(
                "/api/users/" + user1.getId(),
                HttpMethod.PATCH,
                entity,
                com.example.todolist.dto.ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("X-User-Id is required");
    }

    /**
     * 测试4：X-User-Id 与目标用户 ID 不匹配 - 返回 403
     */
    @Test
    @Order(4)
    void testUserIdMismatch_Returns403() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("should_not_work");

        HttpHeaders headers = new HttpHeaders();
        // X-User-Id 是 user1，但尝试修改 user2
        headers.set("X-User-Id", user1.getId().toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UpdateUserRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<com.example.todolist.dto.ErrorResponse> response = restTemplate.exchange(
                "/api/users/" + user2.getId(),
                HttpMethod.PATCH,
                entity,
                com.example.todolist.dto.ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * 测试5：修改不存在的用户 - 返回 404
     */
    @Test
    @Order(5)
    void testUpdateNonExistentUser_Returns404() {
        Long nonExistentUserId = 99999L;

        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("ghost_user");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", nonExistentUserId.toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UpdateUserRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/" + nonExistentUserId,
                HttpMethod.PATCH,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * 测试6：稳定性验证 - 多次修改自己的用户名都应该成功
     */
    @Test
    @Order(6)
    void testStability_MultipleUpdatesSuccess() {
        // 第一次修改
        UpdateUserRequest request1 = new UpdateUserRequest();
        request1.setUsername("version_2");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", user1.getId().toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UpdateUserRequest> entity1 = new HttpEntity<>(request1, headers);

        ResponseEntity<String> response1 = restTemplate.exchange(
                "/api/users/" + user1.getId(),
                HttpMethod.PATCH,
                entity1,
                String.class
        );

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 第二次修改
        UpdateUserRequest request2 = new UpdateUserRequest();
        request2.setUsername("version_3");
        HttpEntity<UpdateUserRequest> entity2 = new HttpEntity<>(request2, headers);

        ResponseEntity<String> response2 = restTemplate.exchange(
                "/api/users/" + user1.getId(),
                HttpMethod.PATCH,
                entity2,
                String.class
        );

        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 验证最终用户名
        User finalUser = userRepository.findById(user1.getId()).orElseThrow();
        assertThat(finalUser.getUsername()).isEqualTo("version_3");
    }
}
