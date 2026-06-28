# EKS Ephemeral Module

이 모듈은 Mode B용 짧은 EKS 실습 환경을 생성합니다.

생성 리소스:

- EKS cluster
- EKS cluster IAM Role
- managed node group 1개
- node IAM Role
- EKS/ELB 인식을 위한 public subnet tag

비용 주의:

- EKS control plane은 클러스터가 존재하는 동안 시간당 과금됩니다.
- managed node group의 EC2, EBS, public IPv4 비용이 발생할 수 있습니다.
- NAT Gateway는 만들지 않습니다.
- ALB, RDS, MSK는 이 모듈에서 만들지 않습니다.

저비용 결정:

- worker node는 public subnet에 배치합니다.
- `aws-dev-ephemeral`은 EKS 사용 시 public subnet의 `map_public_ip_on_launch`를 켭니다.
- node role은 EKS system image와 OpsPilot ECR image pull을 위해
  `AmazonEC2ContainerRegistryReadOnly`를 사용합니다.
- 기본 node group은 `desired=1`, `min=0`, `max=1`, `t3.medium`, `20GiB`입니다.

이 구성은 production 권장 네트워크가 아니라 저비용 ephemeral lab입니다. 사용 후
`aws-dev-ephemeral`을 destroy해서 EKS, node, EBS, VPC 비용을 제거합니다.
