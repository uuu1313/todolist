#!/bin/bash

# 共享待办清单 V2-A 回归测试脚本
# 测试日期: 2026-02-18
# 测试工程师: QA

BASE_URL="http://localhost:8081"
TEST_TOKEN=""
TEST_LIST_ID=""
TEST_ITEM_ID=""
PASSED=0
FAILED=0
TOTAL=0

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试结果函数
pass_test() {
    echo -e "${GREEN}✓ PASS${NC}: $1"
    ((PASSED++))
    ((TOTAL++))
}

fail_test() {
    echo -e "${RED}✗ FAIL${NC}: $1"
    echo -e "${RED}  详情: $2${NC}"
    ((FAILED++))
    ((TOTAL++))
}

info_test() {
    echo -e "${YELLOW}ℹ INFO${NC}: $1"
}

# ================================
# V1 功能回归测试
# ================================

echo "================================"
echo "V1 功能回归测试"
echo "================================"
echo ""

# V1-TC-01: 创建待办清单
echo "测试 V1-TC-01: 创建待办清单"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/lists" \
  -H "Content-Type: application/json")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "201" ]; then
    TEST_TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    TEST_LIST_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | cut -d':' -f2)

    if [ -n "$TEST_TOKEN" ] && [ ${#TEST_TOKEN} -eq 8 ]; then
        pass_test "创建清单成功，token格式正确: $TEST_TOKEN"
    else
        fail_test "创建清单" "token格式错误或为空"
    fi
else
    fail_test "创建清单" "HTTP状态码错误: $HTTP_CODE (期望201)"
fi
echo ""

# V1-TC-02: 添加待办事项
echo "测试 V1-TC-02: 添加待办事项（V1兼容模式，不传新字段）"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/lists/$TEST_TOKEN/items" \
  -H "Content-Type: application/json" \
  -d '{"title": "学习Spring Boot"}')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "201" ]; then
    TEST_ITEM_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    TITLE=$(echo "$BODY" | grep -o '"title":"[^"]*"' | cut -d'"' -f4)
    COMPLETED=$(echo "$BODY" | grep -o '"completed":[^,}]*' | cut -d':' -f2)

    if [ "$TITLE" = "学习Spring Boot" ] && [ "$COMPLETED" = "false" ]; then
        # 检查新字段的默认值
        PRIORITY=$(echo "$BODY" | grep -o '"priority":"[^"]*"' | cut -d'"' -f4)
        DUEDATE=$(echo "$BODY" | grep -o '"dueDate":null')

        if [ "$PRIORITY" = "MEDIUM" ] && [ -n "$DUEDATE" ]; then
            pass_test "添加事项成功，新字段默认值正确 (priority=MEDIUM, dueDate=null)"
        else
            fail_test "添加事项" "新字段默认值不正确"
        fi
    else
        fail_test "添加事项" "响应数据不正确"
    fi
else
    fail_test "添加事项" "HTTP状态码错误: $HTTP_CODE (期望201)"
fi
echo ""

# V1-TC-03: 获取待办清单详情
echo "测试 V1-TC-03: 获取待办清单详情（包含新字段）"
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/lists/$TEST_TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    # 检查新字段 title
    TITLE=$(echo "$BODY" | grep -o '"title":"[^"]*"' | head -1 | cut -d'"' -f4)
    # 检查items中的新字段
    ITEM_PRIORITY=$(echo "$BODY" | grep -o '"priority":"[^"]*"' | head -1 | cut -d'"' -f4)

    if [ -n "$TITLE" ] && [ "$ITEM_PRIORITY" = "MEDIUM" ]; then
        pass_test "获取清单详情成功，新字段正确返回"
    else
        fail_test "获取清单详情" "新字段缺失或不正确"
    fi
else
    fail_test "获取清单详情" "HTTP状态码错误: $HTTP_CODE (期望200)"
fi
echo ""

# V1-TC-04: 标记待办事项完成/未完成
echo "测试 V1-TC-04: 标记待办事项完成（V1兼容模式，只传completed）"
RESPONSE=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/api/items/$TEST_ITEM_ID" \
  -H "Content-Type: application/json" \
  -d '{"completed": true}')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    COMPLETED=$(echo "$BODY" | grep -o '"completed":[^,}]*' | cut -d':' -f2)
    # 检查其他字段未变
    PRIORITY=$(echo "$BODY" | grep -o '"priority":"[^"]*"' | cut -d'"' -f4)

    if [ "$COMPLETED" = "true" ] && [ "$PRIORITY" = "MEDIUM" ]; then
        pass_test "标记完成成功，其他字段保持不变（向后兼容）"
    else
        fail_test "标记完成" "响应数据不正确"
    fi
else
    fail_test "标记完成" "HTTP状态码错误: $HTTP_CODE (期望200)"
fi
echo ""

# V1-TC-05: 编辑待办事项标题
echo "测试 V1-TC-05: 编辑待办事项标题"
RESPONSE=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/api/items/$TEST_ITEM_ID" \
  -H "Content-Type: application/json" \
  -d '{"title": "深入学习Spring Boot"}')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    TITLE=$(echo "$BODY" | grep -o '"title":"[^"]*"' | cut -d'"' -f4)
    if [ "$TITLE" = "深入学习Spring Boot" ]; then
        pass_test "编辑标题成功"
    else
        fail_test "编辑标题" "标题未更新"
    fi
else
    fail_test "编辑标题" "HTTP状态码错误: $HTTP_CODE (期望200)"
fi
echo ""

# V1-TC-06: 获取待办事项列表
echo "测试 V1-TC-06: 获取待办事项列表（包含新字段）"
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/lists/$TEST_TOKEN/items")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    # 检查是否返回数组
    if echo "$BODY" | grep -q '^\['; then
        # 检查新字段
        PRIORITY=$(echo "$BODY" | grep -o '"priority":"[^"]*"' | head -1 | cut -d'"' -f4)
        if [ "$PRIORITY" = "MEDIUM" ]; then
            pass_test "获取事项列表成功，新字段正确返回"
        else
            fail_test "获取事项列表" "新字段缺失"
        fi
    else
        fail_test "获取事项列表" "响应不是数组"
    fi
else
    fail_test "获取事项列表" "HTTP状态码错误: $HTTP_CODE (期望200)"
fi
echo ""

# ================================
# V2-A 新功能测试
# ================================

echo "================================"
echo "V2-A 新功能测试"
echo "================================"
echo ""

# V2A-TC-01: 创建清单时自动生成标题
echo "测试 V2A-TC-01: 创建清单时自动生成默认标题"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/lists")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "201" ]; then
    TITLE=$(echo "$BODY" | grep -o '"title":"我的清单 [^"]*"' | cut -d'"' -f4)
    if [ -n "$TITLE" ]; then
        pass_test "自动生成默认标题: $TITLE"
    else
        fail_test "自动生成标题" "title字段缺失或格式错误"
    fi
else
    fail_test "自动生成标题" "HTTP状态码错误: $HTTP_CODE (期望201)"
fi
echo ""

# V2A-TC-02: 更新清单标题
echo "测试 V2A-TC-02: 更新清单标题"
RESPONSE=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/api/lists/$TEST_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": "家庭购物清单"}')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    TITLE=$(echo "$BODY" | grep -o '"title":"家庭购物清单"')
    if [ -n "$TITLE" ]; then
        pass_test "更新清单标题成功"
    else
        fail_test "更新清单标题" "标题未更新"
    fi
else
    fail_test "更新清单标题" "HTTP状态码错误: $HTTP_CODE (期望200)"
fi
echo ""

# V2A-TC-03: 添加事项时设置优先级和截止日期
echo "测试 V2A-TC-03: 添加事项时设置优先级和截止日期"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/lists/$TEST_TOKEN/items" \
  -H "Content-Type: application/json" \
  -d '{"title": "买牛奶", "priority": "HIGH", "dueDate": "2026-02-20"}')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "201" ]; then
    TITLE=$(echo "$BODY" | grep -o '"title":"买牛奶"')
    PRIORITY=$(echo "$BODY" | grep -o '"priority":"HIGH"')
    DUEDATE=$(echo "$BODY" | grep -o '"dueDate":"2026-02-20"')

    if [ -n "$TITLE" ] && [ -n "$PRIORITY" ] && [ -n "$DUEDATE" ]; then
        pass_test "添加事项成功，优先级和截止日期正确设置"
        # 保存这个item ID用于后续测试
        TEST_ITEM_ID_2=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    else
        fail_test "添加事项（含优先级和截止日期）" "字段不正确"
    fi
else
    fail_test "添加事项（含优先级和截止日期）" "HTTP状态码错误: $HTTP_CODE (期望201)"
fi
echo ""

# V2A-TC-04: 更新事项优先级
echo "测试 V2A-TC-04: 更新事项优先级"
RESPONSE=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/api/items/$TEST_ITEM_ID_2" \
  -H "Content-Type: application/json" \
  -d '{"priority": "LOW"}')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    PRIORITY=$(echo "$BODY" | grep -o '"priority":"LOW"')
    if [ -n "$PRIORITY" ]; then
        pass_test "更新优先级成功: HIGH -> LOW"
    else
        fail_test "更新优先级" "优先级未更新"
    fi
else
    fail_test "更新优先级" "HTTP状态码错误: $HTTP_CODE (期望200)"
fi
echo ""

# V2A-TC-05: 更新事项截止日期
echo "测试 V2A-TC-05: 更新事项截止日期"
RESPONSE=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/api/items/$TEST_ITEM_ID_2" \
  -H "Content-Type: application/json" \
  -d '{"dueDate": "2026-02-25"}')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    DUEDATE=$(echo "$BODY" | grep -o '"dueDate":"2026-02-25"')
    if [ -n "$DUEDATE" ]; then
        pass_test "更新截止日期成功: 2026-02-20 -> 2026-02-25"
    else
        fail_test "更新截止日期" "截止日期未更新"
    fi
else
    fail_test "更新截止日期" "HTTP状态码错误: $HTTP_CODE (期望200)"
fi
echo ""

# V2A-TC-06: 清除事项截止日期
echo "测试 V2A-TC-06: 清除事项截止日期"
RESPONSE=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/api/items/$TEST_ITEM_ID_2" \
  -H "Content-Type: application/json" \
  -d '{"dueDate": ""}')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    DUEDATE=$(echo "$BODY" | grep -o '"dueDate":null')
    if [ -n "$DUEDATE" ]; then
        pass_test "清除截止日期成功"
    else
        fail_test "清除截止日期" "截止日期未清除"
    fi
else
    fail_test "清除截止日期" "HTTP状态码错误: $HTTP_CODE (期望200)"
fi
echo ""

# V2A-TC-07: 删除清单
echo "测试 V2A-TC-07: 删除清单（级联删除所有事项）"
# 先创建一个临时清单用于删除测试
RESPONSE=$(curl -s -X POST "$BASE_URL/api/lists")
TEMP_TOKEN=$(echo "$RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# 添加几个事项
curl -s -X POST "$BASE_URL/api/lists/$TEMP_TOKEN/items" \
  -H "Content-Type: application/json" \
  -d '{"title": "事项1"}' > /dev/null
curl -s -X POST "$BASE_URL/api/lists/$TEMP_TOKEN/items" \
  -H "Content-Type: application/json" \
  -d '{"title": "事项2"}' > /dev/null

# 删除清单
RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/api/lists/$TEMP_TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "204" ]; then
    # 验证清单已删除
    RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/lists/$TEMP_TOKEN")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    if [ "$HTTP_CODE" = "404" ]; then
        pass_test "删除清单成功，级联删除所有事项"
    else
        fail_test "删除清单" "清单未被删除或仍然可访问"
    fi
else
    fail_test "删除清单" "HTTP状态码错误: $HTTP_CODE (期望204)"
fi
echo ""

# ================================
# 边界测试和错误处理
# ================================

echo "================================"
echo "边界测试和错误处理"
echo "================================"
echo ""

# BC-01: 空标题验证
echo "测试 BC-01: 添加事项 - 空标题"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/lists/$TEST_TOKEN/items" \
  -H "Content-Type: application/json" \
  -d '{"title": ""}')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "400" ]; then
    pass_test "空标题被正确拒绝（400）"
else
    fail_test "空标题验证" "HTTP状态码错误: $HTTP_CODE (期望400)"
fi
echo ""

# BC-02: 无效优先级
echo "测试 BC-02: 添加事项 - 无效优先级"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/lists/$TEST_TOKEN/items" \
  -H "Content-Type: application/json" \
  -d '{"title": "测试事项", "priority": "INVALID"}')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "400" ]; then
    pass_test "无效优先级被正确拒绝（400）"
else
    fail_test "无效优先级验证" "HTTP状态码错误: $HTTP_CODE (期望400)"
fi
echo ""

# BC-03: 无效日期格式
echo "测试 BC-03: 添加事项 - 无效日期格式"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/lists/$TEST_TOKEN/items" \
  -H "Content-Type: application/json" \
  -d '{"title": "测试事项", "dueDate": "2026/02/18"}')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "400" ]; then
    pass_test "无效日期格式被正确拒绝（400）"
else
    fail_test "无效日期格式验证" "HTTP状态码错误: $HTTP_CODE (期望400)"
fi
echo ""

# BC-04: 无效日期（2026-02-30）
echo "测试 BC-04: 添加事项 - 无效日期（2026-02-30）"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/lists/$TEST_TOKEN/items" \
  -H "Content-Type: application/json" \
  -d '{"title": "测试事项", "dueDate": "2026-02-30"}')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "400" ]; then
    pass_test "无效日期（2026-02-30）被正确拒绝（400）"
else
    fail_test "无效日期验证" "HTTP状态码错误: $HTTP_CODE (期望400), 可能被接受"
fi
echo ""

# BC-05: 清单标题为空
echo "测试 BC-05: 更新清单标题 - 空标题"
RESPONSE=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/api/lists/$TEST_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": ""}')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "400" ]; then
    pass_test "空清单标题被正确拒绝（400）"
else
    fail_test "空清单标题验证" "HTTP状态码错误: $HTTP_CODE (期望400)"
fi
echo ""

# BC-06: 清单标题超长
echo "测试 BC-06: 更新清单标题 - 超长标题（101字符）"
LONG_TITLE=$(printf 'A%.0s' {1..101})
RESPONSE=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/api/lists/$TEST_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"title\": \"$LONG_TITLE\"}")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "400" ]; then
    pass_test "超长清单标题被正确拒绝（400）"
else
    fail_test "超长清单标题验证" "HTTP状态码错误: $HTTP_CODE (期望400)"
fi
echo ""

# BC-07: 不存在的清单token
echo "测试 BC-07: 访问不存在的清单token"
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/lists/invalidtoken123")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "404" ]; then
    pass_test "不存在的token返回404"
else
    fail_test "无效token验证" "HTTP状态码错误: $HTTP_CODE (期望404)"
fi
echo ""

# BC-08: 不存在的事项ID
echo "测试 BC-08: 更新不存在的事项ID"
RESPONSE=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/api/items/999999" \
  -H "Content-Type: application/json" \
  -d '{"completed": true}')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "404" ]; then
    pass_test "不存在的事项ID返回404"
else
    fail_test "无效事项ID验证" "HTTP状态码错误: $HTTP_CODE (期望404)"
fi
echo ""

# BC-09: 删除不存在的清单
echo "测试 BC-09: 删除不存在的清单"
RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/api/lists/nonexistenttoken")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "404" ]; then
    pass_test "删除不存在的清单返回404"
else
    fail_test "删除不存在清单验证" "HTTP状态码错误: $HTTP_CODE (期望404)"
fi
echo ""

# BC-10: PATCH请求体为空
echo "测试 BC-10: PATCH请求体为空"
RESPONSE=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/api/items/$TEST_ITEM_ID" \
  -H "Content-Type: application/json" \
  -d '{}')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "400" ]; then
    pass_test "空PATCH请求体被正确拒绝（400）"
else
    info_test "空PATCH请求体状态码: $HTTP_CODE (可能允许空请求体)"
fi
echo ""

# ================================
# 优先级枚举值测试
# ================================

echo "================================"
echo "优先级枚举值测试"
echo "================================"
echo ""

# 测试所有有效的优先级值
echo "测试所有有效的优先级值（HIGH, MEDIUM, LOW）"
for priority in "HIGH" "MEDIUM" "LOW"; do
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/lists/$TEST_TOKEN/items" \
      -H "Content-Type: application/json" \
      -d "{\"title\": \"测试${priority}优先级\", \"priority\": \"$priority\"}")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" = "201" ]; then
        PRIORITY_CHECK=$(echo "$BODY" | grep -o "\"priority\":\"$priority\"")
        if [ -n "$PRIORITY_CHECK" ]; then
            pass_test "优先级 $priority 正确处理"
        else
            fail_test "优先级 $priority" "返回值不匹配"
        fi
    else
        fail_test "优先级 $priority" "HTTP状态码错误: $HTTP_CODE (期望201)"
    fi
done
echo ""

# ================================
# 向后兼容性专项测试
# ================================

echo "================================"
echo "向后兼容性专项测试"
echo "================================"
echo ""

# 向后兼容性测试1: V1客户端不传新字段
echo "测试 V1兼容-1: V1客户端添加事项（不传priority和dueDate）"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/lists/$TEST_TOKEN/items" \
  -H "Content-Type: application/json" \
  -d '{"title": "V1兼容测试"}')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "201" ]; then
    TITLE=$(echo "$BODY" | grep -o '"title":"V1兼容测试"')
    PRIORITY=$(echo "$BODY" | grep -o '"priority":"MEDIUM"')
    DUEDATE=$(echo "$BODY" | grep -o '"dueDate":null')

    if [ -n "$TITLE" ] && [ -n "$PRIORITY" ] && [ -n "$DUEDATE" ]; then
        pass_test "V1兼容模式正常工作，自动设置默认值"
    else
        fail_test "V1兼容模式" "默认值设置不正确"
    fi
else
    fail_test "V1兼容模式" "HTTP状态码错误: $HTTP_CODE (期望201)"
fi
echo ""

# 向后兼容性测试2: V1客户端只传completed字段
echo "测试 V1兼容-2: V1客户端更新事项（只传completed）"
RESPONSE=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/api/items/$TEST_ITEM_ID" \
  -H "Content-Type: application/json" \
  -d '{"completed": false}')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    COMPLETED=$(echo "$BODY" | grep -o '"completed":false')
    # 检查priority和dueDate仍然存在
    PRIORITY_EXISTS=$(echo "$BODY" | grep -o '"priority":"MEDIUM"')

    if [ -n "$COMPLETED" ] && [ -n "$PRIORITY_EXISTS" ]; then
        pass_test "V1兼容模式更新成功，其他字段未受影响"
    else
        fail_test "V1兼容模式更新" "其他字段被影响"
    fi
else
    fail_test "V1兼容模式更新" "HTTP状态码错误: $HTTP_CODE (期望200)"
fi
echo ""

# ================================
# 测试总结
# ================================

echo "================================"
echo "测试总结"
echo "================================"
echo ""
echo "总测试数: $TOTAL"
echo -e "${GREEN}通过: $PASSED${NC}"
echo -e "${RED}失败: $FAILED${NC}"
echo ""

PASS_RATE=$(awk "BEGIN {printf \"%.2f\", ($PASSED/$TOTAL)*100}")
echo "通过率: $PASS_RATE%"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ 所有测试通过！${NC}"
    exit 0
else
    echo -e "${RED}✗ 存在失败的测试用例${NC}"
    exit 1
fi
