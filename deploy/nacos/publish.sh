#!/usr/bin/env bash
# 发布 7 个 dataId 到远端 Nacos（鉴权开启）。重复执行幂等覆盖。
set -euo pipefail
NACOS="${NACOS_ADDR:-http://localhost:8848}"
NACOS_USER="${NACOS_USER:-nacos}"
NACOS_PASS="${NACOS_PASS:-nacos}"
DIR="$(cd "$(dirname "$0")" && pwd)"

TOKEN=$(curl -s -m 8 -X POST "$NACOS/nacos/v1/auth/login" \
  -d "username=$NACOS_USER&password=$NACOS_PASS" \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
[ -n "$TOKEN" ] || { echo "Nacos 登录失败"; exit 1; }

for dataId in aigm-common user-service scenario-service game-service ai-engine-service memory-service gateway; do
  resp=$(curl -s -m 10 -X POST "$NACOS/nacos/v1/cs/configs?accessToken=$TOKEN" \
    --data-urlencode "dataId=${dataId}.yaml" \
    --data-urlencode "group=DEFAULT_GROUP" \
    --data-urlencode "type=yaml" \
    --data-urlencode "content@$DIR/${dataId}.yaml")
  echo "publish ${dataId}.yaml -> $resp"
done
