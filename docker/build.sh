#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common-build.sh"

declare -A tasks=(
  [platform-agent]=:platform-modules:platform-agent:bootJar
  [platform-system]=:platform-modules:platform-system:bootJar
  [platform-files]=:platform-modules:platform-files:bootJar
  [platform-sse]=:platform-modules:platform-sse:bootJar
  [platform-scheduler]=:platform-modules:platform-scheduler:bootJar
  [platform-auth]=:platform-auth:bootJar
  [platform-gateway]=:platform-gateway:bootJar
)
if [[ "$#" -eq 0 ]]; then
  services=("${!tasks[@]}")
else
  services=("$@")
fi
for service in "${services[@]}"; do
  [[ -n "${tasks[${service}]:-}" ]] || { echo "未知平台服务：${service}" >&2; exit 2; }
  build_service "${service}" "${tasks[${service}]}"
done
