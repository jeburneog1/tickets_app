###############################################################################
# Root Variables
###############################################################################

variable "aws_region" {
  description = "AWS region for all resources"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name (prod, staging, dev)"
  type        = string
  validation {
    condition     = contains(["prod", "staging", "dev"], var.environment)
    error_message = "Environment must be prod, staging, or dev."
  }
}

variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
  default     = "ticketing"
}

variable "owner" {
  description = "Owner tag applied to all resources"
  type        = string
  default     = "platform-team"
}

# ── Network ──────────────────────────────────────────────────────────────────

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "Availability zones for subnet placement"
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b", "us-east-1c"]
}

variable "private_subnets" {
  description = "CIDR blocks for private subnets (one per AZ)"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
}

variable "public_subnets" {
  description = "CIDR blocks for public subnets (one per AZ)"
  type        = list(string)
  default     = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]
}

# ── ALB ──────────────────────────────────────────────────────────────────────

variable "acm_certificate_arn" {
  description = "ARN of the ACM certificate for HTTPS. Leave empty to use HTTP only."
  type        = string
  default     = ""
}

variable "alb_allowed_cidr_blocks" {
  description = "CIDR blocks allowed to reach the ALB"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

# ── ECS ──────────────────────────────────────────────────────────────────────

variable "app_image_tag" {
  description = "Docker image tag to deploy"
  type        = string
  default     = "latest"
}

variable "app_container_port" {
  description = "Port exposed by the application container"
  type        = number
  default     = 8080
}

variable "health_check_path" {
  description = "Path for ALB health checks"
  type        = string
  default     = "/actuator/health"
}

variable "ecs_task_cpu" {
  description = "CPU units for the ECS task (1 vCPU = 1024)"
  type        = number
  default     = 1024
}

variable "ecs_task_memory" {
  description = "Memory (MiB) for the ECS task"
  type        = number
  default     = 2048
}

variable "ecs_desired_count" {
  description = "Desired number of ECS tasks"
  type        = number
  default     = 2
}

variable "ecs_min_capacity" {
  description = "Minimum number of ECS tasks for auto-scaling"
  type        = number
  default     = 2
}

variable "ecs_max_capacity" {
  description = "Maximum number of ECS tasks for auto-scaling"
  type        = number
  default     = 10
}

# ── ECR ──────────────────────────────────────────────────────────────────────

variable "ecr_image_tag_mutability" {
  description = "Image tag mutability for ECR (MUTABLE or IMMUTABLE)"
  type        = string
  default     = "IMMUTABLE"
}

# ── DynamoDB ─────────────────────────────────────────────────────────────────

variable "enable_dynamodb_pitr" {
  description = "Enable Point-In-Time Recovery for DynamoDB tables"
  type        = bool
  default     = true
}

# ── SQS ──────────────────────────────────────────────────────────────────────

variable "sqs_message_retention_seconds" {
  description = "SQS message retention period in seconds (default 4 days)"
  type        = number
  default     = 345600
}

variable "sqs_visibility_timeout" {
  description = "SQS visibility timeout in seconds"
  type        = number
  default     = 300
}

variable "sqs_max_receive_count" {
  description = "Number of times a message is received before being sent to DLQ"
  type        = number
  default     = 3
}

# ── Application ──────────────────────────────────────────────────────────────

variable "reservation_ttl_minutes" {
  description = "Ticket reservation timeout in minutes"
  type        = number
  default     = 10
}

variable "app_additional_env_vars" {
  description = "Additional environment variables for the application"
  type = list(object({
    name  = string
    value = string
  }))
  default = []
}

# ── Observability ─────────────────────────────────────────────────────────────

variable "alarm_email" {
  description = "Email address for CloudWatch alarm notifications"
  type        = string
  default     = ""
}

variable "log_retention_days" {
  description = "CloudWatch log retention in days"
  type        = number
  default     = 30
}
