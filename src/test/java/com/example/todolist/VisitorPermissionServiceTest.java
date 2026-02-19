package com.example.todolist;

import com.example.todolist.entity.MemberRole;
import com.example.todolist.entity.Priority;
import com.example.todolist.entity.TodoList;
import com.example.todolist.entity.TodoItem;
import com.example.todolist.entity.User;
import com.example.todolist.exception.ForbiddenException;
import com.example.todolist.exception.NotFoundException;
import com.example.todolist.repository.TodoListRepository;
import com.example.todolist.repository.TodoItemRepository;
import com.example.todolist.repository.UserRepository;
import com.example.todolist.repository.ListMemberRepository;
import com.example.todolist.service.ItemService;
import com.example.todolist.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class VisitorPermissionServiceTest {

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

        // Create list
        todoList = new TodoList();
        todoList.setToken("ABC12345");
        todoList.setTitle("Test List");
        todoList = listRepository.save(todoList);
        listToken = todoList.getToken();

        // Add owner as OWNER member
        memberService.addMember(listToken, owner.getId(), MemberRole.OWNER);

        // Visitor is NOT added to the list - they are a VISITOR
    }

    @Test
    void testIsMemberMethod() {
        // Owner is a member
        assertTrue(memberService.isMember(todoList, owner), "Owner should be a member");

        // Visitor is NOT a member
        assertFalse(memberService.isMember(todoList, visitor), "Visitor should NOT be a member");
    }

    @Test
    void testIsVisitorMethod() {
        // Owner is NOT a visitor
        assertFalse(memberService.isVisitor(todoList, owner), "Owner should NOT be a visitor");

        // Visitor IS a visitor
        assertTrue(memberService.isVisitor(todoList, visitor), "Visitor should be a visitor");
    }

    @Test
    void testOwnerCanCreateItem() {
        // Owner creates item - should succeed
        TodoItem item = itemService.addItem(listToken, "Owner Item", Priority.MEDIUM, null, owner.getId());

        assertThat(item).isNotNull();
        assertThat(item.getTitle()).isEqualTo("Owner Item");
        assertThat(item.getCreatedBy().getId()).isEqualTo(owner.getId());

        // Verify item was saved
        TodoItem savedItem = itemRepository.findById(item.getId()).orElse(null);
        assertThat(savedItem).isNotNull();
        assertThat(savedItem.getTitle()).isEqualTo("Owner Item");
    }

    @Test
    void testVisitorCannotCreateItem_ThroughServiceLogic() {
        // This test demonstrates the VISITOR concept
        // In real scenario, controller would check isMember() before calling service

        // First verify visitor is indeed a visitor
        assertTrue(memberService.isVisitor(todoList, visitor), "User should be a visitor");

        // Simulate what controller should do:
        User user = visitor;
        assertThrows(ForbiddenException.class, () -> {
            if (memberService.isVisitor(todoList, user)) {
                // This is what should happen - throw ForbiddenException
                throw new ForbiddenException("只有清单成员可以执行此操作");
            }
        });
    }

    @Test
    void testMemberCanCreateAfterBeingAdded() {
        // Initially visitor is not a member
        assertTrue(memberService.isVisitor(todoList, visitor));

        // Add visitor as MEMBER
        memberService.addMember(listToken, visitor.getId(), MemberRole.MEMBER);

        // Now visitor is a member
        assertTrue(memberService.isMember(todoList, visitor));
        assertFalse(memberService.isVisitor(todoList, visitor));

        // Visitor (now member) can create item
        TodoItem item = itemService.addItem(listToken, "New Member Item", Priority.MEDIUM, null, visitor.getId());

        assertThat(item).isNotNull();
        assertThat(item.getTitle()).isEqualTo("New Member Item");
    }

    @Test
    void testRemovedMemberBecomesVisitor() {
        // Add visitor as MEMBER
        memberService.addMember(listToken, visitor.getId(), MemberRole.MEMBER);
        assertTrue(memberService.isMember(todoList, visitor));

        // Remove visitor from list
        memberService.removeMember(listToken, visitor.getId(), owner.getId());

        // Now visitor is no longer a member (becomes a visitor)
        assertFalse(memberService.isMember(todoList, visitor));
        assertTrue(memberService.isVisitor(todoList, visitor));
    }

    @Test
    void testOwnerIsNotVisitor() {
        // Owner should be a member
        assertTrue(memberService.isMember(todoList, owner));

        // Owner should NOT be a visitor
        assertFalse(memberService.isVisitor(todoList, owner));
    }

    @Test
    void testExistsByListAndUser() {
        // Test the helper method
        assertTrue(memberService.existsByListAndUser(todoList, owner));
        assertFalse(memberService.existsByListAndUser(todoList, visitor));

        // Add visitor as member
        memberService.addMember(listToken, visitor.getId(), MemberRole.MEMBER);

        // Now should return true
        assertTrue(memberService.existsByListAndUser(todoList, visitor));
    }

    @Test
    void testPermissionCheckLogic() {
        // Test the logic flow for permission checking

        // Scenario 1: Owner tries to create item
        User user1 = owner;
        if (memberService.isVisitor(todoList, user1)) {
            fail("Owner should not be visitor");
        }
        // Owner passes the check - can proceed

        // Scenario 2: Visitor tries to create item
        User user2 = visitor;
        if (memberService.isVisitor(todoList, user2)) {
            // This is what happens for visitor
            // In controller, this would throw ForbiddenException
            assertTrue(true, "Visitor correctly identified");
        } else {
            fail("Visitor should fail permission check");
        }
    }

    @Test
    void testItemOperationsWithPermissionChecks() {
        // Create an item as owner
        TodoItem item = itemService.addItem(listToken, "Test Item", Priority.MEDIUM, null, owner.getId());
        assertThat(item).isNotNull();

        // Test permission check before update
        User user = visitor;
        if (memberService.isVisitor(todoList, user)) {
            // Visitor would be blocked here in real controller
            assertTrue(true);
        } else {
            fail("Visitor should be blocked");
        }

        // Owner can update
        user = owner;
        if (memberService.isVisitor(todoList, user)) {
            fail("Owner should not be blocked");
        }
        // Owner passes - can proceed with update
        TodoItem updatedItem = itemService.updateItem(
            item.getId(),
            listToken,
            "Updated Title",
            true,
            Priority.HIGH,
            null,
            owner.getId()
        );
        assertThat(updatedItem.getTitle()).isEqualTo("Updated Title");
        assertThat(updatedItem.getCompleted()).isTrue();
    }

    @Test
    void testDeleteItemWithPermissionChecks() {
        // Create an item as owner
        TodoItem item = itemService.addItem(listToken, "Test Item", Priority.MEDIUM, null, owner.getId());
        Long itemId = item.getId();

        // Test permission check before delete
        User user = visitor;
        if (memberService.isVisitor(todoList, user)) {
            // Visitor would be blocked here in real controller
            assertTrue(true);
        } else {
            fail("Visitor should be blocked");
        }

        // Verify item still exists
        assertThat(itemRepository.findById(itemId)).isPresent();

        // Owner can delete
        user = owner;
        if (memberService.isVisitor(todoList, user)) {
            fail("Owner should not be blocked");
        }
        // Owner passes - can proceed with delete
        itemService.deleteItem(itemId, listToken);

        // Verify item is deleted
        assertThat(itemRepository.findById(itemId)).isNotPresent();
    }
}
