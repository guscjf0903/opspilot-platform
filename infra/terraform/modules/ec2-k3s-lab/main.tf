data "aws_region" "current" {}

data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }

  filter {
    name   = "root-device-type"
    values = ["ebs"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

locals {
  name = "${var.name_prefix}-k3s-lab"

  ecr_pull_actions = [
    "ecr:BatchCheckLayerAvailability",
    "ecr:BatchGetImage",
    "ecr:DescribeImages",
    "ecr:DescribeRepositories",
    "ecr:GetDownloadUrlForLayer"
  ]

  user_data = <<-EOT
    #!/usr/bin/env bash
    set -euxo pipefail

    dnf update -y
    dnf install -y curl git jq tar gzip
    dnf install -y awscli-2 || dnf install -y awscli || true

    hostnamectl set-hostname "${local.name}"

    mkdir -p /etc/rancher/k3s
    cat >/etc/rancher/k3s/config.yaml <<'EOF'
    write-kubeconfig-mode: "0644"
    node-label:
      - "opspilot.io/mode=aws-dev-ephemeral"
      - "opspilot.io/runtime=k3s"
    EOF

    curl -sfL https://get.k3s.io | INSTALL_K3S_CHANNEL="${var.k3s_channel}" sh -

    mkdir -p /home/ec2-user/.kube
    cp /etc/rancher/k3s/k3s.yaml /home/ec2-user/.kube/config
    chown -R ec2-user:ec2-user /home/ec2-user/.kube

    curl -fsSL https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 -o /tmp/get_helm.sh
    chmod +x /tmp/get_helm.sh
    /tmp/get_helm.sh

    cat >/etc/opspilot-mode-b-lite.txt <<'EOF'
    OpsPilot Mode B Lite k3s lab is ready.
    Use SSM Session Manager by default.
    Clone the repository and run scripts/mode-b-lite/deploy-k3s.sh with IMAGE_TAG=<commit-sha>.
    EOF
  EOT
}

resource "aws_iam_role" "this" {
  name = "${local.name}-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = var.tags
}

resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.this.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role_policy" "ecr_pull" {
  count = length(var.ecr_repository_arns) > 0 ? 1 : 0

  name = "${local.name}-ecr-pull"
  role = aws_iam_role.this.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["ecr:GetAuthorizationToken"]
        Resource = "*"
      },
      {
        Effect   = "Allow"
        Action   = local.ecr_pull_actions
        Resource = var.ecr_repository_arns
      }
    ]
  })
}

resource "aws_iam_instance_profile" "this" {
  name = "${local.name}-profile"
  role = aws_iam_role.this.name

  tags = var.tags
}

resource "aws_security_group" "this" {
  name        = "${local.name}-sg"
  description = "Security group for OpsPilot Mode B Lite k3s lab."
  vpc_id      = var.vpc_id

  egress {
    description = "Allow outbound traffic for package install and image pulls."
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.tags, {
    Name = "${local.name}-sg"
  })
}

resource "aws_security_group_rule" "ssh" {
  count = length(var.allowed_ssh_cidr_blocks) > 0 ? 1 : 0

  type              = "ingress"
  description       = "Optional SSH access."
  from_port         = 22
  to_port           = 22
  protocol          = "tcp"
  cidr_blocks       = var.allowed_ssh_cidr_blocks
  security_group_id = aws_security_group.this.id
}

resource "aws_security_group_rule" "k3s_api" {
  count = length(var.allowed_k3s_api_cidr_blocks) > 0 ? 1 : 0

  type              = "ingress"
  description       = "Optional Kubernetes API access."
  from_port         = 6443
  to_port           = 6443
  protocol          = "tcp"
  cidr_blocks       = var.allowed_k3s_api_cidr_blocks
  security_group_id = aws_security_group.this.id
}

resource "aws_security_group_rule" "http" {
  count = length(var.allowed_http_cidr_blocks) > 0 ? 1 : 0

  type              = "ingress"
  description       = "Optional public HTTP access for short live demo."
  from_port         = 80
  to_port           = 80
  protocol          = "tcp"
  cidr_blocks       = var.allowed_http_cidr_blocks
  security_group_id = aws_security_group.this.id
}

resource "aws_security_group_rule" "https" {
  count = length(var.allowed_http_cidr_blocks) > 0 ? 1 : 0

  type              = "ingress"
  description       = "Optional public HTTPS access for short live demo."
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = var.allowed_http_cidr_blocks
  security_group_id = aws_security_group.this.id
}

resource "aws_instance" "this" {
  ami                         = data.aws_ami.al2023.id
  instance_type               = var.instance_type
  subnet_id                   = var.subnet_id
  vpc_security_group_ids      = [aws_security_group.this.id]
  associate_public_ip_address = true
  key_name                    = var.key_name
  iam_instance_profile        = aws_iam_instance_profile.this.name
  user_data_replace_on_change = true
  user_data                   = local.user_data

  metadata_options {
    http_endpoint = "enabled"
    http_tokens   = "required"
  }

  root_block_device {
    volume_size = var.root_volume_size_gb
    volume_type = "gp3"
    encrypted   = true

    tags = merge(var.tags, {
      Name = "${local.name}-root"
    })
  }

  tags = merge(var.tags, {
    Name = local.name
  })
}
