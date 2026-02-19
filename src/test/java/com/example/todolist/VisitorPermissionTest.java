package com.example.todolist;

import com.example.todolist.dto.CreateItemRequest;
import com.example.todolist.dto.ItemResponse;
import com.example.todolist.dto.MemberResponse;
import com.example.todolist.dto.UpdateItemRequest;
import com.example.todolist.entity.Priority;
import com.example.todolist.entity.TodoList;
import com.example.todolist.entity.User;
import com.example.todolist.repository.TodoListRepository;
import com.example.todolist.repository.TodoItemRepository;
import com.example.todolist.repository.UserRepository;
import com.example.todolist.repository.ListMemberRepository;
import com.example.todolist.service.ItemService;
import com.example.todolist.service.MemberService;
import com.example.todolist.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class VisitorPermissionTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TodoListRepository listRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TodoItemRepository itemRepository;

    @Autowired
    private ListMemberRepository memberRepository;

    @Autowired
    private MemberService memberService;

    @Autowired
    private ItemService itemService;

    private User owner;
    private User visitor;
    private TodoList todoList;
    private String listToken;

    @BeforeEach
    void setUp() {
        // Clean up
        itemRepository.deleteAll();
        memberRepository.deleteAll();
        listRepository.deleteAll();
        userRepository.deleteAll();

        // Create users
        owner = new User("owner");
        owner = userRepository.save(owner);

        visitor = new User("visitor");
        visitor = userRepository.save(visitor);

        // Create list with owner as member
        todoList = new TodoList();
        todoList.setToken("ABC12345");
        todoList.setTitle("Test List");
        todoList = listRepository.save(todoList);
        listToken = todoList.getToken();

        // Add owner as OWNER member
        memberService.addMember(listToken, owner.getId(), com.example.todolist.entity.MemberRole.OWNER);

        // Visitor is NOT added to the list - they are a VISITOR
    }

    @Test
    void testVisitorCanReadItems() {
        // Add an item as owner
        itemService.addItem(listToken, "Test Item", Priority.MEDIUM, null, owner.getId());

        // Visitor tries to read items - should succeed
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", visitor.getId().toString());
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/lists/" + listToken + "/items",
                HttpMethod.GET,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testVisitorCannotCreateItem() {
        // Visitor tries to create item - should fail with 403
        CreateItemRequest request = new CreateItemRequest();
        request.setTitle("Visitor Item");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", visitor.getId().toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateItemRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/lists/" + listToken + "/items",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void testOwnerCanCreateItem() {
        // Owner creates item - should succeed
        CreateItemRequest request = new CreateItemRequest();
        request.setTitle("Owner Item");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", owner.getId().toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateItemRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ItemResponse> response = restTemplate.exchange(
                "/api/lists/" + listToken + "/items",
                HttpMethod.POST,
                entity,
                ItemResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getTitle()).isEqualTo("Owner Item");
    }

    @Test
    void testVisitorCannotUpdateItem() {
        // Create an item as owner
        var item = itemService.addItem(listToken, "Test Item", Priority.MEDIUM, null, owner.getId());

        // Visitor tries to update item - should fail with 403
        UpdateItemRequest request = new UpdateItemRequest();
        request.setTitle("Updated by Visitor");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", visitor.getId().toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UpdateItemRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/items/" + item.getId() + "?token=" + listToken,
                HttpMethod.PATCH,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void testVisitorCannotDeleteItem() {
        // Create an item as owner
        var item = itemService.addItem(listToken, "Test Item", Priority.MEDIUM, null, owner.getId());

        // Visitor tries to delete item - should fail with 403
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", visitor.getId().toString());
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/items/" + item.getId() + "?token=" + listToken,
                HttpMethod.DELETE,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void testIsMemberMethod() {
        // Owner is a member
        assertTrue(memberService.isMember(todoList, owner));

        // Visitor is NOT a member
        assertFalse(memberService.isMember(todoList, visitor));
    }

    @Test
    void testIsVisitorMethod() {
        // Owner is NOT a visitor
        assertFalse(memberService.isVisitor(todoList, owner));

        // Visitor IS a visitor
        assertTrue(memberService.isVisitor(todoList, visitor));
    }
}
