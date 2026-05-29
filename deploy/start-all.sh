#!/usr/bin/env bash
# 本地启动全部后端服务（连远端中间件 123.57.166.60）。先 mvn package 出 jar。
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOGDIR="${LOGDIR:-/tmp}"
# LLM/Embedding key 可选：导出后真实调用，否则走确定性 stub/伪向量兜底
: "${LLM_API_KEY:=}"; export LLM_API_KEY
: "${EMBEDDING_API_KEY:=}"; export EMBEDDING_API_KEY

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
