locals {
  github_oidc_provider_host = replace(var.github_oidc_provider_url, "https://", "")
  oidc_provider_arn         = var.create_oidc_provider ? aws_iam_openid_connect_provider.github[0].arn : var.existing_oidc_provider_arn
}

resource "aws_iam_openid_connect_provider" "github" {
  count = var.create_oidc_provider ? 1 : 0

  url            = var.github_oidc_provider_url
  client_id_list = [var.oidc_audience]
  tags           = var.tags
}

data "aws_iam_policy_document" "assume_role" {
  statement {
    sid     = "AllowGitHubActionsOidc"
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [local.oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.github_oidc_provider_host}:aud"
      values   = [var.oidc_audience]
    }

    condition {
      test     = "StringLike"
      variable = "${local.github_oidc_provider_host}:sub"
      values   = var.allowed_subject_patterns
    }
  }
}

resource "aws_iam_role" "this" {
  name                 = var.role_name
  description          = "Allows selected GitHub Actions workflows to push OpsPilot images to ECR."
  assume_role_policy   = data.aws_iam_policy_document.assume_role.json
  max_session_duration = var.max_session_duration_seconds
  tags                 = var.tags

  lifecycle {
    precondition {
      condition     = var.create_oidc_provider || length(var.existing_oidc_provider_arn) > 0
      error_message = "existing_oidc_provider_arn is required when create_oidc_provider is false."
    }

    precondition {
      condition     = length(var.ecr_repository_arns) > 0
      error_message = "At least one ECR repository ARN is required."
    }
  }
}

data "aws_iam_policy_document" "ecr_push" {
  statement {
    sid       = "GetEcrAuthorizationToken"
    effect    = "Allow"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid    = "PushImagesToAllowedRepositories"
    effect = "Allow"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:CompleteLayerUpload",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart"
    ]
    resources = var.ecr_repository_arns
  }
}

resource "aws_iam_role_policy" "ecr_push" {
  name   = "${var.role_name}-ecr-push"
  role   = aws_iam_role.this.id
  policy = data.aws_iam_policy_document.ecr_push.json
}
