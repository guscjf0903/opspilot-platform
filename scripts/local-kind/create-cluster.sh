#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CLUSTER_NAME="opspilot-demo"

if ! command -v kind >/dev/null 2>&1; then
  echo "kind is required. Install it before creating the demo cluster." >&2
  exit 1
fi

if kind get clusters | grep -qx "${CLUSTER_NAME}"; then
  echo "kind cluster ${CLUSTER_NAME} already exists."
  exit 0
fi

kind create cluster \
  --name "${CLUSTER_NAME}" \
  --config "${ROOT_DIR}/local/kind/cluster.yml"
