#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${ROOT_DIR}/local/.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  ENV_FILE="${ROOT_DIR}/local/.env.example"
fi

docker compose \
  --env-file "${ENV_FILE}" \
  -f "${ROOT_DIR}/local/docker-compose.yml" \
  config >/dev/null

kubectl kustomize "${ROOT_DIR}/sample-app/manifests/base" >/dev/null
kubectl kustomize "${ROOT_DIR}/sample-app/manifests/overlays/payment-readiness-failure" >/dev/null
kubectl kustomize "${ROOT_DIR}/sample-app/manifests/overlays/consumer-lag" >/dev/null
kubectl kustomize "${ROOT_DIR}/sample-app/manifests/overlays/worker-crashloop" >/dev/null
kubectl kustomize "${ROOT_DIR}/local/prometheus" >/dev/null

echo "Local Compose and Kubernetes manifests are valid."
