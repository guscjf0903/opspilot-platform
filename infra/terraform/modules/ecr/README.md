# ECR Module

Creates private Amazon ECR repositories for OpsPilot container images.

Cost guardrails:

- Repositories are private.
- Images are encrypted with AES-256.
- Image scanning is disabled by default to avoid surprise usage.
- Lifecycle policy expires untagged images and keeps only a small number of recent images.

This module only creates repositories. Image pushes are handled separately by CI/CD.
