# 샘플 앱

`sample-app` namespace에는 OpsPilot 데모를 위한 작은 workload가 있습니다.

| Workload | 목적 |
| --- | --- |
| `payment-api` | 정상 readiness와 readiness 실패 시나리오 |
| `catalog-api` | 높은 CPU request를 가진 비용 추천 후보 |
| `worker` | 정상 상태와 CrashLoopBackOff 시나리오 |
| `order-producer` | `orders.created` topic에 데모 메시지 생성 |
| `order-consumer` | `order-consumer` group으로 메시지 소비 |
| `unused-demo-data` | mount되지 않은 PVC 후보 |

기본 앱을 배포합니다.

```bash
./scripts/local-kind/deploy-sample-app.sh
```

장애를 재현할 때는 원하는 overlay를 적용합니다.

```bash
kubectl --context kind-opspilot-demo apply -k sample-app/manifests/overlays/payment-readiness-failure
kubectl --context kind-opspilot-demo apply -k sample-app/manifests/overlays/consumer-lag
kubectl --context kind-opspilot-demo apply -k sample-app/manifests/overlays/worker-crashloop
```

정상 상태로 되돌릴 때는 base를 다시 적용합니다.

```bash
kubectl --context kind-opspilot-demo apply -k sample-app/manifests/base
```
