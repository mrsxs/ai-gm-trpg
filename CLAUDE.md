# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 实现状态（2026-05-30）

**项目已完整实现并端到端跑通**：8 个 Maven 模块 + Vue3 前端均已落地，连接远端中间件（Nacos/MySQL/Redis/PG）可一键启动。登录 `player/123456` 开局「迷雾古宅」可连续回合通关，真实 LLM 叙事 + 语义记忆召回已端到端验证。

**已落地的三处设计偏差**（以实际代码为准，勿以文档为准）：

| # | 设计文档 | 实际实现 | 影响范围 |
|---|---|---|---|
| 1 | JWT（jjwt 0.12.x） | **sa-token 1.39**（Redis 共享会话，`token-name=Authorization`，`token-prefix=Bearer`） | gateway 鉴权、user-service 签发；下游 X-User-* 头契约不变 |
| 2 | pgvector `vector(1024)` | **`REAL[]` 数组 + 应用层余弦**（远端 PG 未装 pgvector）；装上后切回 `deploy/pg-init/01-schema-memory.sql` | memory-service 仅 |
| 3 | 文档未提兜底 | **确定性 stub**：无 LLM key → 关键词叙事；无 embedding key → 哈希伪向量；均可端到端跑通 | ai-engine, memory |

设计文档仍是 DDL/API 契约/错误码/AI Schema 的 SSOT，但上表三处以**代码为准**。

---

## 项目一句话

单人 AI 跑团「游戏主持人(GM)」。玩家自由文本输入 → AI 即兴叙事并分饰 NPC → 剧情跳转被编剧预设的**状态机白名单**死死兜住。三大 AI 难点：①状态机约束 LLM 防跑偏 ②多 NPC 人格一致 ③长程记忆 RAG。

---

## 设计文档导航（按行号直达）

两份文档为施工 SSOT，查阅时按行号跳转：

| 简称 | 文件 | 用途 |
|---|---|---|
| **DOC B** | `AI跑团GM·微服务毕设-实现规格书(AI可执行).md`（9746 行） | 施工蓝图：文件清单、类骨架、DDL、API 契约、AI Schema、可执行 SQL |
| **DOC A** | `AI跑团GM·微服务毕设-架构设计.md`（4410 行） | 设计意图：架构动机、时序图、风险权衡 |

**DOC B 关键行号**：基线技术栈 `L176` · 仓库结构 `L220` · 全局约定 `L310`（错误码 `L340` / Feign+端口 `L393` / Nacos `L410` / CORS `L420`）· 完整 DDL `L433` · REST API 契约 `L669` · **AI 引擎契约 `L756`**（condition 小语法 `L789` / 输出 schema `L831` / **白名单规则 §6.5 `L863`**）· 种子剧本「迷雾古宅」`L879` · **game-service 回合六步编排 `L5468`** · 白名单二次校验 `L5520` · 禁止事项 `L97`。

**DOC A 关键行号**：整体架构图 `L179` · **回合端到端时序 `L375`** · AI 设计深挖 `L715` · 数据模型 ER `L1231` · 测试策略 `L2922`。

**裁决规则**：DOC A `L488` 说"白名单只在 ai-engine 校验"——**错误**，以 DOC B §6.5 `L863` + §9 `L5520` 为准：game-service 做**权威二次校验**。

---

## 技术栈（版本锁定，禁止跨大版本升降）

| 层 | 技术 | 版本 |
|---|---|---|
| 运行时 | JDK | **17** |
| 构建 | Maven | **3.9.x** |
| 框架三件套（必须整组用） | Spring Boot / Spring Cloud / Spring Cloud Alibaba | **3.2.5 / 2023.0.1 / 2023.0.1.0** |
| 服务注册+配置 | Nacos Server | **2.3.x**（standalone） |
| ORM | MyBatis-Plus | **3.5.5**（必须用 `mybatis-plus-spring-boot3-starter`） |
| **鉴权（实际）** | **sa-token** | **1.39.0**（覆盖文档 jjwt） |
| API 文档 | springdoc-openapi | **2.5.0** |
| 主库 | MySQL | **8.0.x**（`aigm_user` / `aigm_scenario` / `aigm_game`） |
| 向量库 | PostgreSQL | **16.x** + pgvector **0.7.x**（`aigm_memory`，维度严格 **1024**，目前降级 REAL[]） |
| 前端 | Vue / Vite / Element Plus / Pinia / Axios / Node | **3.4 / 5 / 2.7 / 2.1 / 1.7 / 20.x** |

---

## 仓库结构

```
ai-gm-trpg/
├── common/              # 无启动类：R/ResultCode/BizException/GlobalExceptionHandler
│                        # UserContext/ConditionEvaluator/PageQuery+Result
│                        # Feign 接口（AiEngineClient/MemoryClient/ScenarioClient）+ 20+ DTO
├── gateway/             # Spring Cloud Gateway(WebFlux)：sa-token 全局校验 + X-User-* 头 + CORS
├── user-service/        # 注册/登录/sa-token 签发/RBAC（MySQL aigm_user）
├── scenario-service/    # 剧本/节点/NPC/分支 CRUD + 结构校验 + 运行时只读 Feign 接口
├── game-service/        # ★编排核心：开局 + 回合六步编排 + 白名单二次校验 + 事务落库
├── ai-engine-service/   # 无状态：PromptBuilder→LLM→OutputValidator（INTERNAL）
├── memory-service/      # 长程记忆 store/recall RAG（INTERNAL）
├── frontend/            # Vue3 六大页面（Login/Register/GameHall/GamePlay/ScenarioEditor/UserManage）
├── deploy/
│   ├── start-all.sh     # 一键启动 6 个后端服务（按依赖顺序）
│   ├── nacos/           # Nacos 配置文件 + publish.sh（7 个 dataId）
│   ├── mysql-init/      # DB 初始化 SQL（01-schema-user/02-scenario/03-game/04-seed/05-seed-cafe）
│   └── pg-init/         # PostgreSQL 初始化（pgvector 版 + fallback REAL[] 版）
├── docs/
│   ├── adr/             # 架构决策记录（ADR-0001~0004，见下文）
│   └── agents/          # Agent 技能配置（issue-tracker/triage-labels/domain）
├── .env.example         # LLM/Embedding/中间件密钥模板（填写后保存为 .env，已 gitignore）
├── CONTEXT.md           # 领域语言词汇表
└── README.md            # 快速启动 + 冒烟验证
```

**模块依赖铁律**：业务服务之间**不直接依赖对方源码**，只通过 `common/feign` 的 Feign 接口 + DTO 调用。`gateway` 只复用 `common` 的工具类，**禁止引入 spring-boot-starter-web（WebMVC）**。

---

## 服务命名与端口

| spring.application.name | 端口 | 网关前缀 |
|---|---|---|
| `gateway` | **8080** | 唯一对外入口；前端 `axios.baseURL=http://localhost:8080` |
| `user-service` | 8081 | `/api/user/**` |
| `scenario-service` | 8082 | `/api/scenario/**`（`/api/scenario/run/**` 走 Feign 内网） |
| `game-service` | 8083 | `/api/game/**` |
| `ai-engine-service` | 8084 | `/api/ai/**` — **INTERNAL，网关不对外路由** |
| `memory-service` | 8085 | `/api/memory/**` — **INTERNAL，网关不对外路由** |

---

## 构建与运行

中间件（Nacos/MySQL/Redis/PostgreSQL）运行在**远端服务器**，本地无需 docker-compose。凭据见 `README.md § 中间件` 或 `.env.example`。

```bash
# ── 首次环境初始化 ──────────────────────────────────────────────
# 1) 建库 + 种子（需本地 mysql/psql 客户端）
mysql  -h<MIDDLEWARE_HOST> -uroot -p<PASSWORD> < deploy/mysql-init/01-schema-user.sql
mysql  -h<MIDDLEWARE_HOST> -uroot -p<PASSWORD> < deploy/mysql-init/02-schema-scenario.sql
mysql  -h<MIDDLEWARE_HOST> -uroot -p<PASSWORD> < deploy/mysql-init/03-schema-game.sql
mysql  -h<MIDDLEWARE_HOST> -uroot -p<PASSWORD> < deploy/mysql-init/04-seed.sql
psql "host=<MIDDLEWARE_HOST> dbname=aigm_memory user=postgres" \
     -f deploy/pg-init/01b-schema-memory-fallback.sql   # 无 pgvector 时用 fallback

# 2) 发布 Nacos 配置（首次必做，7 个 dataId）
bash deploy/nacos/publish.sh
# Nacos 控制台：http://<MIDDLEWARE_HOST>:8848/nacos（nacos / 密码见 README）

# ── 日常开发 ────────────────────────────────────────────────────
# 3) 填写 LLM / Embedding key（可选，缺 key 走确定性兜底）
cp .env.example .env   # 编辑填入 LLM_API_KEY / EMBEDDING_API_KEY

# 4) 全量构建
mvn -DskipTests clean package   # 产物：各服务 target/*.jar

# 5) 一键启动（推荐）
bash deploy/start-all.sh        # 按序起 6 服务，日志写 /tmp/aigm-*.log
# 或逐个启动（开发期热重载友好）
mvn -pl user-service     spring-boot:run
mvn -pl gateway          spring-boot:run
mvn -pl scenario-service spring-boot:run
mvn -pl game-service     spring-boot:run
mvn -pl ai-engine-service spring-boot:run
mvn -pl memory-service   spring-boot:run

# 停止后端：pkill -f 'spring-boot:run'  或  pkill -f 'aigm-'

# 6) 前端
cd frontend && npm install && npm run dev   # http://localhost:5173
```

**启动顺序**（强依赖）：Nacos 就绪 → `user-service` → 其余后端服务 → 前端。`start-all.sh` 中已固化顺序（ai-engine → memory → scenario → user → [sleep 8] → game → gateway）。

**冒烟验证**（见 README）：
```bash
TOKEN=$(curl -s -X POST localhost:8080/api/user/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"player","password":"123456"}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
curl -s -X POST localhost:8080/api/game/sessions \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"scenarioId":1}'   # 返回包含 firstTurn.narrative 即成功
curl -s -o /dev/null -w '%{http_code}\n' -X POST localhost:8080/api/ai/generate  # 应 404
```

**测试**：
```bash
mvn -pl common test          # ConditionEvaluator 受控小语法（7 项）
mvn -pl game-service test    # TransitionWhitelist 防跑偏（5 项，含 1503 越界拒绝）
```

---

## 核心架构：回合六步编排（动 game-service 前必读）

运行时状态**唯一事实源 = `t_game_state`**（game-service）。调用关系有向无环：

```
game-service（编排者）
  ├─ Feign→ scenario-service  取节点定义 + NPC persona + transitions 白名单
  ├─ Feign→ memory-service    recall（弱依赖，失败降级为空）
  ├─ Feign→ ai-engine-service generate（LLM 慢调用，事务外）
  └─ 本地事务 DB 写（快，事务内）→ 事务后 Feign→ memory-service store
```

`POST /api/game/sessions/{id}/turns` **六步**（`GameService.java`）：

1. **取状态**：查 `t_game_session`（1220 不存在 / 1221 非本人 / 1202 已结束）+ `t_game_state`
2. **取轨道**：Feign→scenario-service 取节点定义 + NPC(persona) + `transitions` 白名单
3. **RAG 召回**：Feign→memory-service `recall`（弱依赖，失败静默降级为空）
4. **调 AI**：`GenerateRequest`→ai-engine→LLM（`response_format=json_object`）→结构化 JSON；ai-engine 内部做预校验/纠偏（JSON 1501 重试一次再降级 / Schema 1502 补默认 / 白名单预检 / clamp）
5. **白名单权威二次校验**（事务内）：game-service 用**应用 stateChanges 后的 state** 对 `proposedTransition` 再校验——`toNodeId` 必须 `null` 或 ∈ 当前节点 `transitions` 且 condition 求真，否则记 **1503**、强制停留
6. **落库**（同一本地事务）：UPDATE `t_game_state` + INSERT `t_turn` + `turn_count++`；目标节点 `isEnding=1` → status 置 WIN(2)/LOSE(3)；**事务提交后**才 Feign→memory-service `store`（失败仅 warn）

> **不变量**：白名单校验做两次（ai-engine 预检 + game-service 权威），慢调用（recall/generate）在事务外，事务内只做快速本地 DB 写。

**condition 受控小语法**（`ConditionEvaluator.java`，编辑期 ScenarioEditor 与运行期 game-service 共享同一集合）：
- `always` — 无条件通过
- `flag.<key>==true|false` — flag 缺省 false
- `attr.<key><op><整数>` — op ∈ `>= <= > < ==`，**无 `!=`**，attr 缺省 0
- `item.<物品名>` — 检查背包
- ` && ` 串联（**只支持 `&&`，不支持 `||`**；多分支用多条 transition + priority）
- 看不懂的表达式一律返回 false；空表达式 = `always`

---

## 全局约定

- **统一返回体 `R<T>`**：`{code, message, data}`，`code=0` 成功；HTTP 恒 200（仅网关鉴权失败返 401/403），业务错误靠 `code`。`R.ok()` / `R.ok(data)` / `R.fail(ResultCode)` / `R.fail(code, msg)`。
- **错误码 `ResultCode` 枚举**（禁止造别名）：AUTH `1001-1006` · PARAM `1100-1103` · BIZ `1200-1221`（`STATE_INVALID`=1202 / `GAME_SESSION_FORBIDDEN`=1221）· AI `1500-1504`（`AI_LLM_BAD_JSON`=1501 / `AI_TRANSITION_REJECTED`=1503）· SYS `1900-1903`（`SERVICE_UNAVAILABLE`=1901）。全表见 DOC B `L340`。
- **鉴权（实际实现为 sa-token）**：gateway `AuthGlobalFilter` 读 Redis 会话校验 → 注入 `X-User-Id` / `X-User-Name` / `X-User-Roles`（逗号分隔）透传；**下游信任网关头做方法级 RBAC，禁止重复解析 token**。角色：`PLAYER` / `AUTHOR` / `ADMIN`。
- **内部接口**：`/api/ai/**`、`/api/memory/**`、`/api/scenario/run/**` 仅供 Feign 内网直连，网关不对外路由；Feign 传 `X-Internal-Call: true`。
- **Feign 超时**：AI 相关 readTimeout **60s**（LLM 慢），其余默认 5s；失败抛 `BizException(SERVICE_UNAVAILABLE)`(1901)。
- **Nacos**：namespace `public` / group `DEFAULT_GROUP` / profile `local` / file-extension `yaml`；共享 dataId `aigm-common.yaml`（所有服务含 gateway 都要 shared-configs 引入）+ 各服务 `<name>.yaml`。
- **CORS** 只在 gateway 配（来源 `http://localhost:5173`），下游不再配。
- **时间格式** `yyyy-MM-dd HH:mm:ss`（GMT+8）；**分页** `page` 从 1 起、`size` 默认 10 上限 100。
- **id 命名**：节点 `node_<英文>`、NPC `npc_<英文>`、flag 小写蛇形；DB 主键自增 BIGINT，白名单校验用数值 id。

---

## 关键文件速查

| 文件 | 作用 |
|---|---|
| `game-service/.../GameService.java` | 六步回合编排入口 |
| `game-service/.../TransitionWhitelist.java` | 权威白名单校验（含 1503 拒绝） |
| `common/.../condition/ConditionEvaluator.java` | condition 受控小语法解析引擎 |
| `common/.../feign/AiEngineClient.java` | AI 生成 Feign 接口 |
| `common/.../feign/MemoryClient.java` | 记忆 store/recall Feign 接口 |
| `common/.../feign/ScenarioClient.java` | 剧本运行时 Feign 接口 |
| `gateway/.../AuthGlobalFilter.java` | sa-token 校验 + X-User-* 头注入 |
| `ai-engine-service/.../PromptBuilder.java` | Prompt 组装（含 NPC persona 注入） |
| `ai-engine-service/.../OutputValidator.java` | LLM 输出 Schema 校验/纠偏 |
| `memory-service/.../MemoryServiceImpl.java` | REAL[] 余弦相似度 store/recall |
| `frontend/src/views/GamePlay.vue` | 核心游戏 UI（14 KB，回合提交 + NPC 对话） |
| `frontend/src/views/ScenarioEditor.vue` | Coze 风格可视化剧本编辑器（31 KB，节点画布） |
| `frontend/src/api/index.js` | 30+ API endpoint 封装 + Axios 拦截器 |
| `deploy/start-all.sh` | 一键启动 6 服务（按顺序，自动加载 .env） |
| `deploy/nacos/publish.sh` | 发布 7 个 Nacos dataId |
| `deploy/mysql-init/04-seed.sql` | 种子账号 + 「迷雾古宅」剧本 |

---

## 架构决策记录（docs/adr/）

| ADR | 结论 |
|---|---|
| 0001 | 白名单约束节点图可达性，不约束 state 值 |
| 0002 | 游戏结局只能通过白名单 transition 到达，不走特殊逻辑 |
| 0003 | publish 校验做结构完整性（可达性/分支合法），不校验「能否通关」 |
| 0004 | NPC 秘密通过 evidence 属性阈值解锁 |

---

## 前端结构

```
frontend/src/
├── views/
│   ├── Login.vue          # 登录
│   ├── Register.vue       # 注册
│   ├── GameHall.vue       # 大厅：已发布剧本列表 + 历史对局
│   ├── GamePlay.vue       # ★游戏主界面：回合提交 / NPC 对话 / 状态看板 / 记忆日志
│   ├── ScenarioEditor.vue # ★剧本编辑器：Coze 风格节点画布（@vue-flow + dagre 布局）
│   └── UserManage.vue     # ADMIN 专用用户管理
├── api/
│   ├── index.js           # 30+ API 封装（game/scenario/user/auth）
│   └── request.js         # Axios 实例 + Authorization 拦截器
├── store/user.js          # Pinia：用户状态 / login / logout / 角色
└── components/
    ├── TopBar.vue         # 顶部导航栏
    ├── StateBar.vue       # 实时 flag/attr/item 状态展示
    ├── InvestigationLog.vue # 记忆/回合历史日志
    └── StatusTag.vue      # 对局状态徽标
```

---

## 硬性禁止事项

1. **禁止偏离命名**：服务/模块/表/字段/接口路径/JSON 字段/角色/错误码/id 规范一律以基线为准。
2. **禁止换技术栈/跨大版本升降**；Spring Boot/Cloud/Alibaba 三件套必须整组用。
3. **禁止超纲扩大 AI**：只调 OpenAI 兼容接口，不自训模型、不做自主多 Agent、不做语音。
4. **禁止破坏状态机约束**：LLM 跳转绝不能绕过白名单校验（基线 §6.5）。
5. **gateway 禁止引入 WebMVC**；下游禁止重复配 CORS。
6. **下游禁止重复解析 token**（只信网关注入的 X-User-* 头）。
7. **内部接口禁止对外暴露**（`/api/ai/**`、`/api/memory/**`）。
8. **禁止泄露密钥**：LLM API Key、sa-token secret 不得硬编码或提交（用 `.env`，已 gitignore）。

---

## Agent Skills

本仓库已按工程技能集配置，供 `to-issues`、`triage`、`to-prd`、`qa`、`improve-codebase-architecture`、`diagnose`、`tdd`、`grill-with-docs` 等技能读取。

### Issue tracker
issue 与 PRD 作为 markdown 文件存放在 `.scratch/<feature-slug>/`（本地跟踪器）。详见 `docs/agents/issue-tracker.md`。

### Triage labels
五个状态名：`needs-triage` / `needs-info` / `ready-for-agent` / `ready-for-human` / `wontfix`，记录在每个 issue 文件顶部的 `Status:` 行。详见 `docs/agents/triage-labels.md`。

### Domain docs
单一上下文：根目录 `CONTEXT.md` + `docs/adr/`（由 `/grill-with-docs` 在术语/决策定型时懒创建）。详见 `docs/agents/domain.md`。
