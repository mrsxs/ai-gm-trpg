-- ============ aigm_user 种子（基线 §08-devops 4.2） ============
USE aigm_user;
-- 密文均为明文 "123456" 的真实 BCrypt(strength=10,$2a$) 结果，可直接登录
INSERT IGNORE INTO t_user (id, username, password, nickname, status) VALUES
  (1001, 'admin',  '$2a$10$BfwTeu7NDRKwHDEY6rwdqewoqWGBszYh7adbXW6mnAAqHhcFb6kja', '系统管理员', 1),
  (1002, 'author', '$2a$10$BfwTeu7NDRKwHDEY6rwdqewoqWGBszYh7adbXW6mnAAqHhcFb6kja', '编剧老王',   1),
  (1003, 'player', '$2a$10$BfwTeu7NDRKwHDEY6rwdqewoqWGBszYh7adbXW6mnAAqHhcFb6kja', '玩家小李',   1);

INSERT IGNORE INTO t_user_role (user_id, role_id) SELECT 1001, id FROM t_role WHERE role_code = 'ADMIN';
INSERT IGNORE INTO t_user_role (user_id, role_id) SELECT 1002, id FROM t_role WHERE role_code = 'AUTHOR';
INSERT IGNORE INTO t_user_role (user_id, role_id) SELECT 1003, id FROM t_role WHERE role_code = 'PLAYER';

-- ============ aigm_scenario 种子：迷雾古宅（基线 §7.7） ============
USE aigm_scenario;

INSERT IGNORE INTO t_scenario (id, title, intro, genre, start_node_id, status, author_id) VALUES
  (1, '迷雾古宅', '雨夜受邀来到离奇宅邸，主人暴毙，你要在与众人周旋中找出真凶。', '悬疑', NULL, 1, 1002);

INSERT IGNORE INTO t_scene_node (id, scenario_id, node_key, title, narrative_brief, is_ending, ending_type, sort_no) VALUES
  (3001, 1, 'node_intro',    '雨夜抵达',   '玩家雨夜抵达古宅，管家开门。氛围:阴森、压抑。目标:交代背景并把玩家引入大厅。不要剧透凶手。', 0, NULL, 1),
  (3002, 1, 'node_hall',     '幽暗的大厅', '布满灰尘的大厅，中枢探索点。氛围:阴森。目标:让玩家选择去仆人区/上楼/(有钥匙时)进书房。线索:墙上挂画后有暗格。', 0, NULL, 2),
  (3003, 1, 'node_servant',  '仆人区',     '女仆莉莉在此。目标:玩家可通过对话取得女仆信任，获得书房钥匙(set has_key)。线索:女仆知道当晚部分真相但害怕。', 0, NULL, 3),
  (3004, 1, 'node_study',    '书房密室',   '需钥匙进入。关键线索:抽屉暗格里的褪色日记(found_diary, evidence+1)。氛围:尘封、隐秘。', 0, NULL, 4),
  (3005, 1, 'node_upstairs', '二楼卧室',   '主人卧室，发现沾血手术刀(found_weapon, evidence+1)与血迹。高 sanity 考验。', 0, NULL, 5),
  (3006, 1, 'node_confront', '对峙真凶',   '收束节点。依据已掌握证据数判定:evidence>=2 可成功指认家庭医生格雷。', 0, NULL, 6),
  (3007, 1, 'node_win',      '真相大白',   '证据确凿，成功指认真凶 npc_doctor，沉冤得雪。结局 WIN。', 1, 'WIN',  7),
  (3008, 1, 'node_lose',     '误判/逃脱',  '证据不足或精神崩溃，真凶逍遥法外。结局 LOSE。', 1, 'LOSE', 8);

UPDATE t_scenario SET start_node_id = 3001 WHERE id = 1;

INSERT IGNORE INTO t_npc (id, scenario_id, npc_key, name, persona, background, secret) VALUES
  (2001, 1, 'npc_butler', '管家·霍金斯', '年迈、表面恭敬实则警惕；说话用敬语、爱回避正面问题；语速慢、滴水不漏。', '在古宅服侍三十年，熟悉宅中一切。', '当晚听到书房有争执但隐瞒，怕被牵连。'),
  (2002, 1, 'npc_maid',   '女仆·莉莉',   '胆小、善良、紧张；说话断续、欲言又止；被威胁不敢说真话。',           '年轻女仆，目睹了一些关键细节。',     '看到医生深夜进出主人房间；信任足够会交出书房钥匙。'),
  (2003, 1, 'npc_doctor', '家庭医生·格雷', '表面温和理性、谈吐得体；实为真凶；被逼问时强词夺理、转移话题。',     '主人的私人医生，常出入宅邸。',       '为侵吞遗产用药物谋害主人，手术刀是凶器。'),
  (2004, 1, 'npc_ghost',  '低语的幻影',   '神秘、破碎、似真似幻；只在玩家 sanity 低时出现，给隐晦暗示。',       '宅中传说的幽灵，亦可能是幻觉。',     '低语指向二楼与书房的线索。');

INSERT IGNORE INTO t_node_npc (node_id, npc_id) VALUES
  (3001, 2001),
  (3002, 2001),
  (3003, 2002),
  (3005, 2004),
  (3006, 2001), (3006, 2002), (3006, 2003);

INSERT IGNORE INTO t_transition (scenario_id, from_node_id, to_node_id, condition_expr, description, priority) VALUES
  (1, 3001, 3002, 'always',            '走进大厅',           10),
  (1, 3002, 3003, 'always',            '去仆人区找女仆',     10),
  (1, 3002, 3005, 'always',            '上楼',               8),
  (1, 3002, 3004, 'flag.has_key==true','用钥匙进书房',       20),
  (1, 3003, 3002, 'always',            '返回大厅',           5),
  (1, 3003, 3004, 'flag.has_key==true','拿到钥匙后直接去书房',15),
  (1, 3004, 3005, 'always',            '离开书房上楼',       8),
  (1, 3004, 3006, 'attr.evidence>=1',  '掌握线索后去对峙',   12),
  (1, 3005, 3006, 'always',            '下楼对峙',           10),
  (1, 3006, 3007, 'attr.evidence>=2',  '证据充分，指认真凶', 20),
  (1, 3006, 3008, 'attr.evidence<2',   '证据不足，误判收场', 10);

INSERT IGNORE INTO t_flag_def (scenario_id, flag_key, flag_name, default_value) VALUES
  (1, 'has_key',        '已获得书房钥匙',   'false'),
  (1, 'maid_trusts',    '女仆已信任玩家',   'false'),
  (1, 'found_diary',    '已找到书房日记',   'false'),
  (1, 'found_weapon',   '已找到二楼凶器',   'false'),
  (1, 'talked_to_maid', '已与女仆交谈',     'false');
