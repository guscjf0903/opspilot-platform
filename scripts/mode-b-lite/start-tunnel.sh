#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

TF_DIR="${TF_DIR:-${ROOT_DIR}/infra/terraform/envs/aws-dev-ephemeral}"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
INSTANCE_ID="${INSTANCE_ID:-}"
REMOTE_PORT="${REMOTE_PORT:-8080}"
LOCAL_PORT="${LOCAL_PORT:-8080}"

if [[ -z "${INSTANCE_ID}" ]]; then
  INSTANCE_ID="$(terraform -chdir="${TF_DIR}" output -raw k3s_lab_instance_id 2>/dev/null || true)"
fi

if [[ -z "${INSTANCE_ID}" ]]; then
  echo "Mode B Lite instance id is empty." >&2
  echo "Run terraform apply with enable_k3s_lab=true first, or set INSTANCE_ID manually." >&2
  exit 1
fi

AWS_ARGS=(--region "${AWS_REGION}")
if [[ -n "${AWS_PROFILE:-}" ]]; then
  AWS_ARGS+=(--profile "${AWS_PROFILE}")
fi

echo "Starting SSM port forwarding session."
echo "Local:  http://127.0.0.1:${LOCAL_PORT}"
echo "Remote: ${INSTANCE_ID}:${REMOTE_PORT}"
echo "Keep this terminal open while viewing OpsPilot."

aws "${AWS_ARGS[@]}" ssm start-session \
  --target "${INSTANCE_ID}" \
  --document-name AWS-StartPortForwardingSession \
  --parameters "{\"portNumber\":[\"${REMOTE_PORT}\"],\"localPortNumber\":[\"${LOCAL_PORT}\"]}"
