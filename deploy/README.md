# Deployment

OpsPilot 배포는 Helm을 기준으로 정리합니다. 로컬 데모 앱은 기존처럼
`sample-app/manifests`의 Kustomize 리소스를 사용할 수 있고, AWS 데모 단계에서는
Helm chart로 OpsPilot과 sample-app을 재현 가능하게 배포합니다.

공개 저장소에서는 기본값을 저비용, read-only, public ingress 비활성으로 유지합니다.

## 원칙

- public ingress는 기본 비활성이다.
- 외부 방문자용 live demo는 read-only로 제한한다.
- Secret 값은 Helm values에 직접 넣지 않는다.
- resource request/limit은 포트폴리오 데모 규모에 맞게 작게 시작한다.
- Prometheus, OpenCost, sample-app은 데모 목적에 맞게 작은 retention과 replica를 사용한다.

## 예정 구조

```text
deploy/
  helm/
    opspilot/
      Chart.yaml
      values.yaml
      values-local-kind.yaml
      values-aws-dev-ephemeral.yaml
      templates/
    sample-app/
      Chart.yaml
      values.yaml
      values-aws-dev-ephemeral.yaml
      templates/
```

## 검증 명령

```bash
helm lint deploy/helm/opspilot
helm template opspilot deploy/helm/opspilot -f deploy/helm/opspilot/values-aws-dev-ephemeral.yaml
kubeconform
```
