#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PNPM_VERSION="10.34.1"

cd "${ROOT_DIR}/frontend"

if corepack pnpm --version >/dev/null 2>&1; then
  PNPM=(corepack pnpm)
else
  PNPM=(npx --yes "pnpm@${PNPM_VERSION}")
fi

"${PNPM[@]}" install --frozen-lockfile
exec "${PNPM[@]}" dev
