package com.example.todolist;

import com.example.todolist.entity.*;
import com.example.todolist.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.transaction.annotation.*;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 排序测试：验证同级别事项按创建时间升序排列
 *
 * 验收标准：
 * 1. 新增 A、B、C 后列表顺序为 A 在上、C 在下
 * 2. 页面刷新后顺序不变（稳定性）
 * 3. 现有 completed/priority 分组行为不变
 */
@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TodoItemRepositorySortTest {

    @Autowired
    private TodoItemRepository itemRepository;

    @Autowired
    private TodoListRepository listRepository;

    private TodoList testList;

    @BeforeEach
    void setUp() {
        // 创建测试列表
        testList = new TodoList();
        testList.setTitle("Test List");
        testList.setToken(generateToken());
        testList = listRepository.save(testList);
    }

    /**
     * 生成 8 字符的随机 token
     */
    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        itemRepository.deleteAll();
        listRepository.deleteAll();
    }

    /**
     * 测试1：同优先级连续创建 A/B/C，返回顺序应为 A,B,C
     * 核心测试：验证修复后的排序逻辑
     */
    @Test
    @Order(1)
    void testSamePriorityItemsOrderedByCreatedAtAsc() {
        // 连续创建三个同优先级事项
        TodoItem itemA = createAndSaveItem("Item A", Priority.MEDIUM, null,
            LocalDateTime.of(2025, 1, 1, 10, 0));
        TodoItem itemB = createAndSaveItem("Item B", Priority.MEDIUM, null,
            LocalDateTime.of(2025, 1, 1, 11, 0));
        TodoItem itemC = createAndSaveItem("Item C", Priority.MEDIUM, null,
            LocalDateTime.of(2025, 1, 1, 12, 0));

        // 查询并验证顺序：A(旧) -> B -> C(新)
        List<TodoItem> items = itemRepository.findByListId(testList.getId());

        assertEquals(3, items.size());
        assertEquals("Item A", items.get(0).getTitle());
        assertEquals("Item B", items.get(1).getTitle());
        assertEquals("Item C", items.get(2).getTitle());
    }

    /**
     * 测试2：刷新/重复查询顺序保持一致（稳定性）
     */
    @Test
    @Order(2)
    void testOrderConsistencyAcrossMultipleQueries() {
        createAndSaveItem("First", Priority.MEDIUM, null,
            LocalDateTime.now().minusSeconds(30));
        createAndSaveItem("Second", Priority.MEDIUM, null,
            LocalDateTime.now().minusSeconds(20));
        createAndSaveItem("Third", Priority.MEDIUM, null,
            LocalDateTime.now().minusSeconds(10));

        // 多次查询验证顺序一致性
        List<TodoItem> firstQuery = itemRepository.findByListId(testList.getId());
        List<TodoItem> secondQuery = itemRepository.findByListId(testList.getId());
        List<TodoItem> thirdQuery = itemRepository.findByListId(testList.getId());

        // 验证每次查询结果一致
        assertEquals(firstQuery, secondQuery);
        assertEquals(secondQuery, thirdQuery);
        assertEquals("First", firstQuery.get(0).getTitle());
        assertEquals("Second", firstQuery.get(1).getTitle());
        assertEquals("Third", firstQuery.get(2).getTitle());
    }

    /**
     * 测试3：不破坏 completed 既有排序（未完成在前）
     */
    @Test
    @Order(3)
    void testCompletedGroupingNotBroken() {
        // 先创建已完成项
        TodoItem completed = createAndSaveItem("Completed", Priority.HIGH, null,
            LocalDateTime.now().minusHours(2));
        completed.setCompleted(true);
        itemRepository.save(completed);

        // 后创建未完成项
        createAndSaveItem("Active", Priority.HIGH, null, LocalDateTime.now().minusHours(1));

        List<TodoItem> items = itemRepository.findByListId(testList.getId());

        // 未完成项应在前（completed ASC），即使创建时间更晚
        assertEquals(2, items.size());
        assertFalse(items.get(0).getCompleted());
        assertTrue(items.get(1).getCompleted());
    }

    /**
     * 测试4：不破坏 priority 既有排序（HIGH > MEDIUM > LOW）
     */
    @Test
    @Order(4)
    void testPriorityGroupingNotBroken() {
        // 创建不同优先级的项，LOW 最先创建
        createAndSaveItem("Low Priority", Priority.LOW, null,
            LocalDateTime.now().minusHours(3));
        createAndSaveItem("High Priority", Priority.HIGH, null,
            LocalDateTime.now().minusHours(2));
        createAndSaveItem("Medium Priority", Priority.MEDIUM, null,
            LocalDateTime.now().minusHours(1));

        List<TodoItem> items = itemRepository.findByListId(testList.getId());

        // HIGH(1) > MEDIUM(2) > LOW(3)，优先级高于创建时间
        assertEquals(3, items.size());
        assertEquals("High Priority", items.get(0).getTitle());
        assertEquals("Medium Priority", items.get(1).getTitle());
        assertEquals("Low Priority", items.get(2).getTitle());
    }

    /**
     * 测试5：混合场景 - completed + priority + createdAt 综合排序
     */
    @Test
    @Order(5)
    void testMixedSortingScenario() {
        // 未完成 HIGH 组：先旧后新
        TodoItem h1 = createAndSaveItem("H-Old", Priority.HIGH, null,
            LocalDateTime.of(2025, 1, 1, 10, 0));
        TodoItem h2 = createAndSaveItem("H-New", Priority.HIGH, null,
            LocalDateTime.of(2025, 1, 1, 11, 0));

        // 未完成 MEDIUM 组：先旧后新
        TodoItem m1 = createAndSaveItem("M-Old", Priority.MEDIUM, null,
            LocalDateTime.of(2025, 1, 1, 12, 0));
        TodoItem m2 = createAndSaveItem("M-New", Priority.MEDIUM, null,
            LocalDateTime.of(2025, 1, 1, 13, 0));

        // 已完成项
        TodoItem ch = createAndSaveItem("Completed-H", Priority.HIGH, null,
            LocalDateTime.of(2025, 1, 1, 9, 0)); // 创建时间更早
        ch.setCompleted(true);
        itemRepository.save(ch);

        List<TodoItem> items = itemRepository.findByListId(testList.getId());

        assertEquals(5, items.size());

        // 验证完整顺序
        assertEquals("H-Old", items.get(0).getTitle());     // 未完成 HIGH，旧
        assertEquals("H-New", items.get(1).getTitle());     // 未完成 HIGH，新
        assertEquals("M-Old", items.get(2).getTitle());     // 未完成 MEDIUM，旧
        assertEquals("M-New", items.get(3).getTitle());     // 未完成 MEDIUM，新
        assertEquals("Completed-H", items.get(4).getTitle()); // 已完成（最后）
    }

    /**
     * 测试6：dueDate NULLS LAST 规则保持不变
     */
    @Test
    @Order(6)
    void testDueDateNullsLastRule() {
        createAndSaveItem("Has Due Date", Priority.MEDIUM,
            LocalDate.of(2025, 12, 31),
            LocalDateTime.now().minusMinutes(10));
        createAndSaveItem("No Due Date", Priority.MEDIUM, null,
            LocalDateTime.now());

        List<TodoItem> items = itemRepository.findByListId(testList.getId());

        // 有截止日期的应排在前面（NULLS LAST）
        assertEquals(2, items.size());
        assertEquals("Has Due Date", items.get(0).getTitle());
        assertEquals("No Due Date", items.get(1).getTitle());
    }

    /**
     * 测试7：同组内创建时间升序（核心回归测试）
     * 验证修复前行为：新建项在上
     * 验证修复后行为：新建项在下
     */
    @Test
    @Order(7)
    void testNewItemsAppearAtBottomInSameGroup() {
        // 模拟用户连续操作：创建 A -> 刷新 -> 创建 B -> 刷新 -> 创建 C
        TodoItem a = createAndSaveItem("Task A", Priority.MEDIUM, null,
            LocalDateTime.now().minusMinutes(3));
        List<TodoItem> afterA = itemRepository.findByListId(testList.getId());
        assertEquals(1, afterA.size());
        assertEquals("Task A", afterA.get(0).getTitle());

        TodoItem b = createAndSaveItem("Task B", Priority.MEDIUM, null,
            LocalDateTime.now().minusMinutes(2));
        List<TodoItem> afterB = itemRepository.findByListId(testList.getId());
        assertEquals(2, afterB.size());
        assertEquals("Task A", afterB.get(0).getTitle()); // A 在上（旧）
        assertEquals("Task B", afterB.get(1).getTitle()); // B 在下（新）

        TodoItem c = createAndSaveItem("Task C", Priority.MEDIUM, null,
            LocalDateTime.now().minusMinutes(1));
        List<TodoItem> afterC = itemRepository.findByListId(testList.getId());
        assertEquals(3, afterC.size());
        assertEquals("Task A", afterC.get(0).getTitle()); // A 在上（最旧）
        assertEquals("Task B", afterC.get(1).getTitle()); // B 在中间
        assertEquals("Task C", afterC.get(2).getTitle()); // C 在下（最新）
    }

    /**
     * 辅助方法：创建并保存 TodoItem，指定创建时间
     */
    private TodoItem createAndSaveItem(String title, Priority priority,
                                       LocalDate dueDate, LocalDateTime createdAt) {
        TodoItem item = new TodoItem();
        item.setTitle(title);
        item.setPriority(priority);
        item.setDueDate(dueDate);
        item.setCompleted(false);
        item.setList(testList);
        item.setCreatedAt(createdAt);
        item.setUpdatedAt(createdAt);
        return itemRepository.save(item);
    }
}
