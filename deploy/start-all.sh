#!/usr/bin/env bash
# 本地启动全部后端服务（连中间件(默认本地 localhost，可用 .env 环境变量覆盖)）。先 mvn package 出 jar。
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOGDIR="${LOGDIR:-/tmp}"
# 若根目录有 .env（gitignore，存放密钥）则自动加载
if [ -f "$ROOT/.env" ]; then set -a; . "$ROOT/.env"; set +a; fi
# LLM/Embedding 配置可选：提供 LLM_API_KEY 即走真实 LLM，否则确定性 stub/伪向量兜底
: "${LLM_API_KEY:=}"; export LLM_API_KEY
: "${LLM_BASE_URL:=}"; export LLM_BASE_URL
: "${LLM_MODEL:=}"; export LLM_MODEL
: "${EMBEDDING_API_KEY:=}"; export EMBEDDING_API_KEY
: "${NACOS_PASSWORD:=}"; export NACOS_PASSWORD

start() {
  local name=$1
  echo "starting $name ..."
  nohup java -jar "$ROOT/$name/target/$name-1.0.0.jar" > "$LOGDIR/aigm-$name.log" 2>&1 &
}

# 无状态/数据服务先起，gateway 与 game 随后
start ai-engine-service
start memory-service
start scenario-service
start user-service
sleep 8
start game-service
start gateway
echo "全部启动中，日志在 $LOGDIR/aigm-*.log"
