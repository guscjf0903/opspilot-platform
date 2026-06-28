#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${ROOT_DIR}/local/.env"
COMPOSE_FILE="${ROOT_DIR}/local/docker-compose.yml"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing local/.env. Run: cp local/.env.example local/.env" >&2
  exit 1
fi

docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up -d
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" ps
