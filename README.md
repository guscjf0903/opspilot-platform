# OpsPilot

OpsPilot은 Kubernetes, AWS, Kafka 리소스를 한 화면에서 관측하고, 객체 관계도와
근거 기반 AI 분석으로 장애 원인과 비용 낭비 지점을 빠르게 찾는 CloudOps 포트폴리오
프로젝트입니다.

이 저장소는 프론트엔드, 백엔드, 로컬 데모, Kubernetes sample app, Terraform/Helm
초안, GitHub Pages 공개 데모를 함께 관리하는 monorepo입니다.

## 주요 기능

- Kubernetes namespace, deployment, pod, node, event 상태 모니터링
- Deployment, ReplicaSet, Pod, Service, EndpointSlice, PVC, Node 관계도 시각화
- Kafka topic, consumer group, lag 관측과 workload 연결
- metric, event, rollout, Kafka lag 기반 AI incident 분석 화면
- workload별 비용 추정과 절감 추천 화면
- dry-run, diff, 승인, audit log 기반 안전한 운영 조치 흐름
- GitHub Pages에서 동작하는 읽기 전용 정적 데모

## Monorepo 구조

| 경로 | 역할 |
| --- | --- |
| `backend/` | Java 21, Spring Boot 3.5 기반 API |
| `frontend/` | Vue 3, TypeScript, Vite 기반 Web |
| `local/` | PostgreSQL, Redpanda 로컬 실행 설정 |
| `sample-app/` | Kubernetes 장애 재현용 sample app |
| `infra/` | 저비용 AWS Terraform 구성 |
| `deploy/` | Helm 배포 구성 초안 |
| `portfolio/` | GitHub Pages landing page |
| `.github/workflows/` | GitHub Actions workflow |
| `scripts/` | 로컬 실행과 검증 스크립트 |

개인 기획 문서와 상세 설계 메모는 공개 저장소에 포함하지 않습니다.

## 공개 데모

GitHub Pages는 `portfolio/` landing page와 `frontend` Vue 앱을 함께 배포합니다.
배포 후 구조는 아래와 같습니다.

```text
/
  index.html
  assets/
  demo/
    index.html
    assets/
```

`/demo/` Vue 앱은 `VITE_DEMO_MODE=true`로 빌드되며 실제 backend 없이 snapshot
fixture 데이터로 Dashboard, Workloads, Topology, Kafka, Cost, Actions, AI 분석 화면을
보여줍니다. 공개 방문자는 EKS, ALB, RDS, MSK가 꺼져 있어도 핵심 화면을 확인할 수
있고, 실제 Kubernetes 리소스 변경은 발생하지 않습니다.

## GitHub Actions

| Workflow | 역할 |
| --- | --- |
| `Publish portfolio` | `portfolio/`와 demo mode Vue 앱을 GitHub Pages로 배포 |
| `Infra check` | Terraform fmt, init, validate 검증 |

`Publish portfolio`는 `main` branch에 `portfolio/**`, `frontend/**`, `.nvmrc`,
workflow 파일 변경이 push될 때 실행됩니다. GitHub repository Settings에서 Pages
source를 `GitHub Actions`로 설정해야 공개 URL이 생성됩니다.

`Infra check`는 PR 또는 수동 실행으로 Terraform 문법을 검증합니다. `terraform apply`
단계가 없으므로 AWS 리소스를 만들지 않습니다.

## 사전 준비

- Java 21
- Node.js 24
- pnpm 10
- Docker Desktop
- `kubectl`
- kind

AWS, Terraform, Helm은 실제 클라우드 데모 단계에서만 필요합니다.

## 로컬 실행

로컬 환경 변수를 준비하고 PostgreSQL, Redpanda를 실행합니다.

```bash
cp local/.env.example local/.env
./scripts/start-local-dependencies.sh
```

별도 터미널에서 API와 Web을 실행합니다.

```bash
./scripts/run-backend.sh
./scripts/run-frontend.sh
```

- Web: <http://localhost:5173>
- API health: <http://localhost:8080/actuator/health>
- Redpanda Kafka API: `localhost:9092`
- PostgreSQL: `localhost:5432`

## kind 데모 앱

kind가 설치된 환경에서는 sample namespace와 장애 재현용 앱을 배포할 수 있습니다.

```bash
./scripts/create-kind-cluster.sh
./scripts/deploy-sample-app.sh
kubectl get pods -n sample-app
```

장애 시나리오는 [sample-app/README.md](./sample-app/README.md)를 참고합니다.

## 검증

전체 로컬 검증:

```bash
./scripts/verify-local.sh
```

Backend:

```bash
cd backend
./gradlew test
```

Frontend:

```bash
cd frontend
pnpm install --frozen-lockfile
pnpm test
pnpm build
```

GitHub Pages demo build:

```bash
cd frontend
VITE_DEMO_MODE=true \
VITE_APP_ENV=pages-demo \
VITE_PUBLIC_BASE_PATH=./ \
VITE_ROUTER_HISTORY=hash \
pnpm build
```

## 보안 원칙

- `.env`, `.env.*`, Terraform state, `*.tfvars`, `*.tfplan`, kubeconfig, AWS credential은
  commit하지 않습니다.
- 공개 저장소에는 `.env.example`, `terraform.tfvars.example`처럼 값이 비어 있거나
  placeholder인 예시 파일만 포함합니다.
- GitHub Pages demo는 snapshot fixture만 사용하며 live AWS endpoint를 상시 연결하지
  않습니다.
- 실제 AWS 리소스 생성은 비용 알림과 예상 종료 시간을 정한 뒤 짧게 실행합니다.
