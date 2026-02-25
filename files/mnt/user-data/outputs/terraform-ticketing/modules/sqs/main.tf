###############################################################################
# Module: SQS
# Queues:
#  1. purchase-queue      – main order processing queue (+ DLQ)
#  2. reservation-expiry-queue – triggers release of expired reservations
# Both use SSE-SQS encryption and at-least-once delivery semantics.
###############################################################################

terraform {
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 5.0" }
  }
}

locals {
  name_prefix = "${var.project_name}-${var.environment}"
}

###############################################################################
# Purchase Queue — Dead Letter Queue
###############################################################################

resource "aws_sqs_queue" "purchase_dlq" {
  name                      = "${local.name_prefix}-purchase-dlq"
  message_retention_seconds = 1209600 # 14 days for DLQ
  sqs_managed_sse_enabled   = true

  tags = { Name = "${local.name_prefix}-purchase-dlq", Purpose = "dead-letter" }
}

resource "aws_sqs_queue_redrive_allow_policy" "purchase_dlq" {
  queue_url = aws_sqs_queue.purchase_dlq.id

  redrive_allow_policy = jsonencode({
    redrivePermission = "byQueue"
    sourceQueueArns   = [aws_sqs_queue.purchase.arn]
  })
}

###############################################################################
# Purchase Queue — Main
###############################################################################

resource "aws_sqs_queue" "purchase" {
  name                       = "${local.name_prefix}-purchase-queue"
  message_retention_seconds  = var.message_retention_seconds
  visibility_timeout_seconds = var.visibility_timeout
  sqs_managed_sse_enabled    = true

  # Prevent duplicate processing with deduplication at application level
  # (idempotency key stored in DynamoDB)

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.purchase_dlq.arn
    maxReceiveCount     = var.max_receive_count
  })

  tags = { Name = "${local.name_prefix}-purchase-queue", Purpose = "order-processing" }
}

###############################################################################
# Reservation Expiry Queue — DLQ
###############################################################################

resource "aws_sqs_queue" "reservation_expiry_dlq" {
  name                      = "${local.name_prefix}-reservation-expiry-dlq"
  message_retention_seconds = 1209600
  sqs_managed_sse_enabled   = true

  tags = { Name = "${local.name_prefix}-reservation-expiry-dlq", Purpose = "dead-letter" }
}

resource "aws_sqs_queue_redrive_allow_policy" "reservation_expiry_dlq" {
  queue_url = aws_sqs_queue.reservation_expiry_dlq.id

  redrive_allow_policy = jsonencode({
    redrivePermission = "byQueue"
    sourceQueueArns   = [aws_sqs_queue.reservation_expiry.arn]
  })
}

###############################################################################
# Reservation Expiry Queue — Main
###############################################################################

resource "aws_sqs_queue" "reservation_expiry" {
  name                       = "${local.name_prefix}-reservation-expiry-queue"
  message_retention_seconds  = var.message_retention_seconds
  visibility_timeout_seconds = var.visibility_timeout
  sqs_managed_sse_enabled    = true

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.reservation_expiry_dlq.arn
    maxReceiveCount     = var.max_receive_count
  })

  tags = { Name = "${local.name_prefix}-reservation-expiry-queue", Purpose = "reservation-cleanup" }
}

###############################################################################
# Queue Policies — restrict access to the same AWS account
###############################################################################

data "aws_caller_identity" "current" {}

resource "aws_sqs_queue_policy" "purchase" {
  queue_url = aws_sqs_queue.purchase.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "DenyNonSSL"
        Effect    = "Deny"
        Principal = "*"
        Action    = "sqs:*"
        Resource  = aws_sqs_queue.purchase.arn
        Condition = {
          Bool = { "aws:SecureTransport" = "false" }
        }
      },
      {
        Sid       = "AllowAccountAccess"
        Effect    = "Allow"
        Principal = { AWS = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root" }
        Action    = ["sqs:SendMessage", "sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:GetQueueAttributes"]
        Resource  = aws_sqs_queue.purchase.arn
      }
    ]
  })
}

resource "aws_sqs_queue_policy" "reservation_expiry" {
  queue_url = aws_sqs_queue.reservation_expiry.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "DenyNonSSL"
        Effect    = "Deny"
        Principal = "*"
        Action    = "sqs:*"
        Resource  = aws_sqs_queue.reservation_expiry.arn
        Condition = {
          Bool = { "aws:SecureTransport" = "false" }
        }
      },
      {
        Sid       = "AllowAccountAccess"
        Effect    = "Allow"
        Principal = { AWS = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root" }
        Action    = ["sqs:SendMessage", "sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:GetQueueAttributes"]
        Resource  = aws_sqs_queue.reservation_expiry.arn
      }
    ]
  })
}
