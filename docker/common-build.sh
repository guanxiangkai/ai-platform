#!/usr/bin/env bash
set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

build_service() {
  local service="$1"
  local task="$2"
  if [[ "${SKIP_GRADLE:-false}" != "true" ]]; then
    (cd "${PROJECT_ROOT}" && ./gradlew "${task}")
  fi
  (cd "${PROJECT_ROOT}" && docker compose -f docker/docker-compose.yml build "${service}")
}
