###############################################################################
# Ticketing Platform - Root Module
# AWS Provider >= 5.x | Terraform >= 1.6
###############################################################################

terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.5"
    }
  }

  # Uncomment and configure for remote state
  # backend "s3" {
  #   bucket         = "your-tfstate-bucket"
  #   key            = "ticketing/terraform.tfstate"
  #   region         = "us-east-1"
  #   encrypt        = true
  #   dynamodb_table = "terraform-locks"
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "ticketing-platform"
      Environment = var.environment
      ManagedBy   = "terraform"
      Owner       = var.owner
    }
  }
}

###############################################################################
# Data Sources
###############################################################################

data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

###############################################################################
# Local Values - Configuration computed from variables and module outputs
###############################################################################

locals {
  common_tags = {
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "terraform"
    Owner       = var.owner
  }

  # Application environment variables - constructed dynamically
  app_environment_base = [
    { name = "SPRING_PROFILES_ACTIVE", value = var.environment == "prod" ? "production" : var.environment },
    { name = "AWS_REGION", value = var.aws_region },
    { name = "RESERVATION_TTL_MINUTES", value = tostring(var.reservation_ttl_minutes) },
  ]

  # DynamoDB table mappings
  app_environment_dynamodb = [
    { name = "DYNAMODB_TABLE_EVENTS", value = module.dynamodb.events_table_name },
    { name = "DYNAMODB_TABLE_TICKETS", value = module.dynamodb.tickets_table_name },
    { name = "DYNAMODB_TABLE_ORDERS", value = module.dynamodb.orders_table_name },
    { name = "DYNAMODB_TABLE_INVENTORY", value = module.dynamodb.inventory_table_name },
  ]

  # SQS queue mappings
  app_environment_sqs = [
    { name = "SQS_ORDER_QUEUE_URL", value = module.sqs.purchase_queue_url },
    { name = "SQS_RESERVATION_EXPIRY_QUEUE_URL", value = module.sqs.reservation_expiry_queue_url },
  ]

  # Combine all environment variables
  app_environment = concat(
    local.app_environment_base,
    local.app_environment_dynamodb,
    local.app_environment_sqs,
    var.app_additional_env_vars
  )
}

###############################################################################
# Modules
###############################################################################

module "vpc" {
  source = "./modules/vpc"

  project_name       = var.project_name
  environment        = var.environment
  vpc_cidr           = var.vpc_cidr
  availability_zones = var.availability_zones
  private_subnets    = var.private_subnets
  public_subnets     = var.public_subnets
}

module "iam" {
  source = "./modules/iam"

  project_name    = var.project_name
  environment     = var.environment
  aws_account_id  = data.aws_caller_identity.current.account_id
  aws_region      = var.aws_region
  sqs_queue_arns  = module.sqs.queue_arns
  dynamodb_table_arns = module.dynamodb.table_arns
  secret_arns     = module.secrets.secret_arns
}

module "dynamodb" {
  source = "./modules/dynamodb"

  project_name = var.project_name
  environment  = var.environment
  enable_pitr  = var.enable_dynamodb_pitr
}

module "sqs" {
  source = "./modules/sqs"

  project_name              = var.project_name
  environment               = var.environment
  message_retention_seconds = var.sqs_message_retention_seconds
  visibility_timeout        = var.sqs_visibility_timeout
  max_receive_count         = var.sqs_max_receive_count
}

module "ecr" {
  source = "./modules/ecr"

  project_name     = var.project_name
  environment      = var.environment
  aws_account_id   = data.aws_caller_identity.current.account_id
  image_tag_mutability = var.ecr_image_tag_mutability
}

module "alb" {
  source = "./modules/alb"

  project_name       = var.project_name
  environment        = var.environment
  vpc_id             = module.vpc.vpc_id
  public_subnet_ids  = module.vpc.public_subnet_ids
  certificate_arn    = var.acm_certificate_arn
  allowed_cidr_blocks = var.alb_allowed_cidr_blocks
}

module "ecs" {
  source = "./modules/ecs"

  project_name          = var.project_name
  environment           = var.environment
  vpc_id                = module.vpc.vpc_id
  private_subnet_ids    = module.vpc.private_subnet_ids
  alb_target_group_arn  = module.alb.target_group_arn
  alb_security_group_id = module.alb.security_group_id
  ecr_repository_url    = module.ecr.repository_url
  task_execution_role_arn = module.iam.ecs_task_execution_role_arn
  task_role_arn         = module.iam.ecs_task_role_arn
  app_image_tag         = var.app_image_tag

  # Environment variables - computed in locals
  app_environment = local.app_environment

  # Secrets from Secrets Manager (injected as env vars)
  app_secrets = module.secrets.secret_env_map

  cpu              = var.ecs_task_cpu
  memory           = var.ecs_task_memory
  desired_count    = var.ecs_desired_count
  min_capacity     = var.ecs_min_capacity
  max_capacity     = var.ecs_max_capacity

  container_port    = var.app_container_port
  health_check_path = var.health_check_path

  # For auto-scaling based on ALB metrics
  alb_arn_suffix = module.alb.alb_arn_suffix
  tg_arn_suffix  = module.alb.target_group_arn_suffix
}

module "cloudwatch" {
  source = "./modules/cloudwatch"

  project_name      = var.project_name
  environment       = var.environment
  ecs_cluster_name  = module.ecs.cluster_name
  ecs_service_name  = module.ecs.service_name
  sqs_queue_name    = module.sqs.purchase_queue_name
  alb_arn_suffix    = module.alb.alb_arn_suffix
  alarm_email       = var.alarm_email
  log_retention_days = var.log_retention_days
}

module "secrets" {
  source = "./modules/secrets"

  project_name = var.project_name
  environment  = var.environment
}
