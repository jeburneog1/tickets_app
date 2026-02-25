output "dns_name" {
  description = "ALB DNS name"
  value       = aws_lb.main.dns_name
}

output "zone_id" {
  description = "ALB Zone ID"
  value       = aws_lb.main.zone_id
}

output "alb_arn" {
  description = "ALB ARN"
  value       = aws_lb.main.arn
}

output "alb_arn_suffix" {
  description = "ALB ARN suffix for CloudWatch metrics"
  value       = aws_lb.main.arn_suffix
}

output "target_group_arn" {
  description = "Target Group ARN"
  value       = aws_lb_target_group.app.arn
}

output "target_group_name" {
  description = "Target Group name"
  value       = aws_lb_target_group.app.name
}

output "target_group_arn_suffix" {
  description = "Target Group ARN suffix for CloudWatch metrics"
  value       = aws_lb_target_group.app.arn_suffix
}

output "security_group_id" {
  description = "ALB Security Group ID"
  value       = aws_security_group.alb.id
}
