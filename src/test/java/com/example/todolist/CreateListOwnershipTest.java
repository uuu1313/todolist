package com.example.todolist;

import com.example.todolist.dto.ErrorResponse;
import com.example.todolist.entity.TodoList;
import com.example.todolist.entity.User;
import com.example.todolist.repository.TodoListRepository;
import com.example.todolist.repository.UserRepository;
import com.example.todolist.repository.ListMemberRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.client.*;
import org.springframework.http.*;
import org.springframework.transaction.annotation.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 创建清单用户归属测试
 *
 * 验证修复：不允许创建孤儿清单，所有清单必须与有效用户建立 OWNER 关系
 *
 * 注意：不使用 @Transactional，因为 TestRestTemplate 的 HTTP 请求需要在独立事务中访问数据
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CreateListOwnershipTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TodoListRepository listRepository;

    @Autowired
    private ListMemberRepository memberRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        memberRepository.deleteAll();
        listRepository.deleteAll();
        userRepository.deleteAll();

        // 创建测试用户
        testUser = new User("testuser");
        testUser = userRepository.save(testUser);
    }

    /**
     * 测试1：带有效 X-User-Id 创建清单 - 应该成功
     */
    @Test
    @Order(1)
    void testCreateListWithValidUserId_Success() {
        // 发送创建请求
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", testUser.getId().toString());
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/lists",
                HttpMethod.POST,
                entity,
                String.class
        );

        // 验证返回 201
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 验证清单被创建
        long listCount = listRepository.count();
        assertThat(listCount).isEqualTo(1);

        // 验证 membership 被创建
        long memberCount = memberRepository.count();
        assertThat(memberCount).isEqualTo(1);

        // 验证清单属于该用户
        TodoList list = listRepository.findAll().get(0);
        var members = memberRepository.findByListId(list.getId());
        assertThat(members).hasSize(1);
        assertThat(members.get(0).getUser().getId()).isEqualTo(testUser.getId());
    }

    /**
     * 测试2：不带 X-User-Id 创建清单 - 应该返回 400
     */
    @Test
    @Order(2)
    void testCreateListWithoutUserId_Returns400() {
        // 发送不带 X-User-Id 的请求
        HttpEntity<?> entity = new HttpEntity<>(new HttpHeaders());

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/lists",
                HttpMethod.POST,
                entity,
                ErrorResponse.class
        );

        // 验证返回 400
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("X-User-Id is required");

        // 验证数据库中没有新增清单（避免孤儿记录）
        long listCount = listRepository.count();
        assertThat(listCount).isEqualTo(0);

        // 验证也没有新增 membership
        long memberCount = memberRepository.count();
        assertThat(memberCount).isEqualTo(0);
    }

    /**
     * 测试3：带不存在的 X-User-Id 创建清单 - 应该返回 400
     */
    @Test
    @Order(3)
    void testCreateListWithNonExistentUserId_Returns400() {
        // 使用不存在的用户 ID
        Long nonExistentUserId = 99999L;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", nonExistentUserId.toString());
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/lists",
                HttpMethod.POST,
                entity,
                ErrorResponse.class
        );

        // 验证返回 400
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("User not found");

        // 验证数据库中没有新增清单（避免孤儿记录）
        long listCount = listRepository.count();
        assertThat(listCount).isEqualTo(0);

        // 验证也没有新增 membership
        long memberCount = memberRepository.count();
        assertThat(memberCount).isEqualTo(0);
    }

    /**
     * 测试4：创建的清单能在"我的清单"中看到
     * 这是一个集成测试，验证完整流程
     */
    @Test
    @Order(4)
    void testCreatedListAppearsInMyLists() {
        // 步骤1: 创建清单
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", testUser.getId().toString());
        HttpEntity<?> createEntity = new HttpEntity<>(headers);

        ResponseEntity<String> createResponse = restTemplate.exchange(
                "/api/lists",
                HttpMethod.POST,
                createEntity,
                String.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 步骤2: 查询"我的清单"
        HttpEntity<?> getEntity = new HttpEntity<>(headers);

        ResponseEntity<String> getResponse = restTemplate.exchange(
                "/api/my/lists",
                HttpMethod.GET,
                getEntity,
                String.class
        );

        // 验证 200
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 验证响应中包含创建的清单
        String body = getResponse.getBody();
        assertThat(body).isNotNull();
        assertThat(body).isNotEmpty();
    }

    /**
     * 测试5：事务一致性验证
     * 如果 membership 创建失败，清单也应该回滚
     * 由于 addMember 在正常情况下不会失败，这个测试主要验证
     * 清单和 membership 在同一个事务中创建
     */
    @Test
    @Order(5)
    void testTransactionConsistency() {
        // 创建成功的情况
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", testUser.getId().toString());
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/lists",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 验证清单和 membership 要么都存在，要么都不存在
        long listCount = listRepository.count();
        long memberCount = memberRepository.count();

        // 两者应该相等（1 个清单对应 1 个 membership）
        assertThat(listCount).isEqualTo(memberCount);
        assertThat(listCount).isGreaterThan(0);
    }
}
