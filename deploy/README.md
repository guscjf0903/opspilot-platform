# 배포

OpsPilot 배포는 Helm을 기준으로 정리합니다. 로컬 데모 앱은 기존처럼
`sample-app/manifests`의 Kustomize 리소스를 사용할 수 있고, AWS 데모 단계에서는
Helm chart로 OpsPilot과 sample-app을 재현 가능하게 배포합니다.

공개 저장소에서는 기본값을 저비용, 읽기 전용, public ingress 비활성으로 유지합니다.

## 원칙

- public ingress는 기본 비활성입니다.
- 외부 방문자용 live demo는 읽기 전용으로 제한합니다.
- Secret 값은 Helm values에 직접 넣지 않습니다.
- resource request와 limit은 포트폴리오 데모 규모에 맞게 작게 시작합니다.
- Prometheus, OpenCost, sample-app은 데모 목적에 맞게 작은 retention과 replica를 사용합니다.

## 현재 구조

```text
deploy/
  helm/
    opspilot/
      Chart.yaml
      values.yaml
      values-local-kind.yaml
      values-aws-dev-ephemeral.yaml
      templates/
```

`opspilot` chart는 backend와 frontend를 작은 replica/resource 기본값으로 배포합니다.
public ingress는 기본 비활성이고, DB password와 AI provider key 같은 Secret 값은 chart
values에 직접 넣지 않습니다.

아직 이 chart는 EKS 클러스터를 만들지 않습니다. Terraform의 `aws-dev-ephemeral`에서
생성한 ECR 이미지 저장소를 참조할 수 있는 배포 템플릿만 제공합니다.

## 검증 명령

```bash
helm lint deploy/helm/opspilot
helm template opspilot deploy/helm/opspilot -f deploy/helm/opspilot/values-local-kind.yaml
helm template opspilot deploy/helm/opspilot -f deploy/helm/opspilot/values-aws-dev-ephemeral.yaml
kubeconform
```
