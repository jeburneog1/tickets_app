output "ecs_task_execution_role_arn" {
  description = "ECS Task Execution Role ARN"
  value       = aws_iam_role.ecs_task_execution.arn
}

output "ecs_task_execution_role_name" {
  description = "ECS Task Execution Role Name"
  value       = aws_iam_role.ecs_task_execution.name
}

output "ecs_task_role_arn" {
  description = "ECS Task Role ARN"
  value       = aws_iam_role.ecs_task.arn
}

output "ecs_task_role_name" {
  description = "ECS Task Role Name"
  value       = aws_iam_role.ecs_task.name
}

output "ecs_autoscaling_role_arn" {
  description = "ECS Auto Scaling Role ARN"
  value       = aws_iam_role.ecs_autoscaling.arn
}
