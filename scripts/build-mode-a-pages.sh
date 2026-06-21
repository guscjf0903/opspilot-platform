#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="${ROOT_DIR}/frontend"
ARTIFACT_DIR="${MODE_A_ARTIFACT_DIR:-${ROOT_DIR}/.mode-a-pages}"

if command -v pnpm >/dev/null 2>&1; then
  BUILD_CMD=(pnpm build)
elif [[ -d "${FRONTEND_DIR}/node_modules" ]] && command -v npm >/dev/null 2>&1; then
  BUILD_CMD=(npm run build)
elif command -v corepack >/dev/null 2>&1; then
  BUILD_CMD=(corepack pnpm build)
else
  echo "pnpm, corepack, or npm with existing frontend/node_modules is required to build the Mode A demo app." >&2
  exit 1
fi

rm -rf "${ARTIFACT_DIR}"
mkdir -p "${ARTIFACT_DIR}/demo"

(
  cd "${FRONTEND_DIR}"
  VITE_DEMO_MODE=true \
    VITE_APP_ENV=pages-demo \
    VITE_PUBLIC_BASE_PATH=./ \
    VITE_ROUTER_HISTORY=hash \
    "${BUILD_CMD[@]}"
)

cp -R "${ROOT_DIR}/portfolio/." "${ARTIFACT_DIR}/"
cp -R "${FRONTEND_DIR}/dist/." "${ARTIFACT_DIR}/demo/"

echo "Mode A Pages artifact assembled at ${ARTIFACT_DIR}"
echo "Preview with: python3 -m http.server 4173 -d ${ARTIFACT_DIR}"
