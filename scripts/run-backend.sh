#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/local/.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing local/.env. Run: cp local/.env.example local/.env" >&2
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

exec "${ROOT_DIR}/backend/gradlew" -p "${ROOT_DIR}/backend" bootRun \
  --args='--spring.profiles.active=local'
