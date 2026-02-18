#!/bin/bash

# 共享待办清单 V2-A 回归测试脚本（简化版）
# 测试日期: 2026-02-18

BASE_URL="http://localhost:8081"
PASSED=0
FAILED=0
TOTAL=0

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

pass_test() {
    echo -e "${GREEN}✓ PASS${NC}: $1"
    ((PASSED++))
    ((TOTAL++))
}

fail_test() {
    echo -e "${RED}✗ FAIL${NC}: $1"
    echo -e "${RED}  期望: $2${NC}"
    echo -e "${RED}  实际: $3${NC}"
    ((FAILED++))
    ((TOTAL++))
}

echo "=========================================="
echo "V2-A 回归测试报告"
echo "测试时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="
echo ""

# 测试1: 创建清单
echo "[测试1] 创建清单并验证自动生成title"
RESPONSE=$(curl -s -X POST "$BASE_URL/api/lists")
echo "响应: $RESPONSE"

if echo "$RESPONSE" | grep -q '"title"'; then
    TITLE=$(echo "$RESPONSE" | grep -o '"title":"[^"]*"' | cut -d'"' -f4)
    TOKEN=$(echo "$RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    pass_test "创建清单成功，title=$TITLE"
    TEST_TOKEN=$TOKEN
else
    fail_test "创建清单" "响应包含title字段" "title字段缺失"
    echo ""
    echo "应用可能未重启或数据库未迁移。请检查："
    echo "1. 应用是否已重启"
    echo "2. 数据库表结构是否已更新"
    echo "3. Entity类是否包含新字段"
    exit 1
fi
echo ""

# 测试2: 获取清单详情
echo "[测试2] 获取清单详情（验证新字段）"
RESPONSE=$(curl -s -X GET "$BASE_URL/api/lists/$TEST_TOKEN")
echo "响应: $RESPONSE"

if echo "$RESPONSE" | grep -q '"title"'; then
    pass_test "GET /lists/{token} 返回title字段"
else
    fail_test "GET /lists/{token}" "响应包含title" "title字段缺失"
fi
echo ""

# 测试3: 添加事项（V1兼容）
echo "[测试3] 添加事项（不传新字段，验证默认值）"
RESPONSE=$(curl -s -X POST "$BASE_URL/api/lists/$TEST_TOKEN/items" \
  -H "Content-Type: application/json" \
  -d '{"title": "Test Item V1"}')
echo "响应: $RESPONSE"

if echo "$RESPONSE" | grep -q '"priority":"MEDIUM"' && echo "$RESPONSE" | grep -q '"dueDate":null'; then
    ITEM_ID=$(echo "$RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    pass_test "添加事项成功，默认priority=MEDIUM, dueDate=null"
else
    fail_test "添加事项" "priority=MEDIUM, dueDate=null" "字段缺失或值不正确"
fi
echo ""

# 测试4: 添加事项（带优先级和截止日期）
echo "[测试4] 添加事项（设置priority和dueDate）"
RESPONSE=$(curl -s -X POST "$BASE_URL/api/lists/$TEST_TOKEN/items" \
  -H "Content-Type: application/json" \
  -d '{"title": "High Priority Task", "priority": "HIGH", "dueDate": "2026-02-20"}')
echo "响应: $RESPONSE"

if echo "$RESPONSE" | grep -q '"priority":"HIGH"' && echo "$RESPONSE" | grep -q '"dueDate":"2026-02-20"'; then
    ITEM_ID_2=$(echo "$RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    pass_test "添加事项成功，priority和dueDate正确设置"
else
    fail_test "添加事项（含新字段）" "priority=HIGH, dueDate=2026-02-20" "字段不正确"
fi
echo ""

# 测试5: 更新清单标题
echo "[测试5] 更新清单标题"
RESPONSE=$(curl -s -X PATCH "$BASE_URL/api/lists/$TEST_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": "Shopping List"}')
echo "响应: $RESPONSE"

if echo "$RESPONSE" | grep -q '"title":"Shopping List"'; then
    pass_test "更新清单标题成功"
else
    fail_test "更新清单标题" "title=Shopping List" "更新失败"
fi
echo ""

# 测试6: 更新事项优先级
echo "[测试6] 更新事项优先级"
RESPONSE=$(curl -s -X PATCH "$BASE_URL/api/items/$ITEM_ID_2" \
  -H "Content-Type: application/json" \
  -d '{"priority": "LOW"}')
echo "响应: $RESPONSE"

if echo "$RESPONSE" | grep -q '"priority":"LOW"'; then
    pass_test "更新优先级成功: HIGH -> LOW"
else
    fail_test "更新优先级" "priority=LOW" "更新失败"
fi
echo ""

# 测试7: 更新事项截止日期
echo "[测试7] 更新事项截止日期"
RESPONSE=$(curl -s -X PATCH "$BASE_URL/api/items/$ITEM_ID_2" \
  -H "Content-Type: application/json" \
  -d '{"dueDate": "2026-02-25"}')
echo "响应: $RESPONSE"

if echo "$RESPONSE" | grep -q '"dueDate":"2026-02-25"'; then
    pass_test "更新截止日期成功: 2026-02-20 -> 2026-02-25"
else
    fail_test "更新截止日期" "dueDate=2026-02-25" "更新失败"
fi
echo ""

# 测试8: 清除截止日期
echo "[测试8] 清除事项截止日期"
RESPONSE=$(curl -s -X PATCH "$BASE_URL/api/items/$ITEM_ID_2" \
  -H "Content-Type: application/json" \
  -d '{"dueDate": ""}')
echo "响应: $RESPONSE"

if echo "$RESPONSE" | grep -q '"dueDate":null'; then
    pass_test "清除截止日期成功"
else
    fail_test "清除截止日期" "dueDate=null" "清除失败"
fi
echo ""

# 测试9: 删除清单
echo "[测试9] 删除清单（级联删除）"
TEMP_RESPONSE=$(curl -s -X POST "$BASE_URL/api/lists")
TEMP_TOKEN=$(echo "$TEMP_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
curl -s -X POST "$BASE_URL/api/lists/$TEMP_TOKEN/items" \
  -H "Content-Type: application/json" \
  -d '{"title": "Temp Item"}' > /dev/null

HTTP_CODE=$(curl -s -w "%{http_code}" -X DELETE "$BASE_URL/api/lists/$TEMP_TOKEN" -o /dev/null)

if [ "$HTTP_CODE" = "204" ]; then
    # 验证清单已删除
    CHECK_CODE=$(curl -s -w "%{http_code}" -X GET "$BASE_URL/api/lists/$TEMP_TOKEN" -o /dev/null)
    if [ "$CHECK_CODE" = "404" ]; then
        pass_test "删除清单成功，级联删除所有事项"
    else
        fail_test "删除清单" "返回404" "清单仍然可访问(HTTP $CHECK_CODE)"
    fi
else
    fail_test "删除清单" "HTTP 204" "HTTP $HTTP_CODE"
fi
echo ""

# 测试10: 边界测试 - 空标题
echo "[测试10] 边界测试 - 清单标题为空"
HTTP_CODE=$(curl -s -w "%{http_code}" -X PATCH "$BASE_URL/api/lists/$TEST_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": ""}' -o /dev/null)

if [ "$HTTP_CODE" = "400" ]; then
    pass_test "空标题被正确拒绝 (HTTP 400)"
else
    fail_test "空标题验证" "HTTP 400" "HTTP $HTTP_CODE"
fi
echo ""

# 测试11: 边界测试 - 无效优先级
echo "[测试11] 边界测试 - 无效优先级"
HTTP_CODE=$(curl -s -w "%{http_code}" -X POST "$BASE_URL/api/lists/$TEST_TOKEN/items" \
  -H "Content-Type: application/json" \
  -d '{"title": "Test", "priority": "INVALID"}' -o /dev/null)

if [ "$HTTP_CODE" = "400" ]; then
    pass_test "无效优先级被正确拒绝 (HTTP 400)"
else
    fail_test "无效优先级验证" "HTTP 400" "HTTP $HTTP_CODE"
fi
echo ""

# 测试12: 边界测试 - 无效日期格式
echo "[测试12] 边界测试 - 无效日期格式"
HTTP_CODE=$(curl -s -w "%{http_code}" -X POST "$BASE_URL/api/lists/$TEST_TOKEN/items" \
  -H "Content-Type: application/json" \
  -d '{"title": "Test", "dueDate": "2026/02/18"}' -o /dev/null)

if [ "$HTTP_CODE" = "400" ]; then
    pass_test "无效日期格式被正确拒绝 (HTTP 400)"
else
    fail_test "无效日期格式验证" "HTTP 400" "HTTP $HTTP_CODE"
fi
echo ""

# 测试13: 边界测试 - 无效日期值
echo "[测试13] 边界测试 - 无效日期值(2026-02-30)"
HTTP_CODE=$(curl -s -w "%{http_code}" -X POST "$BASE_URL/api/lists/$TEST_TOKEN/items" \
  -H "Content-Type: application/json" \
  -d '{"title": "Test", "dueDate": "2026-02-30"}' -o /dev/null)

if [ "$HTTP_CODE" = "400" ]; then
    pass_test "无效日期值被正确拒绝 (HTTP 400)"
else
    fail_test "无效日期值验证" "HTTP 400" "HTTP $HTTP_CODE"
fi
echo ""

# 测试14: 404测试 - 不存在的token
echo "[测试14] 404测试 - 不存在的清单token"
HTTP_CODE=$(curl -s -w "%{http_code}" -X GET "$BASE_URL/api/lists/nonexistenttoken" -o /dev/null)

if [ "$HTTP_CODE" = "404" ]; then
    pass_test "不存在的token返回404"
else
    fail_test "404测试" "HTTP 404" "HTTP $HTTP_CODE"
fi
echo ""

# 测试15: 向后兼容性 - V1客户端
echo "[测试15] 向后兼容性 - V1客户端不传新字段"
RESPONSE=$(curl -s -X POST "$BASE_URL/api/lists/$TEST_TOKEN/items" \
  -H "Content-Type: application/json" \
  -d '{"title": "V1 Client"}')
echo "响应: $RESPONSE"

if echo "$RESPONSE" | grep -q '"priority":"MEDIUM"' && echo "$RESPONSE" | grep -q '"dueDate":null'; then
    pass_test "V1客户端兼容模式正常，默认值正确"
else
    fail_test "V1兼容性" "priority=MEDIUM, dueDate=null" "默认值不正确"
fi
echo ""

# 测试总结
echo "=========================================="
echo "测试总结"
echo "=========================================="
echo "总测试数: $TOTAL"
echo -e "${GREEN}通过: $PASSED${NC}"
echo -e "${RED}失败: $FAILED${NC}"

if [ $TOTAL -gt 0 ]; then
    PASS_RATE=$(awk "BEGIN {printf \"%.2f\", ($PASSED/$TOTAL)*100}")
    echo "通过率: $PASS_RATE%"
else
    echo "通过率: N/A"
fi
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}✓ 所有测试通过！${NC}"
    echo -e "${GREEN}========================================${NC}"
    exit 0
else
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}✗ 存在失败的测试用例${NC}"
    echo -e "${RED}========================================${NC}"
    exit 1
fi
