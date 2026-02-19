-- 创建 user 表 (USER 是保留字，需要加引号)
CREATE TABLE "user" (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

-- 创建 list_member 表
CREATE TABLE list_member (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  list_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role VARCHAR(6) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  FOREIGN KEY (list_id) REFERENCES todo_list(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
  UNIQUE (list_id, user_id)
);

-- 创建 invite_token 表
CREATE TABLE invite_token (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  list_id BIGINT NOT NULL,
  token VARCHAR(12) UNIQUE NOT NULL,
  created_at TIMESTAMP NOT NULL,
  FOREIGN KEY (list_id) REFERENCES todo_list(id) ON DELETE CASCADE
);

-- 扩展 todo_item 表
ALTER TABLE todo_item
ADD COLUMN created_by_id BIGINT DEFAULT NULL;

ALTER TABLE todo_item
ADD COLUMN updated_by_id BIGINT DEFAULT NULL;

ALTER TABLE todo_item
ADD FOREIGN KEY (created_by_id) REFERENCES "user"(id) ON DELETE SET NULL;

ALTER TABLE todo_item
ADD FOREIGN KEY (updated_by_id) REFERENCES "user"(id) ON DELETE SET NULL;
