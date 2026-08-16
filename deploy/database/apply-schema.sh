#!/usr/bin/env bash
set -euo pipefail

required_vars=(
  PLATFORM_DB_HOST PLATFORM_DB_PORT PLATFORM_DB_NAME PLATFORM_DB_USERNAME PLATFORM_DB_PASSWORD
)
for variable in "${required_vars[@]}"; do
  if [[ -z "${!variable:-}" ]]; then
    echo "缺少必要环境变量：${variable}" >&2
    exit 2
  fi
done

container_runtime="${CONTAINER_RUNTIME:-docker}"
if [[ "${container_runtime}" != "docker" && "${container_runtime}" != "podman" ]]; then
  echo "CONTAINER_RUNTIME 只允许 docker 或 podman" >&2
  exit 2
fi

flyway_image="${FLYWAY_IMAGE:?FLYWAY_IMAGE is required}"
if [[ "${flyway_image}" == *:latest || ( "${flyway_image}" != *@sha256:* && "${flyway_image}" != *:* ) ]]; then
  echo "FLYWAY_IMAGE 必须使用精确版本标签或镜像摘要，禁止 latest，当前为：${flyway_image}" >&2
  exit 2
fi

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
runtime_dir="$(mktemp -d)"
chmod 700 "${runtime_dir}"
flyway_config="${runtime_dir}/flyway.conf"
trap 'command rm -rf -- "${runtime_dir}"' EXIT

{
  printf 'flyway.url=jdbc:postgresql://%s:%s/%s\n' "${PLATFORM_DB_HOST}" "${PLATFORM_DB_PORT}" "${PLATFORM_DB_NAME}"
  printf 'flyway.user=%s\n' "${PLATFORM_DB_USERNAME}"
  printf 'flyway.password=%s\n' "${PLATFORM_DB_PASSWORD}"
  printf '%s\n' \
    'flyway.schemas=public' \
    'flyway.defaultSchema=public' \
    'flyway.locations=filesystem:/flyway/sql' \
    'flyway.cleanDisabled=true' \
    'flyway.validateMigrationNaming=true' \
    'flyway.validateOnMigrate=true' \
    'flyway.baselineOnMigrate=false' \
    'flyway.connectRetries=3'
} > "${flyway_config}"
chmod 600 "${flyway_config}"

"${container_runtime}" pull "${flyway_image}"
"${container_runtime}" run --rm \
  --network host \
  --mount "type=bind,src=${script_dir},dst=/flyway/sql,readonly" \
  --mount "type=bind,src=${flyway_config},dst=/flyway/conf/flyway.conf,readonly" \
  "${flyway_image}" \
  -configFiles=/flyway/conf/flyway.conf migrate
