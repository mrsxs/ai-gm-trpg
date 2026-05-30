# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 这个仓库现在是什么

> **实现状态（2026-05-30 更新）**：项目已按文档完整实现并端到端跑通——8 个 Maven 模块 + Vue3 前端均已落地，连远端中间件（Nacos/MySQL/Redis/PG @<MIDDLEWARE_HOST>）可一键启动，登录 `player/123456` 开局「迷雾古宅」可连续回合通关。**鉴权改用 sa-token**（覆盖文档 JWT，见 README §差异），memory 因远端无 pgvector 降级为 `REAL[]`+应用层余弦，无 LLM key 时 ai-engine/embedding 走确定性兜底。运行/架构/差异见 **`README.md`**。下文的「从头实现」描述为历史规划基线，施工细节仍以两份设计文档为 SSOT。

原始状态为**纯设计文档、零代码的绿地项目**。两份文档是唯一事实源：

| 简称 | 文件 | 回答 | 角色 |
|---|---|---|---|
| **DOC B** | `AI跑团GM·微服务毕设-实现规格书(AI可执行).md`（9746 行） | 怎么建 / 按什么顺序建：文件清单、类骨架、配置、可执行 SQL、逐阶段 DoD | **施工蓝图，动手时以它为准** |
| **DOC A** | `AI跑团GM·微服务毕设-架构设计.md`（4410 行） | 是什么 / 为什么：架构动机、选型论证、时序、风险权衡 | 理解设计意图 |

**最高裁决规则**：DOC B 的「实现基线」（§一，DOC B `L167` 起）是两份文档共享的 Single Source of Truth——服务命名、DDL、API 契约、错误码、AI 引擎 JSON Schema、id 命名规范一律以此为准。**当 DOC A 的描述与基线冲突时，实现以基线为准**（已发现一处：DOC A `L488` 说「白名单只在 ai-engine 校验、game-service 仅落库」，而基线 §6.5 `L863` 与 DOC B §9 `L5520` 要求 game-service 做**权威二次校验**——以基线为准）。

## 项目是什么（一句话）

单人 AI 跑团 / 互动叙事「游戏主持人(GM)」。玩家自由文本输入 → AI 即兴叙事并分饰 NPC → 但 AI 的剧情跳转被编剧预设的**状态机白名单**死死兜住，永远跳不出轨道。三大 AI 难点：①状态机约束 LLM 防跑偏 ②多 NPC 人格一致 ③长程记忆 RAG。统一基准日期 **2026-05-29**。

## 文档导航（按行号直达，省去重新 grep）

**DOC B 实现规格书**：基线技术栈 `L176` · 仓库结构 `L220` · 全局约定 `L310`（R `L312` / 错误码 `L340` / JWT `L376` / Feign+端口 `L393` / Nacos `L410` / CORS `L420`）· 完整 DDL `L433` · REST API 契约 `L669` · **AI 引擎契约 `L756`**（condition 小语法 §6.2.1 `L789` / 输出 schema §6.4 `L831` / **白名单规则 §6.5 `L863`**）· 种子剧本「迷雾古宅」`L879`（id 命名 `L912` / 可执行 SQL `L920`）· common `L986` · user-service `L1485` · gateway `L2861` · scenario-service `L3199` · **game-service `L5122`**（开局 `L5410` / **回合六步编排 `L5468`** / 白名单二次校验 `L5520` / 落库事务 `L5553`）· ai-engine-service `L5676` · memory-service `L7187` · 前端 `L8158` · **DevOps/本地运行 `L9074`**（docker-compose `L9144` / 构建运行 `L9239` / DB init `L9304` / Nacos 配置 `L9385` / 一键启动顺序 `L9528` / **构建阶段 P0-P8 `L9729`**）· 前置环境 `L81` · **禁止事项 `L97`**。

**DOC A 架构设计**：整体架构图 `L179` · 服务职责表 `L237` · **回合端到端时序 `L375`** · AI 设计深挖 `L715` · 数据模型 ER `L1231` · 韧性/降级 `L1447`（事务边界 `L2095` / Feign 降级 `L2163`）· 安全 `L2345` · 测试策略 `L2922` · 排期 `L3944` · 答辩要点 `L4150`。

## 技术栈（版本锁定，禁止跨大版本升降；三件套必须整组用）

JDK **17** · Maven **3.9.x** · Spring Boot **3.2.5** · Spring Cloud **2023.0.1** · Spring Cloud Alibaba **2023.0.1.0**（Nacos/Sentinel）· Nacos Server **2.3.x**（standalone，注册+配置中心）· MyBatis-Plus **3.5.5**（必须用 `mybatis-plus-spring-boot3-starter`）· jjwt **0.12.x** · springdoc-openapi **2.5.0** · MySQL **8.0.x**（承载 `aigm_user`/`aigm_scenario`/`aigm_game` 分库）· PostgreSQL **16.x** + pgvector **0.7.x**（`aigm_memory`，向量维度**严格 1024**）· 前端 Vue **3.4** + Vite **5** + Element Plus **2.7** + Pinia **2.1** + axios **1.7** + Node **20.x**。

> 混搭其它大版本会启动即报 `Spring Cloud version compatibility`。详见基线 §1 `L176`。

## 目标仓库结构（Maven 多模块 + 独立前端，模块名勿改）

根 `ai-gm/`：聚合 `pom.xml`(packaging=pom) + `docker-compose.yml` + 8 个后端模块 + `frontend/`。包名统一 `com.aigm.<service>`。

```
common/         # 公共地基：R/ResultCode/BizException/GlobalExceptionHandler/JwtUtil/PageQuery+Result/常量/feign 接口+DTO（无主类，不可 spring-boot:run）
gateway/        # Spring Cloud Gateway(WebFlux)：路由 + JWT 全局校验 + 下发 X-User-* 头 + CORS
user-service/   # 注册/登录/JWT 签发/RBAC（MySQL aigm_user）
scenario-service/ # 剧本/节点/NPC/分支 CRUD + 状态机建模 + 运行时只读接口（MySQL aigm_scenario）
game-service/   # ★编排核心：对局/状态/回合，开局+回合六步编排+白名单二次校验+事务（MySQL aigm_game）
ai-engine-service/ # 无状态：状态+输入 → PromptBuilder → LLM → OutputValidator → 结构化输出
memory-service/ # pgvector 长程记忆：store/recall RAG（PostgreSQL aigm_memory）
frontend/       # Vue3 五大页面：Login/Register/GameHall/GamePlay/ScenarioEditor/UserManage
```

**模块依赖铁律**：业务服务之间**不直接依赖对方源码**，只通过 `common/feign` 里的 Feign 接口 + DTO 调用。`common` 被所有业务服务依赖。`gateway` 只复用 `common` 的 `JwtUtil`/`R`，**不得引入 WebMVC（spring-boot-starter-web）**。

## 服务命名与端口（全局唯一标准，所有 yml/脚本/Feign 以此为准）

| spring.application.name | 端口 | 网关前缀 |
|---|---|---|
| `gateway` | **8080** | 唯一对外入口；前端 `axios.baseURL=http://localhost:8080` |
| `user-service` | 8081 | `/api/user/**` |
| `scenario-service` | 8082 | `/api/scenario/**`（`/api/scenario/run/**` 走 Feign 内网） |
| `game-service` | 8083 | `/api/game/**` |
| `ai-engine-service` | 8084 | `/api/ai/**` — **INTERNAL，网关不对外路由** |
| `memory-service` | 8085 | `/api/memory/**` — **INTERNAL，网关不对外路由** |

## 构建与运行命令

```bash
# 1) 起中间件（仓库根，docker-compose 提供 MySQL/PostgreSQL/Nacos）
docker compose up -d            # docker compose ps 三容器 healthy（~40s）
docker compose down -v          # 彻底重置：清空所有数据并让 init 脚本重建库表+种子

# 2) 发布 Nacos 配置（首次必做一次，控制台 http://localhost:8848/nacos，nacos/nacos）
#    7 个 dataId：aigm-common.yaml(共享 jwt secret) + 各服务 <name>.yaml，见 DOC B §5 L9385

# 3) 全量构建（仓库根；-DskipTests 加速本地启动）
mvn clean install -DskipTests   # 产物为各服务 target/*.jar（fat jar）；common 只是被依赖的库
mvn -pl common install          # 只装 common（其它模块报「找不到 common 类」时先跑这个）

# 4) 注入 LLM/Embedding API Key（本终端，勿提交；或放 .env）
export LLM_API_KEY=...          # 详见 DOC B §6 L9452

# 5) 逐个起服务（开发期推荐插件直跑，热重启方便）。启动顺序：gateway → user → 其余
mvn -pl gateway          spring-boot:run
mvn -pl user-service     spring-boot:run
mvn -pl scenario-service spring-boot:run
mvn -pl game-service     spring-boot:run
mvn -pl ai-engine-service spring-boot:run
mvn -pl memory-service   spring-boot:run
java -jar user-service/target/user-service-1.0.0.jar   # 或跑构建好的 jar（更接近部署）

# 一键脚本（DOC B 提供）：deploy/start-all.sh 起中间件+全部后端（后台）
# 停止：pkill -f spring-boot:run（后端）+ docker compose down（中间件）

# 6) 前端
cd frontend && npm install && npm run dev   # http://localhost:5173
```

**启动强依赖顺序**：中间件 → 发布 Nacos 配置 → Nacos 就绪 → gateway+业务服务（先 user 后其余）→ 前端。ai-engine/memory 无状态可与业务服务并起，但要在 game-service 真正开局前就绪。详见 DOC B §7 `L9528`。

**冒烟验证**：种子账号 `player/123456` 登录拿 token → `/api/game/sessions` 开局应返回 `firstTurn.narrative`；`/api/ai/**` 与 `/api/memory/**` 经网关访问应被拒（内部隔离）。Swagger 经网关 `http://localhost:8080/doc/<service>.html`。

**测试**：测试尚未编写。框架按 DOC A §八（`L2922`）= JUnit5 + Testcontainers（集成起真库）+ WireMock（stub 下游 Feign），由顶层 parent 统一管理。约定写出后，单测跑法：`mvn -pl <module> test -Dtest=类名#方法名`。

## 核心架构：回合编排（系统心脏，动 game-service 前必读）

运行时状态的**唯一事实源 = game-service 的 `t_game_state`**；scenario-service 只给「剧本静态定义（节点/NPC/分支白名单）」；ai-engine-service **完全无状态**（状态+输入→结构化输出的纯函数）；memory-service 是**弱依赖侧库**（召回失败降级为空、不阻断回合）。`game-service` 是**唯一编排者**，调用关系有向无环。

`POST /api/game/sessions/{id}/turns` 的**六步编排**（DOC B §8 `L5468`）：

1. **取状态**：查 `t_game_session`（1220 不存在 / 1221 非本人 / 1202 已结束）+ `t_game_state`。
2. **取轨道**：Feign→scenario-service 取当前节点定义 + 出场 NPC(persona) + `transitions` 白名单。
3. **RAG 召回**：Feign→memory-service `recall`（每回合、弱依赖，失败静默降级为空）。
4. **调 AI**：拼 `GenerateRequest`→ai-engine→LLM(`response_format=json_object`)→结构化 JSON。**ai-engine 内部先做预校验/纠偏**（JSON 1501→重试1次再降级 / Schema 1502 补默认 / 白名单预检 / clamp）。
5. **白名单权威二次校验**（事务内）：game-service 用**应用 stateChanges 后的 state** 对 `proposedTransition` 再校验一次——`toNodeId` 必须 `null` 或 ∈ 当前节点 `transitions` 且 condition 求真，否则记 **1503**、强制停留（防御性，下游不信任上游；condition 的**权威求值在 game-service**）。
6. **落库**（同一本地事务）：UPDATE `t_game_state`(含 current_node/recent_summary) + INSERT `t_turn`(完整 JSON) + `turn_count++`；目标节点 `isEnding=1` → status 置 WIN(2)/LOSE(3)。**事务提交后**才 Feign→memory-service `store` 回写记忆（失败仅 warn、不回滚）。

> 关键不变量：白名单校验**做两次**（ai-engine 预校验 + game-service 权威校验），game-service 是最终事实闸。慢/易失败的外部调用（recall/generate）在事务外，事务内只做本地快速 DB 写。

**condition 受控小语法**（编辑期与运行期必须实现完全相同的集合，DOC B §6.2.1 `L789`）：`always` / `flag.<key>==true|false`（缺省 false）/ `attr.<key><op><整数>`（op ∈ `>= <= > < ==`，**无 `!=`**，缺省 0）/ `item.<物品名>` / 用 ` && ` 串联（**只支持 `&&`，不支持 `||`**，多分支用多条 transition + priority）。**看不懂的表达式一律返回 false**（宁可少放行）；空表达式 = `always`。

## 全局约定（实现时严格对齐基线 §3 `L310`）

- **统一返回体 `R<T>`**：`{code, message, data}`，`code=0` 成功；HTTP 恒 200（仅网关鉴权失败返 401/403），业务结果靠 `code`。方法 `R.ok()/R.ok(data)/R.fail(ResultCode)/R.fail(code,msg)`。
- **错误码 `ResultCode` 枚举名是全局唯一标准、禁止造别名**：AUTH `1001-1006` · PARAM `1100-1103` · BIZ `1200-1221`（`STATE_INVALID`=1202、`GAME_SESSION_FORBIDDEN`=1221）· AI `1500-1504`（`AI_TRANSITION_REJECTED`=1503 越界被拒、`AI_LLM_BAD_JSON`=1501）· SYS `1900-1903`（`SERVICE_UNAVAILABLE`=1901）。全表见 `L340`。
- **JWT 只在 gateway 校验**（HS256，secret 由 Nacos `aigm-common.yaml` 下发，全服务共享）；校验后注入 `X-User-Id`/`X-User-Name`/`X-User-Roles`(逗号分隔) 透传，**下游信任网关头做方法级 RBAC，禁止重复解析 token**。角色：`PLAYER`/`AUTHOR`/`ADMIN`。
- **内部接口**：`/api/ai/**`、`/api/memory/**`、`/api/scenario/run/**` 仅供 Feign 内网直连，网关不对外路由；Feign 传 `X-Internal-Call: true`。
- **Feign 超时**：AI 相关 readTimeout **60s**（LLM 慢），其余默认 5s；失败统一抛 `BizException(SERVICE_UNAVAILABLE)`(1901)。
- **Nacos**：namespace `public` / group `DEFAULT_GROUP` / profile `local` / file-extension `yaml`；共享 dataId `aigm-common.yaml`（所有服务含 gateway/ai-engine/memory 都要 shared-configs 引入）+ 各服务 `<name>.yaml`。
- **CORS** 只在 gateway 配（来源 `http://localhost:5173`），下游不再配。**时间格式** `yyyy-MM-dd HH:mm:ss`(GMT+8)。**分页** `page` 从 1 起、`size` 默认 10 上限 100。
- **id 命名**（DOC B §7.6 `L912`）：节点 `node_<英文>`、NPC `npc_<英文>`、flag 小写蛇形；DB 主键自增 BIGINT，**AI 白名单校验对外用数值 id**，业务 key 仅供人读对齐。

## 构建阶段（施工主线，强依赖、勿跳阶段并行乱做；DOC B §11 `L9729`）

`P0 工程骨架`(根 pom + 空模块 + docker-compose) → `P1 common 地基` → `P2 Nacos 配置+建库+种子` → `P3 鉴权链路 gateway+user` → `P4 业务数据 scenario` → `P5 AI 与记忆 ai-engine+memory`（可与 P3/P4 并行编码）→ `P6 编排核心 game-service` → `P7 前端五大页面` → `P8 端到端联调`。每阶段有 DoD，达标再进下一阶段。

## 硬性禁止事项（DOC B `L97`）

1. **禁止偏离命名**：服务/模块/库/表/字段/接口路径/JSON 字段/角色/错误码/id 规范一律以基线为准，不新增、不重命名、不改大小写。
2. **禁止换技术栈/跨大版本升降**；三件套必须整组用。
3. **禁止超纲扩大 AI**：只调现成 OpenAI 兼容接口，不自训模型、不做自主多 Agent 编排、不做语音。
4. **禁止破坏状态机约束**：LLM 跳转绝不能绕过白名单校验（基线 §6.5）。
5. **gateway 禁止引入 WebMVC**；下游禁止重复配 CORS。
6. **下游禁止重复解析 JWT**（只信网关头）。
7. **内部接口禁止对外暴露**（`/api/ai/**`、`/api/memory/**`）。
8. **禁止泄露密钥**：LLM API Key、JWT secret 不得硬编码或提交。
9. **禁止占位式交付**：不用 TODO/待补充充数，每阶段交付可运行实现。

## Agent skills

本仓库已按工程技能集做好配置，供 `to-issues`、`triage`、`to-prd`、`qa`、`improve-codebase-architecture`、`diagnose`、`tdd`、`grill-with-docs` 等技能读取。

### Issue tracker

issue 与 PRD 作为 markdown 文件存放在 `.scratch/<feature-slug>/`（本地跟踪器，本仓库无 git 远端）。详见 `docs/agents/issue-tracker.md`。

### Triage labels

使用五个默认规范状态名（`needs-triage` / `needs-info` / `ready-for-agent` / `ready-for-human` / `wontfix`），记录在每个 issue 文件顶部的 `Status:` 行。详见 `docs/agents/triage-labels.md`。

### Domain docs

单一上下文：根目录 `CONTEXT.md` + `docs/adr/`（找不到则静默跳过，由 `/grill-with-docs` 在术语/决策定型时懒创建）。详见 `docs/agents/domain.md`。
