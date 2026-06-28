# AWS Dev Foundation

이 환경은 Mode B Lite와 Mode B EKS 실습에서 계속 재사용할 foundation 리소스를 관리합니다.
실습 환경을 지우기 위해 `aws-dev-ephemeral`을 destroy해도 ECR과 GitHub Actions Role이 함께
삭제되지 않도록 분리합니다.

## 생성 리소스

- backend/frontend image용 private ECR repository
- ECR lifecycle policy
- GitHub Actions OIDC Provider
- GitHub Actions ECR push IAM Role

## 비용 정책

- IAM Role과 OIDC Provider 자체 비용은 없습니다.
- ECR repository 자체 비용은 없지만 image 저장 용량 비용이 발생할 수 있습니다.
- hard-zero 비용 모드에서는 ECR image와 repository까지 삭제합니다.

## 실행

```bash
cp infra/terraform/envs/aws-dev-foundation/terraform.tfvars.example infra/terraform/envs/aws-dev-foundation/terraform.tfvars
AWS_PROFILE=opspilot-lab terraform -chdir=infra/terraform/envs/aws-dev-foundation init
AWS_PROFILE=opspilot-lab terraform -chdir=infra/terraform/envs/aws-dev-foundation plan
AWS_PROFILE=opspilot-lab terraform -chdir=infra/terraform/envs/aws-dev-foundation apply
```

GitHub Actions secret에는 아래 output을 저장합니다.

```bash
AWS_PROFILE=opspilot-lab terraform -chdir=infra/terraform/envs/aws-dev-foundation output github_actions_ecr_push_role_arn
```

`aws-dev-ephemeral`에는 아래 output 값을 넘깁니다.

```bash
AWS_PROFILE=opspilot-lab terraform -chdir=infra/terraform/envs/aws-dev-foundation output ecr_repository_arns
AWS_PROFILE=opspilot-lab terraform -chdir=infra/terraform/envs/aws-dev-foundation output ecr_repository_urls
```

## 주의

이 환경은 평소 유지하는 foundation입니다. Mode B 실습을 끌 때는 이 환경을 destroy하지 않고
`aws-dev-ephemeral`만 destroy합니다.

이미 예전 `aws-dev-ephemeral`에서 ECR repository를 만들었고 그대로 유지하고 있다면,
foundation으로 import하거나 같은 이름의 repository 충돌을 정리한 뒤 apply해야 합니다.
