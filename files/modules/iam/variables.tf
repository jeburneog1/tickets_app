variable "project_name" {
  description = "Project name"
  type        = string
}

variable "environment" {
  description = "Environment (prod, staging, dev)"
  type        = string
}

variable "aws_account_id" {
  description = "AWS Account ID"
  type        = string
}

variable "aws_region" {
  description = "AWS Region"
  type        = string
}

variable "dynamodb_table_arns" {
  description = "List of DynamoDB table ARNs"
  type        = list(string)
}

variable "sqs_queue_arns" {
  description = "List of SQS queue ARNs"
  type        = list(string)
}

variable "secret_arns" {
  description = "List of Secrets Manager ARNs"
  type        = list(string)
}
