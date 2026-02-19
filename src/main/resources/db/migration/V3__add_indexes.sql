-- V3: 添加数据库索引以提升查询性能
-- 创建时间: 2026-02-19

-- 为 list_member.user_id 添加索引（频繁查询 findByUserId）
CREATE INDEX idx_list_member_user_id ON list_member(user_id);

-- 为 todo_item.list_id 添加索引（关联查询）
CREATE INDEX idx_todo_item_list_id ON todo_item(list_id);

-- 为 invite_token.token 添加索引（查询邀请链接）
CREATE INDEX idx_invite_token_token ON invite_token(token);
