-- aigm_scenario 建库建表（基线 §4.2）
CREATE DATABASE IF NOT EXISTS aigm_scenario DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE aigm_scenario;

CREATE TABLE IF NOT EXISTS t_scenario (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '剧本ID',
  title          VARCHAR(100) NOT NULL               COMMENT '剧本标题',
  intro          VARCHAR(500) DEFAULT NULL            COMMENT '简介',
  cover          VARCHAR(255) DEFAULT NULL            COMMENT '封面URL',
  genre          VARCHAR(30)  DEFAULT NULL            COMMENT '题材:悬疑/奇幻等',
  start_node_id  BIGINT       DEFAULT NULL            COMMENT '起始场景节点ID(指向t_scene_node)',
  status         TINYINT      NOT NULL DEFAULT 0      COMMENT '状态:0草稿 1已发布 2下架',
  author_id      BIGINT       NOT NULL               COMMENT '作者(编剧)用户ID',
  deleted        TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_author (author_id),
  KEY idx_status (status)
) ENGINE=InnoDB COMMENT='剧本表';

CREATE TABLE IF NOT EXISTS t_scene_node (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '节点ID',
  scenario_id     BIGINT       NOT NULL               COMMENT '所属剧本ID',
  node_key        VARCHAR(50)  NOT NULL               COMMENT '节点业务key(剧本内唯一,如node_intro)',
  title           VARCHAR(100) NOT NULL               COMMENT '节点标题',
  narrative_brief TEXT         NOT NULL               COMMENT '叙事提示要点(喂给LLM的剧情纲要/氛围/目标)',
  is_ending       TINYINT      NOT NULL DEFAULT 0     COMMENT '是否结局节点:0否 1是',
  ending_type     VARCHAR(20)  DEFAULT NULL            COMMENT '结局类型:WIN/LOSE/NEUTRAL',
  sort_no         INT          NOT NULL DEFAULT 0     COMMENT '排序',
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_scenario_nodekey (scenario_id, node_key),
  KEY idx_scenario (scenario_id),
  CONSTRAINT fk_node_scenario FOREIGN KEY (scenario_id) REFERENCES t_scenario(id)
) ENGINE=InnoDB COMMENT='场景节点表';

CREATE TABLE IF NOT EXISTS t_npc (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'NPC ID',
  scenario_id   BIGINT       NOT NULL               COMMENT '所属剧本ID',
  npc_key       VARCHAR(50)  NOT NULL               COMMENT 'NPC业务key(剧本内唯一,如npc_butler)',
  name          VARCHAR(50)  NOT NULL               COMMENT 'NPC名称',
  persona       TEXT         NOT NULL               COMMENT '人格设定(性格/说话风格/动机,保证人格一致)',
  background    TEXT         DEFAULT NULL            COMMENT '背景故事',
  secret        TEXT         DEFAULT NULL            COMMENT '隐藏秘密(可被剧情解锁)',
  avatar        VARCHAR(255) DEFAULT NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_scenario_npckey (scenario_id, npc_key),
  KEY idx_scenario (scenario_id),
  CONSTRAINT fk_npc_scenario FOREIGN KEY (scenario_id) REFERENCES t_scenario(id)
) ENGINE=InnoDB COMMENT='NPC人设表';

CREATE TABLE IF NOT EXISTS t_node_npc (
  id        BIGINT NOT NULL AUTO_INCREMENT,
  node_id   BIGINT NOT NULL COMMENT '节点ID',
  npc_id    BIGINT NOT NULL COMMENT 'NPC ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_node_npc (node_id, npc_id),
  KEY idx_node (node_id),
  CONSTRAINT fk_nn_node FOREIGN KEY (node_id) REFERENCES t_scene_node(id),
  CONSTRAINT fk_nn_npc  FOREIGN KEY (npc_id)  REFERENCES t_npc(id)
) ENGINE=InnoDB COMMENT='节点NPC关联表';

CREATE TABLE IF NOT EXISTS t_transition (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分支ID',
  scenario_id   BIGINT       NOT NULL               COMMENT '所属剧本ID(冗余,便于查询)',
  from_node_id  BIGINT       NOT NULL               COMMENT '源节点ID',
  to_node_id    BIGINT       NOT NULL               COMMENT '目标节点ID',
  condition_expr VARCHAR(255) DEFAULT NULL           COMMENT '触发条件表达式(如 flag.has_key==true)',
  description   VARCHAR(255) DEFAULT NULL            COMMENT '分支说明(给AI/编剧看)',
  priority      INT          NOT NULL DEFAULT 0     COMMENT '优先级(数值大优先匹配)',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_from (from_node_id),
  KEY idx_scenario (scenario_id),
  CONSTRAINT fk_tr_from FOREIGN KEY (from_node_id) REFERENCES t_scene_node(id),
  CONSTRAINT fk_tr_to   FOREIGN KEY (to_node_id)   REFERENCES t_scene_node(id)
) ENGINE=InnoDB COMMENT='分支(状态转移边)表';

CREATE TABLE IF NOT EXISTS t_flag_def (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '旗标定义ID',
  scenario_id   BIGINT       NOT NULL               COMMENT '所属剧本ID',
  flag_key      VARCHAR(50)  NOT NULL               COMMENT '旗标key(如 has_key)',
  flag_name     VARCHAR(100) DEFAULT NULL            COMMENT '旗标含义',
  default_value VARCHAR(20)  DEFAULT 'false'        COMMENT '默认值',
  PRIMARY KEY (id),
  UNIQUE KEY uk_scenario_flag (scenario_id, flag_key),
  KEY idx_scenario (scenario_id),
  CONSTRAINT fk_flag_scenario FOREIGN KEY (scenario_id) REFERENCES t_scenario(id)
) ENGINE=InnoDB COMMENT='旗标定义表';
