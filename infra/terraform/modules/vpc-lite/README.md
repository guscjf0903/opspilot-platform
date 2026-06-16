# VPC Lite 모듈

OpsPilot 임시 AWS 데모를 위한 작고 저비용인 VPC 기반을 생성합니다.

## 비용 가드레일

- 기본값은 public subnet 2개입니다.
- NAT Gateway를 만들지 않습니다.
- VPC endpoint를 만들지 않습니다.
- 생성되는 subnet은 instance의 public IPv4 자동 할당을 켜지 않습니다.

이 구성은 포트폴리오 데모용 네트워크입니다. production 수준 설계에서는 private subnet,
NAT Gateway 또는 VPC endpoint, 더 강한 ingress 정책, 필요 시 WAF를 별도로 고려해야 합니다.
