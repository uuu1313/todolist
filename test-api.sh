#!/bin/bash

# 共享待办清单 API 测试脚本
# 使用前请确保应用已经启动 (mvn spring-boot:run)

echo "==================================="
echo "  共享待办清单 API 测试脚本"
echo "==================================="
echo ""

BASE_URL="http://localhost:8080"

# 检查应用是否运行
echo "检查应用状态..."
if ! curl -s "$BASE_URL/api/lists" > /dev/null 2>&1; then
    echo "错误: 应用未运行。请先运行 'mvn spring-boot:run' 启动应用。"
    exit 1
fi
echo "应用运行正常 ✓"
echo ""

# API 1: 创建清单
echo "==================================="
echo "API 1: 创建清单"
echo "==================================="
RESPONSE=$(curl -s -X POST "$BASE_URL/api/lists")
echo "$RESPONSE" | python -m json.tool 2>/dev/null || echo "$RESPONSE"
TOKEN=$(echo "$RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo ""
echo "获取到 Token: $TOKEN"
echo ""

# API 4: 添加事项
echo "==================================="
echo "API 4: 添加事项 1"
echo "==================================="
curl -s -X POST "$BASE_URL/api/lists/$TOKEN/items" \
  -H "Content-Type: application/json; charset=UTF-8" \
  -d '{"title": "Buy milk"}' | python -m json.tool 2>/dev/null
echo ""

echo "==================================="
echo "API 4: 添加事项 2"
echo "==================================="
curl -s -X POST "$BASE_URL/api/lists/$TOKEN/items" \
  -H "Content-Type: application/json; charset=UTF-8" \
  -d '{"title": "Buy eggs"}' | python -m json.tool 2>/dev/null
echo ""

# API 3: 获取所有事项
echo "==================================="
echo "API 3: 获取所有事项"
echo "==================================="
curl -s "$BASE_URL/api/lists/$TOKEN/items" | python -m json.tool 2>/dev/null
echo ""

# API 2: 获取清单详情
echo "==================================="
echo "API 2: 获取清单详情"
echo "==================================="
curl -s "$BASE_URL/api/lists/$TOKEN" | python -m json.tool 2>/dev/null
echo ""

# API 5: 更新事项
echo "==================================="
echo "API 5: 更新事项 (标记为完成)"
echo "==================================="
curl -s -X PATCH "$BASE_URL/api/items/1" \
  -H "Content-Type: application/json; charset=UTF-8" \
  -d '{"completed": true}' | python -m json.tool 2>/dev/null
echo ""

# API 6: 删除事项
echo "==================================="
echo "API 6: 删除事项"
echo "==================================="
HTTP_CODE=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/api/items/2")
echo "HTTP 状态码: $(echo "$HTTP_CODE" | tail -1)"
echo ""

# 测试错误处理
echo "==================================="
echo "测试错误处理"
echo "==================================="

echo "测试 1: 访问不存在的清单 (404)"
curl -s "$BASE_URL/api/lists/invalidtoken" | python -m json.tool 2>/dev/null
echo ""

echo "测试 2: 空标题 (400)"
curl -s -X POST "$BASE_URL/api/lists/$TOKEN/items" \
  -H "Content-Type: application/json; charset=UTF-8" \
  -d '{"title": ""}' | python -m json.tool 2>/dev/null
echo ""

echo "测试 3: 访问不存在的事项 (404)"
curl -s -X PATCH "$BASE_URL/api/items/99999" \
  -H "Content-Type: application/json; charset=UTF-8" \
  -d '{"completed": true}' | python -m json.tool 2>/dev/null
echo ""

echo "==================================="
echo "  测试完成！"
echo "==================================="
echo ""
echo "Web 界面:"
echo "  首页: $BASE_URL/"
echo "  清单: $BASE_URL/lists/$TOKEN"
echo ""
echo "H2 控制台:"
echo "  URL: $BASE_URL/h2-console"
echo "  JDBC URL: jdbc:h2:mem:todolist"
echo "  用户名: sa"
echo "  密码: (留空)"
