#!/bin/bash
echo "Creating list..."
RESPONSE=$(curl -s http://localhost:8080/api/lists -X POST)
echo "Response: $RESPONSE"
TOKEN=$(echo $RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo "Token: $TOKEN"

echo ""
echo "Adding item..."
curl -s -X POST "http://localhost:8080/api/lists/$TOKEN/items" \
  -H "Content-Type: application/json" \
  -d '{"title": "买牛奶"}'
