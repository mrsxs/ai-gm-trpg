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

-- ============ aigm_scenario 种子：迷雾古宅（扩展版 · 22 节点开放探索 · 显式指认收尾） ============
-- 真凶=家庭医生格雷(慢性投毒侵吞遗产，当晚用手术刀灭口)。红鲱鱼:侄女(遗产动机)、园丁(当晚在地窖徘徊)。
-- 证据以 flag 计数(找过不重复加，天然防刷分循环)。结局只认对格雷的"显式指认"，理智归零才 LOSE。
USE aigm_scenario;

-- 重建剧本1的静态定义（清旧→插新；幂等可重跑）
DELETE FROM t_transition WHERE scenario_id = 1;
DELETE FROM t_node_npc   WHERE node_id IN (SELECT id FROM t_scene_node WHERE scenario_id = 1);
DELETE FROM t_flag_def   WHERE scenario_id = 1;
DELETE FROM t_npc        WHERE scenario_id = 1;
DELETE FROM t_scene_node WHERE scenario_id = 1;

INSERT INTO t_scenario (id, title, intro, genre, start_node_id, status, author_id) VALUES
  (1, '迷雾古宅', '雨夜，你受律师之邀来到范德姆庄园——庄园主埃德蒙暴毙于书房。表面是心脏病发，但宅中人人有所隐瞒。自由探索这座双层宅邸，收集物证、盘问众人，揪出潜藏的真凶。', '悬疑', 3001, 1, 1002)
  ON DUPLICATE KEY UPDATE title=VALUES(title), intro=VALUES(intro), genre=VALUES(genre), start_node_id=VALUES(start_node_id), status=VALUES(status), author_id=VALUES(author_id);

-- ---------- NPC（6 人格） ----------
INSERT INTO t_npc (id, scenario_id, npc_key, name, persona, background, secret) VALUES
  (2001, 1, 'npc_butler',   '管家·霍金斯',   '年迈、恭敬而克制；满口敬语、滴水不漏、爱回避正面追问；忠于旧主、守宅规如命。被反复追问或出示证据时会松动，暗示而非明说。', '在范德姆庄园服侍三十年，熟知每一条走廊与暗道。', '当晚听见书房争执，也知道书房通地窖的暗道，但怕牵连不敢主动说。'),
  (2002, 1, 'npc_maid',     '女仆·莉莉',     '年轻、胆小、善良；说话断续、欲言又止、容易受惊；被善待会逐渐交心。', '入府不久的女仆，胆小却心细，撞见了不该看的事。', '亲眼看到格雷医生深夜从主人卧室出来、神色慌张；信任到位会交出书房钥匙并作证。'),
  (2003, 1, 'npc_doctor',   '家庭医生·格雷', '表面温文尔雅、谈吐缜密、滴水不漏；实为真凶。被逼问时强词夺理、转移话题、反咬他人(常把矛头引向负债的侄女)；唯有铁证当面才会失态。', '埃德蒙的私人医生，长期出入宅邸、掌管其用药。', '多年以伪造诊疗、慢性投毒侵吞埃德蒙财产；事发当晚败露，用随身手术刀灭口并伪装成猝死。'),
  (2004, 1, 'npc_niece',    '侄女·伊莎贝拉', '高傲、戒备、情绪化；因负债而尖锐，对盘问极不耐烦；表面冷漠实则悲伤。', '埃德蒙唯一的血亲与法定继承人，常年在外、负债累累。', '当晚确与叔叔激烈争吵借钱被拒——有动机却并非凶手；她的争吵是误导玩家的红鲱鱼。'),
  (2005, 1, 'npc_gardener', '园丁·老汤姆',   '沉默寡言、警惕、答非所问；只在被理解时吐露片语。', '在庄园做了二十年的哑忍园丁，住温室旁。', '当晚见到有人深夜出入地窖、丢下东西；他怕惹祸只把所见模糊带过——看似可疑实则无辜的红鲱鱼。'),
  (2006, 1, 'npc_lawyer',   '律师·芬奇',     '干练、理性、就事论事；信息充分但谨慎，按程序办事。', '埃德蒙的遗嘱执行人，正是他写信请玩家来查清死因。', '掌握遗嘱副本：近月遗嘱被改、格雷被悄悄添为受益人；他还知道阁楼保险箱的密码。');

-- ---------- 场景节点（22） ----------
INSERT INTO t_scene_node (id, scenario_id, node_key, title, narrative_brief, is_ending, ending_type, sort_no) VALUES
  (3001, 1, 'node_arrival',   '雨夜门廊',   '玩家雨夜抵达范德姆庄园，管家霍金斯开门相迎。氛围:阴冷、压抑、礼数周到却疏离。目标:交代背景(主人暴毙、玩家受律师之邀来查)，把玩家引入大厅。绝不剧透凶手。', 0, NULL, 1),
  (3002, 1, 'node_hall',      '幽暗大厅',   '一楼枢纽:水晶吊灯蒙尘、环廊与楼梯交汇。可通往:餐厅/厨房/仆人区/花园/会客厅/画廊长廊，上楼去二楼回廊，(有钥匙)进书房，(知暗道)下地窖。提示玩家可自由走动。若玩家在此打转,管家可点出尚未探访的去处。', 0, NULL, 2),
  (3003, 1, 'node_dining',    '冷清餐厅',   '长餐桌残留未撤的晚宴。侄女伊莎贝拉在此借酒消愁。她当晚与叔叔争吵借钱被拒——动机明显(红鲱鱼)。盘问得当可得知争吵细节,但她并非真凶。不要把她坐实为凶手。', 0, NULL, 3),
  (3004, 1, 'node_kitchen',   '幽暗厨房',   '炉火将熄,女仆莉莉在此惊惶忙碌。她是关键证人。玩家友善相待、表明来意时 setFlags:[maid_trust];一旦已信任,她会交出书房钥匙并作证看到格雷医生深夜从主人卧室慌张走出——此时务必 setFlags:[has_key,witness_maid]、addItems:[书房钥匙]。未取得信任前只会害怕地含糊其辞。', 0, NULL, 4),
  (3005, 1, 'node_servant',   '仆人区',     '狭窄潮湿的下人房,莉莉的栖身处。与厨房一样可推进莉莉信任线:友善对待→setFlags:[maid_trust];已信任→她交出书房钥匙并作证看到格雷深夜出主人房→setFlags:[has_key,witness_maid]、addItems:[书房钥匙]。', 0, NULL, 5),
  (3006, 1, 'node_study',     '书房密室',   '命案现场,需 has_key 进入。书桌抽屉暗格藏着埃德蒙的褪色日记:写他"近来日渐虚弱、疑心药石有异"。玩家搜出并读日记时 setFlags:[found_diary]、addItems:[褪色日记]。墙边书架后有通地窖的暗道机关,玩家拨弄发现时 setFlags:[found_passage]。氛围:尘封、血腥气未散。', 0, NULL, 6),
  (3007, 1, 'node_garden',    '雨夜花园',   '冷雨中的荒芜花园,泥泞、藤蔓疯长。通往温室。可远眺宅邸轮廓,渲染氛围、给方向感(防止玩家迷路)。', 0, NULL, 7),
  (3008, 1, 'node_greenhouse','破败温室',   '园丁老汤姆的领地,潮热、霉味。被理解后他会含糊吐露:当晚见有人深夜出入地窖、丢下东西(set witness_gardener)。他看似可疑实则无辜(红鲱鱼)。', 0, NULL, 8),
  (3009, 1, 'node_parlor',    '会客厅',     '会客厅,壁炉与酒柜。律师芬奇在此整理文书,他正是请玩家来查案的人。他交代背景:遗嘱近月被改、格雷被悄悄添为受益人;并会告知阁楼保险箱密码——玩家问及时 setFlags:[safe_code]。是把矛头引向格雷的关键一环。', 0, NULL, 9),
  (3010, 1, 'node_landing',   '二楼回廊',   '二楼枢纽:阴影里挂满家族肖像。可通往:主人卧室/客房/(有密码)阁楼/家族教堂,下楼回大厅。若玩家打转,可用肖像、风声渲染并点出未探之处。', 0, NULL, 10),
  (3011, 1, 'node_master',    '主人卧室',   '埃德蒙的卧室,床畔暗褐血迹、药瓶散落。惊悚场景:attrDelta sanity-15。细查墙板后有机关,印证书房暗道,玩家发现时可 setFlags:[found_passage];散落药瓶标签与格雷有关,暗示其投毒(指向客房的诊疗记录)。', 0, NULL, 11),
  (3012, 1, 'node_guest',     '客房',       '格雷医生借住的客房。床头黑色药箱里藏着伪造的诊疗记录与多出的药剂——足证他长期错误用药。玩家翻查药箱发现时务必 setFlags:[found_records]、addItems:[伪造诊疗记录]。格雷可能在场,谈吐镇定、回避要害。', 0, NULL, 12),
  (3013, 1, 'node_attic',     '尘封阁楼',   '需 safe_code 开启角落旧保险箱。惊悚、积尘:attrDelta sanity-10。保险箱内是被篡改的遗嘱原件:格雷被添入受益人(动机铁证)。玩家开箱取出时 setFlags:[found_will]、addItems:[被篡改的遗嘱]。', 0, NULL, 13),
  (3014, 1, 'node_cellar_entry','地窖入口', '需 found_passage(经书房暗道或主卧机关得知)才能找到并下行。湿冷石阶、铁门虚掩。霉烂酒桶与煤堆之间的暗格藏着沾血手术刀(凶器)与丢弃的沾血手套——最直接物证;玩家翻找/搜查时务必 setFlags:[found_weapon,found_glove]、addItems:[手术刀]。惊悚:attrDelta sanity-10。可再深入或返回大厅。', 0, NULL, 14),
  (3015, 1, 'node_cellar',    '阴森地窖',   '阴森地窖,霉烂酒桶与煤堆。惊悚场景:attrDelta sanity-10。煤堆暗格里藏着沾血的细长手术刀(凶器)与一只丢弃的沾血手套。玩家翻找发现时务必 setFlags:[found_weapon,found_glove]、addItems:[手术刀]。这是最直接的物证。', 0, NULL, 15),
  (3016, 1, 'node_passage',   '暗道密室',   '连接书房与地窖的狭窄暗道,墙上有旧药剂架。氛围:幽闭、隐秘。暗示凶手当晚由此往返、转移凶器,串起书房与地窖的线索。', 0, NULL, 16),
  (3017, 1, 'node_chapel',    '家族教堂',   '宅侧的小教堂,残烛、家族墓碑。惊悚、肃穆(sanity-10)。墓志与旧照暗示范德姆家曾因"庸医"出过人命,为格雷的恶行埋伏笔。', 0, NULL, 17),
  (3018, 1, 'node_whisper',   '低语回廊',   '仅当玩家 sanity 较低(<=40)时浮现的恍惚幻境/低语。神秘破碎、似真似幻,以隐晦低语把玩家引回关键线索(书房/地窖/客房),起到防迷路的兜底提示作用。不直接报出凶手。', 0, NULL, 18),
  (3019, 1, 'node_confront',  '对峙厅',     '收束节点:玩家召集众人于大厅对质。关键规则:必须等玩家"明确指认某一个人"才推进,绝不替玩家下结论、绝不自行结束。①玩家明确指认格雷且已掌握凶器(found_weapon)与伪造病历(found_records)或被改遗嘱(found_will)→set accused_doctor、提议跳 node_win。②指认他人/证据不足/只是泛泛发问→格雷强词夺理反咬、其他人各执一词,停留在本节点并提示玩家继续举证或锁定对象,不结束。③理智为0→提议 node_lose。', 0, NULL, 19),
  (3020, 1, 'node_win',       '真相大白',   '铁证当前,格雷心理防线崩溃、当众露馅,被指认归案,埃德蒙沉冤得雪。结局 WIN。给出有收束感的尾声。', 1, 'WIN',  20),
  (3021, 1, 'node_lose',      '坠入疯狂',   '玩家精神崩溃(sanity 归零),古宅的阴影吞没理智,真相随之沉没。结局 LOSE。', 1, 'LOSE', 21),
  (3022, 1, 'node_gallery',   '画廊长廊',   '悬挂历代范德姆肖像的长廊,连接大厅各处。一幅埃德蒙近照面色蜡黄、形容枯槁,暗示久病/中毒(呼应 found_records/日记)。亦作枢纽分流,帮助玩家辨明方向。', 0, NULL, 22);

UPDATE t_scenario SET start_node_id = 3001 WHERE id = 1;

-- ---------- 出场 NPC ----------
INSERT INTO t_node_npc (node_id, npc_id) VALUES
  (3001, 2001),
  (3002, 2001),
  (3003, 2004),
  (3004, 2002),
  (3005, 2002),
  (3007, 2005),
  (3008, 2005),
  (3009, 2006),
  (3012, 2003),
  (3019, 2001), (3019, 2002), (3019, 2003), (3019, 2004), (3019, 2005), (3019, 2006);

-- ---------- 状态机白名单（开放枢纽 + flag 门禁 + 显式指认收尾） ----------
-- 证据门禁全用 flag(布尔幂等),不用数值,避免来回刷同一线索把进度刷爆。
INSERT INTO t_transition (scenario_id, from_node_id, to_node_id, condition_expr, description, priority) VALUES
  -- 开场
  (1, 3001, 3002, 'always', '走进大厅', 10),
  -- 一楼枢纽(大厅)放射 + 回流
  (1, 3002, 3003, 'always', '去餐厅', 10),
  (1, 3002, 3004, 'always', '去厨房', 10),
  (1, 3002, 3005, 'always', '去仆人区', 9),
  (1, 3002, 3007, 'always', '去花园', 9),
  (1, 3002, 3009, 'always', '去会客厅找律师', 10),
  (1, 3002, 3022, 'always', '走进画廊长廊', 8),
  (1, 3002, 3010, 'always', '上二楼', 10),
  (1, 3002, 3006, 'flag.has_key==true',       '用钥匙进书房', 20),
  (1, 3002, 3014, 'flag.found_passage==true', '循暗道下地窖', 18),
  (1, 3003, 3002, 'always', '回大厅', 5),
  (1, 3004, 3002, 'always', '回大厅', 5),
  (1, 3004, 3005, 'always', '进里间仆人区', 8),
  (1, 3005, 3004, 'always', '回厨房', 5),
  (1, 3005, 3002, 'always', '回大厅', 5),
  (1, 3007, 3002, 'always', '回大厅', 5),
  (1, 3007, 3008, 'always', '进温室', 9),
  (1, 3008, 3007, 'always', '回花园', 5),
  (1, 3009, 3002, 'always', '回大厅', 5),
  (1, 3022, 3002, 'always', '回大厅', 5),
  (1, 3022, 3010, 'always', '由长廊上楼', 6),
  -- 书房:日记 + 暗道；与地窖相通
  (1, 3006, 3002, 'always', '离开书房回大厅', 6),
  (1, 3006, 3014, 'flag.found_passage==true', '由书房暗道下地窖', 14),
  (1, 3006, 3016, 'flag.found_passage==true', '钻进墙后暗道', 12),
  -- 二楼枢纽(回廊)放射 + 回流
  (1, 3010, 3002, 'always', '下楼回大厅', 8),
  (1, 3010, 3011, 'always', '进主人卧室', 10),
  (1, 3010, 3012, 'always', '进客房', 10),
  (1, 3010, 3017, 'always', '去家族教堂', 9),
  (1, 3010, 3013, 'flag.safe_code==true', '凭密码进阁楼开保险箱', 16),
  (1, 3011, 3010, 'always', '回二楼回廊', 5),
  (1, 3012, 3010, 'always', '回二楼回廊', 5),
  (1, 3013, 3010, 'always', '回二楼回廊', 5),
  (1, 3017, 3010, 'always', '回二楼回廊', 5),
  -- 地窖/暗道
  (1, 3014, 3015, 'always', '深入地窖', 10),
  (1, 3014, 3016, 'always', '走进暗道密室', 9),
  (1, 3014, 3002, 'always', '拾级返回大厅', 7),
  (1, 3015, 3002, 'always', '离开地窖回大厅', 6),
  (1, 3015, 3014, 'always', '退回地窖入口', 5),
  (1, 3015, 3016, 'always', '穿过暗道', 8),
  (1, 3016, 3006, 'always', '由暗道通回书房', 8),
  (1, 3016, 3015, 'always', '回到地窖', 6),
  -- 低语回廊(理智低时的兜底提示),探完回最近枢纽
  (1, 3011, 3018, 'attr.sanity<=40', '恍惚间步入低语回廊', 7),
  (1, 3013, 3018, 'attr.sanity<=40', '恍惚间步入低语回廊', 7),
  (1, 3017, 3018, 'attr.sanity<=40', '恍惚间步入低语回廊', 7),
  (1, 3018, 3010, 'always', '回过神,返回二楼回廊', 6),
  -- 召集对峙:掌握凶器 + (伪造病历 或 被改遗嘱)即可,从两个枢纽都能发起
  (1, 3002, 3019, 'flag.found_weapon==true && flag.found_records==true', '证据已足,召集众人对峙', 22),
  (1, 3002, 3019, 'flag.found_weapon==true && flag.found_will==true',    '证据已足,召集众人对峙', 21),
  (1, 3010, 3019, 'flag.found_weapon==true && flag.found_records==true', '证据已足,召集众人对峙', 22),
  (1, 3010, 3019, 'flag.found_weapon==true && flag.found_will==true',    '证据已足,召集众人对峙', 21),
  -- 对峙厅:仅"显式指认格雷 + 铁证"才放行;指错/不足则停留(无边可走);理智崩溃则 LOSE
  (1, 3019, 3020, 'flag.found_weapon==true && flag.found_records==true && flag.accused_doctor==true', '铁证指认格雷,真相大白', 30),
  (1, 3019, 3020, 'flag.found_weapon==true && flag.found_will==true && flag.accused_doctor==true',    '铁证指认格雷,真相大白', 29),
  (1, 3019, 3002, 'always', '暂退一步,回大厅再查', 4),
  -- 理智归零 → 坠入疯狂(LOSE),从惊悚场景与对峙厅兜底
  (1, 3011, 3021, 'attr.sanity<=0', '精神崩溃', 40),
  (1, 3013, 3021, 'attr.sanity<=0', '精神崩溃', 40),
  (1, 3015, 3021, 'attr.sanity<=0', '精神崩溃', 40),
  (1, 3017, 3021, 'attr.sanity<=0', '精神崩溃', 40),
  (1, 3019, 3021, 'attr.sanity<=0', '精神崩溃', 40);

-- ---------- 旗标定义 ----------
INSERT INTO t_flag_def (scenario_id, flag_key, flag_name, default_value) VALUES
  (1, 'maid_trust',       '女仆莉莉已信任玩家',     'false'),
  (1, 'has_key',          '已获得书房钥匙',         'false'),
  (1, 'found_passage',    '已发现通地窖的暗道',     'false'),
  (1, 'safe_code',        '已获知阁楼保险箱密码',   'false'),
  (1, 'found_diary',      '【证据】埃德蒙的日记',   'false'),
  (1, 'found_records',    '【证据】格雷伪造的诊疗记录', 'false'),
  (1, 'found_will',       '【证据】被篡改的遗嘱',   'false'),
  (1, 'found_weapon',     '【证据】沾血的手术刀(凶器)', 'false'),
  (1, 'found_glove',      '【证据】丢弃的沾血手套', 'false'),
  (1, 'witness_maid',     '【证词】莉莉目击格雷深夜出主人房', 'false'),
  (1, 'witness_gardener', '【证词】老汤姆目击有人深夜出入地窖', 'false'),
  (1, 'accused_doctor',   '玩家已当面指认格雷',     'false');
