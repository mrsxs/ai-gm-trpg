-- aigm_game 建库建表（基线 §4.3）
CREATE DATABASE IF NOT EXISTS aigm_game DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE aigm_game;

CREATE TABLE IF NOT EXISTS t_game_session (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '对局ID',
  user_id       BIGINT       NOT NULL               COMMENT '玩家用户ID',
  scenario_id   BIGINT       NOT NULL               COMMENT '剧本ID',
  title         VARCHAR(100) DEFAULT NULL            COMMENT '存档名(默认=剧本名+时间)',
  status        TINYINT      NOT NULL DEFAULT 1      COMMENT '状态:1进行中 2已通关 3已失败 4已弃局',
  turn_count    INT          NOT NULL DEFAULT 0      COMMENT '已进行回合数',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开局时间',
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近游玩时间',
  PRIMARY KEY (id),
  KEY idx_user (user_id),
  KEY idx_user_status (user_id, status)
) ENGINE=InnoDB COMMENT='对局/存档表';

CREATE TABLE IF NOT EXISTS t_game_state (
  id               BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  session_id       BIGINT   NOT NULL               COMMENT '对局ID',
  current_node_id  BIGINT   NOT NULL               COMMENT '当前所处场景节点ID',
  flags            JSON     DEFAULT NULL            COMMENT '旗标 {"has_key":true,...}',
  inventory        JSON     DEFAULT NULL            COMMENT '物品 ["生锈的钥匙","日记本"]',
  attributes       JSON     DEFAULT NULL            COMMENT '属性 {"sanity":80,"trust":3}',
  recent_summary   TEXT     DEFAULT NULL            COMMENT '近况摘要(滚动压缩的剧情回顾,喂LLM)',
  updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_session (session_id),
  CONSTRAINT fk_state_session FOREIGN KEY (session_id) REFERENCES t_game_session(id)
) ENGINE=InnoDB COMMENT='对局状态表';

CREATE TABLE IF NOT EXISTS t_turn (
  id            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '回合ID',
  session_id    BIGINT   NOT NULL               COMMENT '对局ID',
  turn_no       INT      NOT NULL               COMMENT '回合序号(从1递增)',
  node_id       BIGINT   NOT NULL               COMMENT '该回合发生时所在节点ID',
  player_input  TEXT     DEFAULT NULL            COMMENT '玩家输入(首回合可为空)',
  ai_output     JSON     NOT NULL               COMMENT 'AI结构化输出(见AI引擎schema)',
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_session_turn (session_id, turn_no),
  KEY idx_session (session_id),
  CONSTRAINT fk_turn_session FOREIGN KEY (session_id) REFERENCES t_game_session(id)
) ENGINE=InnoDB COMMENT='回合表';
