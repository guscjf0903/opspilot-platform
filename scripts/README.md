# OpsPilot Scripts

이 디렉토리는 OpsPilot 실행 진입점을 모드별로 정리한다. 루트의 `*.sh` 파일은 기존 문서와
자동화 호환을 위한 wrapper이며, 새로 사용할 때는 하위 폴더의 canonical script를 우선한다.

## 모드별 진입점

| 모드 | 상황 | 스크립트 |
| --- | --- | --- |
| Local Dev | PostgreSQL, Redpanda, backend, frontend 로컬 개발 | `local-dev/start-dependencies.sh`, `local-dev/run-backend.sh`, `local-dev/run-frontend.sh` |
| Local kind | kind cluster와 sample-app, Prometheus, OpenCost 검증 | `local-kind/create-cluster.sh`, `local-kind/deploy-sample-app.sh`, `local-kind/deploy-prometheus.sh`, `local-kind/deploy-opencost.sh` |
| Mode A | GitHub Pages용 정적 포트폴리오와 fixture demo artifact 생성 | `mode-a-pages/build.sh` |
| Mode B Lite | EC2+k3s live demo 원격 배포와 SSM tunnel | `mode-b-lite/deploy-remote.sh`, `mode-b-lite/start-tunnel.sh` |

## Local Dev

```bash
cp local/.env.example local/.env
./scripts/local-dev/start-dependencies.sh
./scripts/local-dev/run-backend.sh
./scripts/local-dev/run-frontend.sh
```

검증:

```bash
./scripts/local-dev/verify.sh
```

## Local kind

```bash
./scripts/local-kind/create-cluster.sh
./scripts/local-kind/deploy-sample-app.sh
./scripts/local-kind/deploy-prometheus.sh
./scripts/local-kind/deploy-opencost.sh
```

port-forward:

```bash
./scripts/local-kind/port-forward-prometheus.sh
./scripts/local-kind/port-forward-opencost.sh
```

## Mode A

```bash
./scripts/mode-a-pages/build.sh
python3 -m http.server 4173 -d .mode-a-pages
```

Mode A는 실제 backend 없이 `frontend/src/demo` fixture adapter로 동작한다.

## Mode B Lite

로컬에서 EC2 k3s lab에 원격 배포:

```bash
AWS_PROFILE=opspilot-lab IMAGE_TAG=<commit-sha> ./scripts/mode-b-lite/deploy-remote.sh
```

`deploy-remote.sh`는 AWS Systems Manager Run Command로 EC2 안에서 repo checkout,
Helm 배포, rollout 확인, frontend port-forward 시작까지 실행한다. Session Manager
Plugin 없이도 배포할 수 있지만, 브라우저 접속용 port forwarding tunnel에는 plugin이 필요하다.

EC2 k3s lab 안에서 직접 배포해야 할 때:

```bash
IMAGE_TAG=<commit-sha> ./scripts/mode-b-lite/deploy-k3s.sh
```

노트북에서 SSM tunnel:

```bash
AWS_PROFILE=opspilot-lab ./scripts/mode-b-lite/start-tunnel.sh
```

Mode B Lite는 비용이 발생할 수 있는 EC2 lab을 사용한다. Terraform `apply`와 `destroy`는
실행 전 비용과 대상 AWS profile을 확인한다.

## 호환 wrapper

기존 경로는 계속 사용할 수 있다.

| 기존 경로 | canonical 경로 |
| --- | --- |
| `start-local-dependencies.sh` | `local-dev/start-dependencies.sh` |
| `run-backend.sh` | `local-dev/run-backend.sh` |
| `run-frontend.sh` | `local-dev/run-frontend.sh` |
| `verify-local.sh` | `local-dev/verify.sh` |
| `create-kind-cluster.sh` | `local-kind/create-cluster.sh` |
| `deploy-sample-app.sh` | `local-kind/deploy-sample-app.sh` |
| `deploy-local-prometheus.sh` | `local-kind/deploy-prometheus.sh` |
| `deploy-local-opencost.sh` | `local-kind/deploy-opencost.sh` |
| `port-forward-prometheus.sh` | `local-kind/port-forward-prometheus.sh` |
| `port-forward-opencost.sh` | `local-kind/port-forward-opencost.sh` |
| `build-mode-a-pages.sh` | `mode-a-pages/build.sh` |
| `deploy-mode-b-lite-k3s.sh` | `mode-b-lite/deploy-k3s.sh` |
| `start-mode-b-lite-tunnel.sh` | `mode-b-lite/start-tunnel.sh` |
