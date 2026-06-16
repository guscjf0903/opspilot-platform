# 비용 가드 부트스트랩

OpsPilot AWS 작업을 시작하기 전에 가장 먼저 실행하는 Terraform 환경입니다.

이 환경은 AWS Budgets 알림만 생성합니다. EKS, EC2, ALB, RDS, MSK, NAT Gateway,
Route53, 실제 workload는 생성하지 않습니다.

## 언제 사용하는가

다른 AWS 리소스를 만들기 전에 먼저 실행합니다.

## 설정

```bash
cd infra/terraform/envs/bootstrap-cost-guard
cp terraform.tfvars.example terraform.tfvars
```

`terraform.tfvars`에 실제 알림 이메일을 넣습니다.

```hcl
alert_emails = ["you@example.com"]
```

`terraform.tfvars`는 commit하지 않습니다.

## 검증

```bash
terraform init -backend=false
terraform fmt -recursive
terraform validate
terraform plan
```

## 적용 정책

`terraform apply`는 plan 결과를 사람이 확인한 뒤에만 실행합니다.

이 환경은 의도적으로 안전하게 작게 만들었지만, 그래도 AWS 계정 단위 예산 알림을
생성합니다. 첫 단계부터 승인 습관을 유지합니다.
