-- 简单剧本「暮光咖啡馆·失踪的金戒指」scenario_id=3。独立种子，只影响 id=3，不触碰迷雾古宅(id=1)。
USE aigm_scenario;

-- 幂等重建：先清 scenario 3 自身的数据
DELETE FROM t_node_npc   WHERE node_id IN (SELECT id FROM t_scene_node WHERE scenario_id = 3);
DELETE FROM t_transition WHERE scenario_id = 3;
DELETE FROM t_flag_def   WHERE scenario_id = 3;
DELETE FROM t_scene_node WHERE scenario_id = 3;
DELETE FROM t_npc        WHERE scenario_id = 3;
DELETE FROM t_scenario   WHERE id = 3;

-- 剧本（status=1 已发布，玩家大厅可见；start_node=4001）
INSERT INTO t_scenario (id, title, intro, genre, start_node_id, status, author_id) VALUES
(3, '暮光咖啡馆·失踪的金戒指',
 '雨天午后，城市角落的「暮光咖啡馆」。一位熟客的传家金戒指落在桌上，转眼不翼而飞——此刻店里只剩两位客人。你是恰好在场的侦探，请查清是谁拿走了它。新手友好的短篇推理：找齐两条物证，当面指认真凶即可破案。',
 '推理', 4001, 1, 1002);

-- NPC（2101 老板娘 / 2102 小林=真凶 / 2103 周伯=红鲱鱼）
INSERT INTO t_npc (id, scenario_id, npc_key, name, persona, background, secret) VALUES
(2101, 3, 'npc_owner', '苏阿姨（老板娘）',
 '热心、焦急、絮叨；信任你这位侦探，急着找回客人的戒指。说话带市井暖意，常给你递热咖啡。',
 '经营暮光咖啡馆二十年，认得每位熟客。案发时她正在后厨煮豆，听见惊呼才出来。',
 '她其实瞥见小林续杯后神色慌张，但不敢妄断好人，只敢旁敲侧击地提醒你。'),
(2102, 3, 'npc_lin', '小林（大学生）',
 '腼腆、紧张、爱回避目光；被问到「续杯」「离座」时会语无伦次、岔开话题。表面是穷学生。',
 '常来蹭网写论文的熟面孔，今天点了美式，案发前曾起身去吧台续杯。',
 '真凶：他趁续杯路过熟客桌时顺走了金戒指塞进背包，把空戒指盒丢进了洗手间垃圾桶。不会主动承认，除非你拿出小票与戒指盒当面对质。'),
(2103, 3, 'npc_zhou', '周伯（退休教师）',
 '淡定、健谈、引经据典；乐意配合你，但对被怀疑有点不快。是个容易被误会的红鲱鱼。',
 '退休中学语文老师，每天来看报喝手冲。案发全程坐在靠窗卡座没挪过窝。',
 '清白：他只是把老花镜落在了熟客桌边，才显得可疑；他全程在看报，可为小林是否离座作证。');

-- 节点（4001 起点大厅 / 4002 吧台→found_receipt / 4003 靠窗·周伯 / 4004 洗手间→found_box / 4005 对质 / 4006 WIN）
INSERT INTO t_scene_node (id, scenario_id, node_key, title, narrative_brief, is_ending, ending_type, sort_no) VALUES
(4001, 3, 'node_hall', '咖啡馆大厅',
 '暮光咖啡馆大厅，窗外细雨淅沥，咖啡机嘶嘶作响。老板娘苏阿姨刚报案：熟客的传家金戒指落在桌上转眼就不见了；此刻店里只有腼腆的大学生小林、淡定看报的退休教师周伯。这是枢纽，引导玩家去【吧台】查小票、去【靠窗卡座】找周伯问话、去【洗手间过道】翻垃圾桶。不要剧透谁是真凶。', 0, NULL, 1),
(4002, 3, 'node_counter', '吧台',
 '吧台与点单小票/挂壁监控所在。当玩家查看小票、监控或询问老板娘案发经过时，可发现：案发前小林曾借口「续杯」离座约两分钟，时间点正对得上。setFlags:[found_receipt]。不要直接说他就是贼，只摆出这条客观线索。', 0, NULL, 2),
(4003, 3, 'node_window', '靠窗卡座',
 '周伯的座位，桌边搁着一副老花镜（红鲱鱼，易被误会但清白）。盘问周伯可得知：他全程在看报没离座，并能作证小林确实起身去过吧台。不要把周伯坐实为贼，他无辜。', 0, NULL, 3),
(4004, 3, 'node_restroom', '洗手间过道',
 '洗手间外的狭窄过道，角落一只垃圾桶。当玩家翻查垃圾桶时，发现一只空的金戒指绒盒，盒内还留着戒指压痕——是被人匆忙丢弃的。setFlags:[found_box]。', 0, NULL, 4),
(4005, 3, 'node_confront', '当面对质',
 '收束节点：玩家集齐【小票/续杯证词】与【洗手间的空戒指盒】后，召集小林与周伯当面对质。仅当玩家明确指名「小林」为偷戒指的人时，setFlags:[accused_thief] 并提议转入结局 node_win；若玩家指认周伯、或证据不足、或含糊其辞，就借 NPC 与证据如实反驳并停留，绝不替玩家下结论、绝不自行结束。', 0, NULL, 5),
(4006, 3, 'node_win', '水落石出', '结局·胜利：在小票与戒指盒的铁证下，小林承认趁续杯顺走了金戒指、把空盒丢进洗手间垃圾桶。戒指完璧归赵，苏阿姨千恩万谢。请收束全剧。', 1, 'WIN', 6);

-- 节点↔NPC 出场绑定
INSERT INTO t_node_npc (node_id, npc_id) VALUES
(4001, 2101), (4001, 2102), (4001, 2103),
(4002, 2101),
(4003, 2103),
(4005, 2101), (4005, 2102), (4005, 2103);

-- 旗标定义
INSERT INTO t_flag_def (scenario_id, flag_key, flag_name, default_value) VALUES
(3, 'found_receipt', '【证据】小票/证词证明小林离座续杯', 0),
(3, 'found_box',     '【证据】洗手间垃圾桶里的空戒指盒', 0),
(3, 'accused_thief', '已当面指认小林为真凶', 0);

-- 转移白名单（枢纽 4001 ⇄ 各线索点；集齐两证可入对质；指认小林通关）
INSERT INTO t_transition (scenario_id, from_node_id, to_node_id, condition_expr, description, priority) VALUES
(3, 4001, 4002, 'always', '去吧台查小票', 10),
(3, 4001, 4003, 'always', '去靠窗卡座找周伯', 10),
(3, 4001, 4004, 'always', '去洗手间过道', 10),
(3, 4002, 4001, 'always', '回大厅', 10),
(3, 4003, 4001, 'always', '回大厅', 10),
(3, 4004, 4001, 'always', '回大厅', 10),
(3, 4001, 4005, 'flag.found_receipt==true && flag.found_box==true', '证据齐备，召集二人当面对质', 20),
(3, 4002, 4005, 'flag.found_receipt==true && flag.found_box==true', '证据齐备，直接对质', 20),
(3, 4004, 4005, 'flag.found_receipt==true && flag.found_box==true', '证据齐备，直接对质', 20),
(3, 4005, 4001, 'always', '暂回大厅再查', 5),
(3, 4005, 4006, 'flag.found_receipt==true && flag.found_box==true && flag.accused_thief==true', '当众指认小林，水落石出', 30);
