#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

IMAGE_TAG="${1:-${IMAGE_TAG:-}}"
if [[ -z "${IMAGE_TAG}" ]]; then
  echo "Usage: IMAGE_TAG=<commit-sha> $0 [instance-id]" >&2
  echo "   or: $0 <commit-sha> [instance-id]" >&2
  exit 1
fi

AWS_REGION="${AWS_REGION:-ap-northeast-2}"
TF_DIR="${TF_DIR:-${ROOT_DIR}/infra/terraform/envs/aws-dev-ephemeral}"
INSTANCE_ID="${2:-${INSTANCE_ID:-}}"
REPO_URL="${REPO_URL:-https://github.com/guscjf0903/opspilot-platform.git}"
SOURCE_REF="${SOURCE_REF:-${IMAGE_TAG}}"
REMOTE_REPO_DIR="${REMOTE_REPO_DIR:-/opt/opspilot-platform}"
POLL_INTERVAL_SECONDS="${POLL_INTERVAL_SECONDS:-10}"
SSM_WAIT_ATTEMPTS="${SSM_WAIT_ATTEMPTS:-30}"
NAMESPACE="${NAMESPACE:-opspilot}"
SAMPLE_NAMESPACE="${SAMPLE_NAMESPACE:-sample-app}"
RELEASE_NAME="${RELEASE_NAME:-opspilot}"
HELM_FULLNAME="${HELM_FULLNAME:-opspilot}"
BACKEND_SECRET_NAME="${BACKEND_SECRET_NAME:-opspilot-backend-secret}"
ECR_PULL_SECRET_NAME="${ECR_PULL_SECRET_NAME:-ecr-registry}"
WAIT_DEMO_WORKLOADS="${WAIT_DEMO_WORKLOADS:-true}"
FRONTEND_FORWARD_HOST="${FRONTEND_FORWARD_HOST:-127.0.0.1}"
FRONTEND_FORWARD_PORT="${FRONTEND_FORWARD_PORT:-8080}"
FRONTEND_SERVICE_PORT="${FRONTEND_SERVICE_PORT:-8080}"

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

echo "Waiting for SSM managed instance ${INSTANCE_ID} to be Online..."
for ((attempt = 1; attempt <= SSM_WAIT_ATTEMPTS; attempt++)); do
  PING_STATUS="$(aws "${AWS_ARGS[@]}" ssm describe-instance-information \
    --filters "Key=InstanceIds,Values=${INSTANCE_ID}" \
    --query "InstanceInformationList[0].PingStatus" \
    --output text 2>/dev/null || true)"

  if [[ "${PING_STATUS}" == "Online" ]]; then
    break
  fi

  if [[ "${attempt}" == "${SSM_WAIT_ATTEMPTS}" ]]; then
    echo "SSM instance ${INSTANCE_ID} did not become Online. Last status: ${PING_STATUS}" >&2
    exit 1
  fi

  sleep "${POLL_INTERVAL_SECONDS}"
done

REMOTE_COMMAND=$(cat <<REMOTE
set -euo pipefail

export AWS_REGION="${AWS_REGION}"
export IMAGE_TAG="${IMAGE_TAG}"
export SOURCE_REF="${SOURCE_REF}"
export REPO_URL="${REPO_URL}"
export REMOTE_REPO_DIR="${REMOTE_REPO_DIR}"
export NAMESPACE="${NAMESPACE}"
export SAMPLE_NAMESPACE="${SAMPLE_NAMESPACE}"
export RELEASE_NAME="${RELEASE_NAME}"
export HELM_FULLNAME="${HELM_FULLNAME}"
export BACKEND_SECRET_NAME="${BACKEND_SECRET_NAME}"
export ECR_PULL_SECRET_NAME="${ECR_PULL_SECRET_NAME}"
export WAIT_DEMO_WORKLOADS="${WAIT_DEMO_WORKLOADS}"
export FRONTEND_FORWARD_HOST="${FRONTEND_FORWARD_HOST}"
export FRONTEND_FORWARD_PORT="${FRONTEND_FORWARD_PORT}"
export FRONTEND_SERVICE_PORT="${FRONTEND_SERVICE_PORT}"
export HOME=/root
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml

for attempt in \$(seq 1 60); do
  if systemctl is-active --quiet k3s && kubectl get nodes >/dev/null 2>&1; then
    break
  fi
  if [ "\${attempt}" = "60" ]; then
    systemctl status k3s --no-pager || true
    exit 1
  fi
  sleep 10
done

kubectl get nodes

if [ ! -d "\${REMOTE_REPO_DIR}/.git" ]; then
  rm -rf "\${REMOTE_REPO_DIR}"
  git clone "\${REPO_URL}" "\${REMOTE_REPO_DIR}"
fi

cd "\${REMOTE_REPO_DIR}"
git fetch --all --tags
git checkout "\${SOURCE_REF}"

kubectl create namespace "\${NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -

if ! kubectl -n "\${NAMESPACE}" get secret "\${BACKEND_SECRET_NAME}" >/dev/null 2>&1; then
  DB_PASSWORD="\${DB_PASSWORD:-\$(openssl rand -base64 24 | tr -d '\n')}"
  kubectl -n "\${NAMESPACE}" create secret generic "\${BACKEND_SECRET_NAME}" \
    --from-literal=db-password="\${DB_PASSWORD}" \
    --from-literal=openai-api-key="\${OPENAI_API_KEY:-}"
fi

ACCOUNT_ID="\$(aws sts get-caller-identity --query Account --output text)"
ECR_REGISTRY="\${ACCOUNT_ID}.dkr.ecr.\${AWS_REGION}.amazonaws.com"
ECR_PASSWORD="\$(aws ecr get-login-password --region "\${AWS_REGION}")"
BACKEND_IMAGE_REPOSITORY="\${BACKEND_IMAGE_REPOSITORY:-\${ECR_REGISTRY}/opspilot-backend}"
FRONTEND_IMAGE_REPOSITORY="\${FRONTEND_IMAGE_REPOSITORY:-\${ECR_REGISTRY}/opspilot-frontend}"

kubectl -n "\${NAMESPACE}" create secret docker-registry "\${ECR_PULL_SECRET_NAME}" \
  --docker-server="https://\${ECR_REGISTRY}" \
  --docker-username=AWS \
  --docker-password="\${ECR_PASSWORD}" \
  --dry-run=client \
  -o yaml | kubectl apply -f -

helm upgrade --install "\${RELEASE_NAME}" "\${REMOTE_REPO_DIR}/deploy/helm/opspilot" \
  -n "\${NAMESPACE}" \
  --create-namespace \
  -f "\${REMOTE_REPO_DIR}/deploy/helm/opspilot/values-aws-dev-ephemeral.yaml" \
  --set "backend.image.repository=\${BACKEND_IMAGE_REPOSITORY}" \
  --set "backend.image.tag=\${IMAGE_TAG}" \
  --set "frontend.image.repository=\${FRONTEND_IMAGE_REPOSITORY}" \
  --set "frontend.image.tag=\${IMAGE_TAG}" \
  --set "imagePullSecrets[0].name=\${ECR_PULL_SECRET_NAME}" \
  --set "demoPostgresql.enabled=true"

kubectl -n "\${NAMESPACE}" rollout status deploy/"\${HELM_FULLNAME}-backend" --timeout=180s
kubectl -n "\${NAMESPACE}" rollout status deploy/"\${HELM_FULLNAME}-frontend" --timeout=180s

if [ "\${WAIT_DEMO_WORKLOADS}" = "true" ]; then
  for deployment in redpanda order-producer order-consumer; do
    if kubectl -n "\${SAMPLE_NAMESPACE}" get deploy/"\${deployment}" >/dev/null 2>&1; then
      kubectl -n "\${SAMPLE_NAMESPACE}" rollout status deploy/"\${deployment}" --timeout=240s
    fi
  done
fi

PORT_FORWARD_LOG="/tmp/opspilot-\${NAMESPACE}-frontend-port-forward.log"
pkill -f "kubectl.*port-forward.*svc/\${HELM_FULLNAME}-frontend" >/dev/null 2>&1 || true
nohup kubectl -n "\${NAMESPACE}" port-forward \
  --address "\${FRONTEND_FORWARD_HOST}" \
  "svc/\${HELM_FULLNAME}-frontend" \
  "\${FRONTEND_FORWARD_PORT}:\${FRONTEND_SERVICE_PORT}" \
  >"\${PORT_FORWARD_LOG}" 2>&1 &

sleep 2

kubectl -n opspilot get pods
kubectl -n sample-app get pods
tail -n 50 /tmp/opspilot-opspilot-frontend-port-forward.log || true
REMOTE
)

echo "Sending Mode B Lite deploy command to ${INSTANCE_ID}..."
COMMAND_ID="$(aws "${AWS_ARGS[@]}" ssm send-command \
  --instance-ids "${INSTANCE_ID}" \
  --document-name "AWS-RunShellScript" \
  --comment "Deploy OpsPilot Mode B Lite ${IMAGE_TAG}" \
  --parameters commands="${REMOTE_COMMAND}" \
  --query "Command.CommandId" \
  --output text)"

echo "SSM command id: ${COMMAND_ID}"
echo "Waiting for remote deployment to finish..."

while true; do
  STATUS="$(aws "${AWS_ARGS[@]}" ssm get-command-invocation \
    --command-id "${COMMAND_ID}" \
    --instance-id "${INSTANCE_ID}" \
    --query "Status" \
    --output text 2>/dev/null || true)"

  case "${STATUS}" in
    Success|Cancelled|TimedOut|Failed|Cancelling)
      break
      ;;
    *)
      sleep "${POLL_INTERVAL_SECONDS}"
      ;;
  esac
done

echo "Remote deployment status: ${STATUS}"
echo
echo "----- remote stdout -----"
aws "${AWS_ARGS[@]}" ssm get-command-invocation \
  --command-id "${COMMAND_ID}" \
  --instance-id "${INSTANCE_ID}" \
  --query "StandardOutputContent" \
  --output text || true

if [[ "${STATUS}" != "Success" ]]; then
  echo
  echo "----- remote stderr -----" >&2
  aws "${AWS_ARGS[@]}" ssm get-command-invocation \
    --command-id "${COMMAND_ID}" \
    --instance-id "${INSTANCE_ID}" \
    --query "StandardErrorContent" \
    --output text >&2 || true
  exit 1
fi

echo
echo "Deployment completed. Open a tunnel from your laptop with:"
echo "AWS_PROFILE=${AWS_PROFILE:-opspilot-lab} ./scripts/mode-b-lite/start-tunnel.sh"
