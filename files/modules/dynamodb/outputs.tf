output "events_table_name" {
  description = "Events table name"
  value       = aws_dynamodb_table.events.name
}

output "events_table_arn" {
  description = "Events table ARN"
  value       = aws_dynamodb_table.events.arn
}

output "tickets_table_name" {
  description = "Tickets table name"
  value       = aws_dynamodb_table.tickets.name
}

output "tickets_table_arn" {
  description = "Tickets table ARN"
  value       = aws_dynamodb_table.tickets.arn
}

output "orders_table_name" {
  description = "Orders table name"
  value       = aws_dynamodb_table.orders.name
}

output "orders_table_arn" {
  description = "Orders table ARN"
  value       = aws_dynamodb_table.orders.arn
}

output "inventory_table_name" {
  description = "Inventory table name"
  value       = aws_dynamodb_table.inventory.name
}

output "inventory_table_arn" {
  description = "Inventory table ARN"
  value       = aws_dynamodb_table.inventory.arn
}

output "table_arns" {
  description = "List of all table ARNs"
  value = [
    aws_dynamodb_table.events.arn,
    aws_dynamodb_table.tickets.arn,
    aws_dynamodb_table.orders.arn,
    aws_dynamodb_table.inventory.arn
  ]
}
