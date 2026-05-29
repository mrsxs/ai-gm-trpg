-- aigm_memory 降级版：远端 PG 未安装 pgvector 扩展时使用。
-- embedding 存为 real[]（float4 数组，维度 1024），余弦相似度由 memory-service 应用层计算。
-- 种子规模（约 10-20 回合/对局）下应用层暴力余弦完全够用；装上 pgvector 后可平滑切回 01-schema-memory.sql。
CREATE TABLE IF NOT EXISTS t_memory (
  id          BIGSERIAL    PRIMARY KEY,
  session_id  BIGINT       NOT NULL,
  content     TEXT         NOT NULL,
  embedding   REAL[]       NOT NULL,
  mem_type    VARCHAR(20)  NOT NULL DEFAULT 'EVENT',
  importance  SMALLINT     NOT NULL DEFAULT 3,
  created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_memory_session ON t_memory (session_id);
