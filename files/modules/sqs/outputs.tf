output "purchase_queue_url" {
  description = "Order processing queue URL"
  value       = aws_sqs_queue.order_processing.url
}

output "purchase_queue_arn" {
  description = "Order processing queue ARN"
  value       = aws_sqs_queue.order_processing.arn
}

output "purchase_queue_name" {
  description = "Order processing queue name"
  value       = aws_sqs_queue.order_processing.name
}

output "purchase_dlq_url" {
  description = "Order processing DLQ URL"
  value       = aws_sqs_queue.order_processing_dlq.url
}

output "purchase_dlq_arn" {
  description = "Order processing DLQ ARN"
  value       = aws_sqs_queue.order_processing_dlq.arn
}

output "reservation_expiry_queue_url" {
  description = "Reservation expiry queue URL"
  value       = aws_sqs_queue.reservation_expiry.url
}

output "reservation_expiry_queue_arn" {
  description = "Reservation expiry queue ARN"
  value       = aws_sqs_queue.reservation_expiry.arn
}

output "queue_arns" {
  description = "List of all queue ARNs"
  value = [
    aws_sqs_queue.order_processing.arn,
    aws_sqs_queue.order_processing_dlq.arn,
    aws_sqs_queue.reservation_expiry.arn,
    aws_sqs_queue.reservation_expiry_dlq.arn
  ]
}
