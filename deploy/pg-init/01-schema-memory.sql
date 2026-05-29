-- aigm_memory（基线 §4.4）：pgvector 版本。需 PG 安装 pgvector 扩展。
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS t_memory (
  id          BIGSERIAL    PRIMARY KEY,
  session_id  BIGINT       NOT NULL,
  content     TEXT         NOT NULL,
  embedding   vector(1024) NOT NULL,
  mem_type    VARCHAR(20)  NOT NULL DEFAULT 'EVENT',
  importance  SMALLINT     NOT NULL DEFAULT 3,
  created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_memory_session ON t_memory (session_id);
CREATE INDEX IF NOT EXISTS idx_memory_embedding ON t_memory USING hnsw (embedding vector_cosine_ops);
