-- aigm_user 建库建表（基线 §4.1），含 t_role 初始三角色
CREATE DATABASE IF NOT EXISTS aigm_user DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE aigm_user;

CREATE TABLE IF NOT EXISTS t_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username      VARCHAR(50)  NOT NULL              COMMENT '用户名(登录账号)',
  password      VARCHAR(100) NOT NULL              COMMENT 'BCrypt 加密后的密码',
  nickname      VARCHAR(50)  DEFAULT NULL          COMMENT '昵称',
  avatar        VARCHAR(255) DEFAULT NULL          COMMENT '头像URL',
  status        TINYINT      NOT NULL DEFAULT 1    COMMENT '状态:1正常 0禁用',
  deleted       TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除:0未删 1已删',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB COMMENT='用户表';

CREATE TABLE IF NOT EXISTS t_role (
  id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  role_code     VARCHAR(20) NOT NULL               COMMENT '角色编码:PLAYER/AUTHOR/ADMIN',
  role_name     VARCHAR(50) NOT NULL               COMMENT '角色名称',
  created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB COMMENT='角色表';

CREATE TABLE IF NOT EXISTS t_user_role (
  id        BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id   BIGINT NOT NULL               COMMENT '用户ID',
  role_id   BIGINT NOT NULL               COMMENT '角色ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_role (user_id, role_id),
  KEY idx_user_id (user_id),
  KEY idx_role_id (role_id),
  CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES t_user(id),
  CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES t_role(id)
) ENGINE=InnoDB COMMENT='用户角色关联表';

INSERT IGNORE INTO t_role (role_code, role_name) VALUES
  ('PLAYER','玩家'), ('AUTHOR','编剧'), ('ADMIN','管理员');
