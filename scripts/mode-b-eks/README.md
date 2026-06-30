# Mode B EKS scripts

이 디렉토리는 이미 생성된 `aws-dev-ephemeral` EKS cluster 위에 OpsPilot을 배포하고 확인하는
로컬 실행 스크립트를 둔다.

사전 조건:

```bash
aws eks update-kubeconfig \
  --profile opspilot-lab \
  --region ap-northeast-2 \
  --name opspilot-aws-dev-eks \
  --alias opspilot-aws-dev-eks
```

배포:

```bash
AWS_PROFILE=opspilot-lab IMAGE_TAG=<commit-sha> ./scripts/mode-b-eks/deploy.sh
```

Prometheus/OpenCost까지 함께 배포:

```bash
AWS_PROFILE=opspilot-lab ./scripts/mode-b-eks/deploy-observability.sh
AWS_PROFILE=opspilot-lab IMAGE_TAG=<commit-sha> ./scripts/mode-b-eks/deploy.sh
```

기본 관측 배포는 `t3.medium` 단일 node의 pod 한도를 고려해 node-exporter를 배포하지 않는다.
node-exporter까지 실험하려면 아래처럼 명시적으로 켠다.

```bash
AWS_PROFILE=opspilot-lab ENABLE_NODE_EXPORTER=true ./scripts/mode-b-eks/deploy-observability.sh
```

Kafka/Redpanda demo는 기본값에서 꺼져 있다. Mode B EKS의 현재 목표는 EKS 위에서
OpsPilot, sample-app, Prometheus/OpenCost, 비용 화면을 안정적으로 검증하는 것이다.

또는 한 번에 실행:

```bash
AWS_PROFILE=opspilot-lab DEPLOY_OBSERVABILITY=true IMAGE_TAG=<commit-sha> ./scripts/mode-b-eks/deploy.sh
```

상태 확인:

```bash
./scripts/mode-b-eks/verify-observability.sh
./scripts/mode-b-eks/verify.sh
./scripts/mode-b-eks/helm-test.sh
```

브라우저 접속:

```bash
./scripts/mode-b-eks/port-forward.sh
```

접속 URL:

```text
http://127.0.0.1:8080
```

기본값은 ALB, RDS, MSK, NAT Gateway를 사용하지 않는다. 외부 URL 공개는 Phase 4에서
별도로 다룬다.
