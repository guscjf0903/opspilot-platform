data "aws_region" "current" {}

locals {
  node_group_name = "${var.cluster_name}-default"
}

resource "terraform_data" "configuration_guard" {
  input = {
    endpoint_private_access = var.endpoint_private_access
    endpoint_public_access  = var.endpoint_public_access
    node_group_desired_size = var.node_group_desired_size
    node_group_max_size     = var.node_group_max_size
    node_group_min_size     = var.node_group_min_size
  }

  lifecycle {
    precondition {
      condition     = var.endpoint_public_access || var.endpoint_private_access
      error_message = "At least one EKS endpoint access mode must be enabled."
    }

    precondition {
      condition     = var.node_group_desired_size >= var.node_group_min_size && var.node_group_desired_size <= var.node_group_max_size
      error_message = "node_group_desired_size must be between node_group_min_size and node_group_max_size."
    }
  }
}

resource "aws_iam_role" "cluster" {
  name = "${var.cluster_name}-cluster-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "eks.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = var.tags
}

resource "aws_iam_role_policy_attachment" "cluster" {
  role       = aws_iam_role.cluster.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
}

resource "aws_iam_role" "node" {
  name = "${var.cluster_name}-node-role"

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

resource "aws_iam_role_policy_attachment" "node_worker" {
  role       = aws_iam_role.node.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
}

resource "aws_iam_role_policy_attachment" "node_cni" {
  role       = aws_iam_role.node.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
}

resource "aws_iam_role_policy_attachment" "node_ecr" {
  role       = aws_iam_role.node.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

resource "aws_ec2_tag" "public_subnet_cluster" {
  for_each = toset(var.public_subnet_ids)

  resource_id = each.value
  key         = "kubernetes.io/cluster/${var.cluster_name}"
  value       = "shared"
}

resource "aws_ec2_tag" "public_subnet_elb" {
  for_each = toset(var.public_subnet_ids)

  resource_id = each.value
  key         = "kubernetes.io/role/elb"
  value       = "1"
}

resource "aws_eks_cluster" "this" {
  name     = var.cluster_name
  role_arn = aws_iam_role.cluster.arn
  version  = var.kubernetes_version

  access_config {
    authentication_mode                         = "API_AND_CONFIG_MAP"
    bootstrap_cluster_creator_admin_permissions = true
  }

  vpc_config {
    subnet_ids              = var.public_subnet_ids
    endpoint_public_access  = var.endpoint_public_access
    endpoint_private_access = var.endpoint_private_access
    public_access_cidrs     = var.endpoint_public_access_cidrs
  }

  depends_on = [
    aws_iam_role_policy_attachment.cluster
  ]

  tags = merge(var.tags, {
    Name = var.cluster_name
  })
}

resource "aws_eks_node_group" "default" {
  cluster_name    = aws_eks_cluster.this.name
  node_group_name = local.node_group_name
  node_role_arn   = aws_iam_role.node.arn
  subnet_ids      = var.public_subnet_ids

  ami_type       = var.node_group_ami_type
  capacity_type  = var.node_group_capacity_type
  disk_size      = var.node_group_disk_size_gb
  instance_types = var.node_group_instance_types

  scaling_config {
    desired_size = var.node_group_desired_size
    min_size     = var.node_group_min_size
    max_size     = var.node_group_max_size
  }

  update_config {
    max_unavailable = 1
  }

  labels = var.node_group_labels

  depends_on = [
    aws_iam_role_policy_attachment.node_worker,
    aws_iam_role_policy_attachment.node_cni,
    aws_iam_role_policy_attachment.node_ecr,
    aws_ec2_tag.public_subnet_cluster,
    aws_ec2_tag.public_subnet_elb
  ]

  tags = merge(var.tags, {
    Name = local.node_group_name
  })
}
