###############################################################################
# Root Outputs
###############################################################################

output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer"
  value       = module.alb.dns_name
}

output "ecr_repository_url" {
  description = "ECR repository URL for pushing Docker images"
  value       = module.ecr.repository_url
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = module.ecs.cluster_name
}

output "ecs_service_name" {
  description = "ECS service name"
  value       = module.ecs.service_name
}

output "dynamodb_events_table" {
  description = "DynamoDB events table name"
  value       = module.dynamodb.events_table_name
}

output "dynamodb_orders_table" {
  description = "DynamoDB orders table name"
  value       = module.dynamodb.orders_table_name
}

output "dynamodb_inventory_table" {
  description = "DynamoDB inventory table name"
  value       = module.dynamodb.inventory_table_name
}

output "sqs_purchase_queue_url" {
  description = "SQS purchase queue URL"
  value       = module.sqs.purchase_queue_url
}

output "sqs_purchase_dlq_url" {
  description = "SQS purchase dead-letter queue URL"
  value       = module.sqs.purchase_dlq_url
}

output "vpc_id" {
  description = "VPC ID"
  value       = module.vpc.vpc_id
}

output "cloudwatch_log_group" {
  description = "CloudWatch log group for the application"
  value       = module.cloudwatch.log_group_name
}

output "deploy_command" {
  description = "Command to force a new ECS deployment after pushing a new image"
  value       = "aws ecs update-service --cluster ${module.ecs.cluster_name} --service ${module.ecs.service_name} --force-new-deployment --region ${var.aws_region}"
}
