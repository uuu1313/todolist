-- 创建 todo_list 表
CREATE TABLE todo_list (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  token VARCHAR(8) NOT NULL UNIQUE,
  title VARCHAR(100) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

-- 创建 todo_item 表
CREATE TABLE todo_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  list_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  priority VARCHAR(6) NOT NULL,
  due_date DATE,
  completed BOOLEAN NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  FOREIGN KEY (list_id) REFERENCES todo_list(id) ON DELETE CASCADE
);
