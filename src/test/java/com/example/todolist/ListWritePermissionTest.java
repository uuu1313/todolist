package com.example.todolist;

import com.example.todolist.dto.UpdateListRequest;
import com.example.todolist.entity.*;
import com.example.todolist.repository.TodoListRepository;
import com.example.todolist.repository.UserRepository;
import com.example.todolist.repository.ListMemberRepository;
import com.example.todolist.service.MemberService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 清单写接口权限测试
 * 验证 PATCH/DELETE /api/lists/{token} 的权限控制
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ListWritePermissionTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Autowired
    private TodoListRepository listRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListMemberRepository memberRepository;

    @Autowired
    private MemberService memberService;

    private User owner;
    private User member;
    private User nonMember;
    private TodoList todoList;
    private String listToken;
    private RestTemplate patchCapableRestTemplate;

    @BeforeEach
    void setUp() {
        // Clean up
        memberRepository.deleteAll();
        listRepository.deleteAll();
        userRepository.deleteAll();

        // Create users
        owner = new User("owner");
        owner = userRepository.save(owner);

        member = new User("member");
        member = userRepository.save(member);

        nonMember = new User("nonMember");
        nonMember = userRepository.save(nonMember);

        // Create list
        todoList = new TodoList();
        todoList.setToken("TESTLIST");
        todoList.setTitle("Test List");
        todoList = listRepository.save(todoList);
        listToken = todoList.getToken();

        // Add owner as OWNER
        memberService.addMember(listToken, owner.getId(), MemberRole.OWNER);
        // Add member as MEMBER
        memberService.addMember(listToken, member.getId(), MemberRole.MEMBER);
        // nonMember is NOT added

        // 初始化支持 PATCH 的 RestTemplate
        patchCapableRestTemplate = restTemplateBuilder
                .requestFactory(HttpComponentsClientHttpRequestFactory.class)
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    private RestTemplate getPatchCapableRestTemplate() {
        return patchCapableRestTemplate;
    }

    /**
     * 测试1：OWNER 可以更新清单
     */
    @Test
    @Order(1)
    void testOwnerCanUpdateList() {
        UpdateListRequest request = new UpdateListRequest();
        request.setTitle("Updated by Owner");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", owner.getId().toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UpdateListRequest> entity = new HttpEntity<>(request, headers);

        String url = restTemplate.getRestTemplate().getUriTemplateHandler()
                .expand("/api/lists/" + listToken).toString();

        ResponseEntity<String> response = getPatchCapableRestTemplate().exchange(
                url,
                HttpMethod.PATCH,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /**
     * 测试2：非 OWNER 不能更新清单 - 返回 403
     */
    @Test
    @Order(2)
    void testNonOwnerCannotUpdateList_Returns403() {
        UpdateListRequest request = new UpdateListRequest();
        request.setTitle("Updated by Member");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", member.getId().toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UpdateListRequest> entity = new HttpEntity<>(request, headers);

        String url = restTemplate.getRestTemplate().getUriTemplateHandler()
                .expand("/api/lists/" + listToken).toString();

        org.springframework.web.client.HttpClientErrorException exception = assertThrows(
            org.springframework.web.client.HttpClientErrorException.class,
            () -> getPatchCapableRestTemplate().exchange(url, HttpMethod.PATCH, entity, String.class)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * 测试3：缺失 X-User-Id 不能更新清单 - 返回 400
     */
    @Test
    @Order(3)
    void testMissingUserIdCannotUpdateList_Returns400() {
        UpdateListRequest request = new UpdateListRequest();
        request.setTitle("Updated without User");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UpdateListRequest> entity = new HttpEntity<>(request, headers);

        String url = restTemplate.getRestTemplate().getUriTemplateHandler()
                .expand("/api/lists/" + listToken).toString();

        ResponseEntity<com.example.todolist.dto.ErrorResponse> response = restTemplate.exchange(
                url,
                HttpMethod.PATCH,
                entity,
                com.example.todolist.dto.ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("X-User-Id is required");
    }

    /**
     * 测试4：OWNER 可以删除清单
     */
    @Test
    @Order(4)
    void testOwnerCanDeleteList() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", owner.getId().toString());
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/lists/" + listToken,
                HttpMethod.DELETE,
                entity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    /**
     * 测试5：非 OWNER 不能删除清单 - 返回 403
     */
    @Test
    @Order(5)
    void testNonOwnerCannotDeleteList_Returns403() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", member.getId().toString());
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // TestRestTemplate 对 4xx 返回响应而不是抛出异常
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/lists/" + listToken,
                HttpMethod.DELETE,
                entity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * 测试6：缺失 X-User-Id 不能删除清单 - 返回 400
     */
    @Test
    @Order(6)
    void testMissingUserIdCannotDeleteList_Returns400() {
        HttpEntity<?> entity = new HttpEntity<>(new HttpHeaders());

        ResponseEntity<com.example.todolist.dto.ErrorResponse> response = restTemplate.exchange(
                "/api/lists/" + listToken,
                HttpMethod.DELETE,
                entity,
                com.example.todolist.dto.ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("X-User-Id is required");
    }

    /**
     * 测试7：不存在的用户 ID 不能更新清单 - 返回 400
     */
    @Test
    @Order(7)
    void testNonExistentUserCannotUpdateList_Returns400() {
        UpdateListRequest request = new UpdateListRequest();
        request.setTitle("Updated by Ghost");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "99999");
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UpdateListRequest> entity = new HttpEntity<>(request, headers);

        String url = restTemplate.getRestTemplate().getUriTemplateHandler()
                .expand("/api/lists/" + listToken).toString();

        // getPatchCapableRestTemplate 会抛出异常，需要捕获
        org.springframework.web.client.HttpClientErrorException exception = assertThrows(
            org.springframework.web.client.HttpClientErrorException.class,
            () -> getPatchCapableRestTemplate().exchange(url, HttpMethod.PATCH, entity, String.class)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getResponseBodyAsString()).contains("User not found");
    }
}
