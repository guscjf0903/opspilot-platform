# Budget 모듈

OpsPilot 포트폴리오 환경을 위한 저비용 AWS Budgets 가드레일을 생성합니다.

이 모듈은 의도적으로 작게 유지합니다. EKS, RDS, ALB, NAT Gateway, MSK 또는 실제
runtime 인프라는 만들지 않습니다. 다른 AWS 환경을 만들기 전에 먼저 적용해서 비용
알림이 이미 켜져 있도록 합니다.

## 입력값

| 이름 | 목적 |
| --- | --- |
| `budget_name` | 월 예산 이름 |
| `monthly_limit_usd` | 월 비용 한도 |
| `alert_emails` | 예산 알림을 받을 이메일 |
| `actual_thresholds_percent` | 실제 사용액 기준 알림 임계값 |
| `forecasted_thresholds_percent` | 예상 사용액 기준 알림 임계값 |
| `tags` | 공통 소유권과 비용 태그 |

## 주의

- AWS credential을 Terraform 파일에 넣지 않습니다.
- `terraform.tfvars`를 commit하지 않습니다.
- 항상 `terraform plan`을 먼저 실행하고 결과를 검토한 뒤 적용합니다.
