# AI 跑团 GM · 微服务毕设

单人 AI 跑团 / 互动叙事「游戏主持人(GM)」。玩家自由文本输入 → AI 即兴叙事并分饰 NPC，但剧情跳转被编剧预设的**状态机白名单**死死兜住，永远跳不出轨道。

> 三大 AI 难点：①状态机约束 LLM 防跑偏 ②多 NPC 人格一致 ③长程记忆 RAG。统一基准日期 **2026-05-29**。

## 架构（8 模块 + 前端）

| 模块 | 端口 | 职责 |
|---|---|---|
| `gateway` | 8080 | 唯一对外入口：路由 + **sa-token 鉴权** + 下发 `X-User-*` 头 + CORS |
| `user-service` | 8081 | 注册/登录/sa-token 签发/RBAC（MySQL `aigm_user`） |
| `scenario-service` | 8082 | 剧本/节点/NPC/分支 CRUD + 状态机建模 + 发布结构校验 + 运行时只读接口（MySQL `aigm_scenario`） |
| `game-service` | 8083 | **编排核心**：开局 + 回合六步编排 + 白名单权威二次校验 + 事务落库（MySQL `aigm_game`） |
| `ai-engine-service` | 8084 | 无状态 PromptBuilder→LLM→OutputValidator（INTERNAL，网关不对外） |
| `memory-service` | 8085 | pgvector 长程记忆 store/recall RAG（PostgreSQL `aigm_memory`，INTERNAL） |
| `common` | — | 公共地基：`R`/`ResultCode`/异常/分页/`UserContext`/`ConditionEvaluator`/Feign 契约 |
| `frontend` | 5173 | Vue3 + Vite5 + Element Plus + Pinia 五大页面 |

技术栈：JDK17 · Spring Boot 3.2.5 · Spring Cloud 2023.0.1 · Spring Cloud Alibaba 2023.0.1.0（Nacos/Sentinel）· MyBatis-Plus 3.5.5 · **sa-token 1.39** · MySQL 8 · PostgreSQL 16。

## 与设计文档的差异（已落地并标注）

1. **鉴权用 sa-token 替代文档的 JWT**：默认模式（随机 token + Redis 共享会话），`token-name=Authorization`/`token-prefix=Bearer`，前端仍发 `Authorization: Bearer <token>`。网关用 sa-token 读 Redis 会话校验 + 下发 `X-User-*` 头，下游只信头做 RBAC（文档契约不变）。
2. **远端 PostgreSQL 未装 pgvector**：memory-service 降级为 `REAL[]` 列 + 应用层余弦相似度（种子规模够用）；装上 pgvector 后切回 `deploy/pg-init/01-schema-memory.sql` 即可。
3. **无 LLM/Embedding Key 时确定性兜底**：ai-engine 用 stub（按关键词推进状态机、符合「多数回合停留」语义）、memory 用哈希伪向量；配置注入真实 key 即走 OpenAI 兼容接口。

## 中间件（远端已就绪）

| 组件 | 地址 | 账号/密码 |
|---|---|---|
| Nacos | http://123.57.166.60:8848/nacos | nacos / 3100880856 |
| MySQL | 123.57.166.60:3306 | root / 3100880856 |
| Redis | 123.57.166.60:6379 | — / 123456 |
| PostgreSQL | 123.57.166.60:5432 | postgres / 3100880856 |

## 运行

```bash
# 1) 建库 + 种子（首次，需本地 mysql/psql 客户端）
mysql  -h123.57.166.60 -uroot -p3100880856 < deploy/mysql-init/01-schema-user.sql
mysql  -h123.57.166.60 -uroot -p3100880856 < deploy/mysql-init/02-schema-scenario.sql
mysql  -h123.57.166.60 -uroot -p3100880856 < deploy/mysql-init/03-schema-game.sql
mysql  -h123.57.166.60 -uroot -p3100880856 < deploy/mysql-init/04-seed.sql
psql "host=123.57.166.60 port=5432 user=postgres dbname=aigm_memory" -f deploy/pg-init/01b-schema-memory-fallback.sql

# 2) 发布 Nacos 配置（7 个 dataId）
bash deploy/nacos/publish.sh

# 3) 构建 + 启动全部后端
mvn -DskipTests clean package
bash deploy/start-all.sh          # 6 个服务，日志 /tmp/aigm-*.log
# 可选真实 LLM：export LLM_API_KEY=... EMBEDDING_API_KEY=...

# 4) 前端
cd frontend && npm install && npm run dev   # http://localhost:5173
```

种子账号（密码均 `123456`）：`admin`(ADMIN) / `author`(AUTHOR) / `player`(PLAYER)。
种子剧本「迷雾古宅」已发布，登录 player 即可开局通关。

## 冒烟验证

```bash
# 登录
TOKEN=$(curl -s -X POST localhost:8080/api/user/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"player","password":"123456"}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
# 开局
curl -s -X POST localhost:8080/api/game/sessions -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"scenarioId":1}'
# 内部服务隔离（应 404）
curl -s -o /dev/null -w '%{http_code}\n' -X POST localhost:8080/api/ai/generate
```

## 测试

```bash
mvn -pl common test          # ConditionEvaluator 受控小语法 7 项
mvn -pl game-service test    # TransitionWhitelist 防跑偏 5 项（含越界拒绝=1503）
```

## 文档

- `AI跑团GM·微服务毕设-实现规格书(AI可执行).md` — 施工蓝图（DDL/接口/AI 契约/种子 SQL，SSOT）
- `AI跑团GM·微服务毕设-架构设计.md` — 架构动机/时序/权衡
- `CONTEXT.md` + `docs/adr/` — 领域语言与关键决策
