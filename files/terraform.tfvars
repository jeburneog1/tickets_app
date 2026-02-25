###############################################################################
# Production environment configuration
# 
# Instructions:
#   1. Copy this file: cp terraform.tfvars.example terraform.tfvars
#   2. Update values below for your environment
#   3. NEVER commit terraform.tfvars to version control (it's in .gitignore)
###############################################################################

# ── General Configuration ────────────────────────────────────────────────────

aws_region   = "us-east-1"
environment  = "dev"
project_name = "ticketing"
owner        = "platform-team"

# ── Network (Multi-AZ for High Availability) ─────────────────────────────────

vpc_cidr           = "10.0.0.0/16"
availability_zones = ["us-east-1a", "us-east-1b", "us-east-1c"]
private_subnets    = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
public_subnets     = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]

# ── ALB (Load Balancer) ──────────────────────────────────────────────────────

# ACM certificate ARN for HTTPS (create certificate in AWS ACM first)
# Leave empty to use HTTP only (not recommended for production)
# acm_certificate_arn = "arn:aws:acm:us-east-1:123456789012:certificate/xxxxxxxx"
acm_certificate_arn = ""

# IP ranges allowed to access the ALB (use specific IPs in production)
alb_allowed_cidr_blocks = ["0.0.0.0/0"]  # Change to your office/VPN IPs

# ── ECS (Container Orchestration) ────────────────────────────────────────────

app_image_tag       = "latest"       # Use git commit SHA in production (e.g., "v1.2.3-abc123")
app_container_port  = 8080           # Spring Boot default port
health_check_path   = "/actuator/health"

# Task sizing (1 vCPU = 1024 units)
ecs_task_cpu        = 1024           # 1 vCPU
ecs_task_memory     = 2048           # 2GB RAM

# Auto-scaling configuration
ecs_desired_count   = 2              # Baseline (must be >= min_capacity)
ecs_min_capacity    = 2              # Always keep 2 tasks running (HA)
ecs_max_capacity    = 10             # Scale up to 10 during peak traffic

# ── Application Configuration ────────────────────────────────────────────────

reservation_ttl_minutes = 10         # Ticket reservation timeout

# Additional custom env vars (optional)
# app_additional_env_vars = [
#   { name = "FEATURE_FLAG_X", value = "true" },
#   { name = "MAX_CONNECTIONS", value = "100" }
# ]

# ── ECR (Container Registry) ─────────────────────────────────────────────────

ecr_image_tag_mutability = "IMMUTABLE"  # Prevent tag overwriting (security)

# ── DynamoDB (NoSQL Database) ────────────────────────────────────────────────

enable_dynamodb_pitr = true          # Point-in-Time Recovery (backup)

# ── SQS (Message Queues) ─────────────────────────────────────────────────────

sqs_message_retention_seconds = 345600    # 4 days
sqs_visibility_timeout        = 300       # 5 minutes
sqs_max_receive_count         = 3         # Max retries before DLQ

# ── Observability ────────────────────────────────────────────────────────────

# Email for CloudWatch alarms (REQUIRED for production monitoring)
alarm_email        = "ops@your-company.com"  # UPDATE THIS

# Log retention (7, 14, 30, 60, 90, 120, 365, or 0 for never expire)
log_retention_days = 30

###############################################################################
# Environment-Specific Examples
###############################################################################

# ── STAGING ──────────────────────────────────────────────────────────────────
# environment         = "staging"
# ecs_desired_count   = 1
# ecs_max_capacity    = 3
# enable_dynamodb_pitr = false
# log_retention_days  = 7

# ── DEVELOPMENT ──────────────────────────────────────────────────────────────
# environment         = "dev"
# ecs_desired_count   = 1
# ecs_max_capacity    = 2
# ecs_task_cpu        = 512
# ecs_task_memory     = 1024
# enable_dynamodb_pitr = false
# log_retention_days  = 3

