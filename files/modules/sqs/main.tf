###############################################################################
# SQS Module - Asynchronous Message Queues
# 
# DECISIONES DE DISEÑO:
# =====================
# 1. FIFO QUEUES: Garantiza orden y exactly-once processing (crítico para órdenes)
# 2. DEAD LETTER QUEUE: Manejo de mensajes fallidos para debugging
# 3. ENCRYPTION: Server-side encryption con AWS managed keys
# 4. VISIBILITY TIMEOUT: 5 minutos para procesamiento complejo
# 5. MESSAGE RETENTION: 4 días (balance entre almacenamiento y recuperación)
# 6. CONTENT-BASED DEDUPLICATION: Evita duplicados automáticamente
###############################################################################

locals {
  common_tags = {
    Module      = "sqs"
    Project     = var.project_name
    Environment = var.environment
  }
}

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# DEAD LETTER QUEUE - Para mensajes fallidos de order processing
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
resource "aws_sqs_queue" "order_processing_dlq" {
  # FIFO queues require exact name with .fifo suffix
  name                        = "${var.project_name}-${var.environment}-order-processing-dlq.fifo"
  fifo_queue                  = true
  content_based_deduplication = true
  
  # Retención extendida para análisis de fallos (14 días)
  message_retention_seconds = 1209600
  
  # Encryption at rest
  sqs_managed_sse_enabled = true

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-${var.environment}-order-processing-dlq"
    Type = "dead-letter-queue"
  })

  lifecycle {
    # Prevent accidental deletion of DLQ in production
    prevent_destroy = false  # Set to true in production
  }
}

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# MAIN QUEUE - Procesamiento asíncrono de órdenes
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
resource "aws_sqs_queue" "order_processing" {
  name                       = "${var.project_name}-${var.environment}-order-processing-queue.fifo"
  fifo_queue                 = true
  content_based_deduplication = true
  
  # Configuración de timeouts
  message_retention_seconds  = var.message_retention_seconds
  visibility_timeout_seconds = var.visibility_timeout
  receive_wait_time_seconds  = 20  # Long polling para reducir costos

  # Encryption at rest
  sqs_managed_sse_enabled = true

  # Dead Letter Queue configuration
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.order_processing_dlq.arn
    maxReceiveCount     = var.max_receive_count
  })

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-${var.environment}-order-processing-queue"
    Type = "main-queue"
  })
}

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# RESERVATION EXPIRY QUEUE - Cleanup de reservaciones expiradas
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
resource "aws_sqs_queue" "reservation_expiry_dlq" {
  name                        = "${var.project_name}-${var.environment}-reservation-expiry-dlq.fifo"
  fifo_queue                  = true
  content_based_deduplication = true
  message_retention_seconds   = 1209600  # 14 días
  sqs_managed_sse_enabled     = true

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-${var.environment}-reservation-expiry-dlq"
    Type = "dead-letter-queue"
  })

  lifecycle {
    prevent_destroy = false  # Set to true in production
  }
}

resource "aws_sqs_queue" "reservation_expiry" {
  name                        = "${var.project_name}-${var.environment}-reservation-expiry-queue.fifo"
  fifo_queue                  = true
  content_based_deduplication = true
  message_retention_seconds   = var.message_retention_seconds
  visibility_timeout_seconds  = 60  # Menor timeout para limpieza rápida
  receive_wait_time_seconds   = 20
  sqs_managed_sse_enabled     = true

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.reservation_expiry_dlq.arn
    maxReceiveCount     = var.max_receive_count
  })

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-${var.environment}-reservation-expiry-queue"
    Type = "cleanup-queue"
  })

  depends_on = [aws_sqs_queue.reservation_expiry_dlq]

  lifecycle {
    prevent_destroy = false  # Set to true in production
  }
}

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# CloudWatch Alarms - Monitoreo de colas
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

# Alarma para mensajes en DLQ (indica problemas)
resource "aws_cloudwatch_metric_alarm" "dlq_messages" {
  alarm_name          = "${var.project_name}-${var.environment}-sqs-dlq-messages"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = 300
  statistic           = "Average"
  threshold           = 10
  alarm_description   = "Alert when messages accumulate in DLQ"
  treat_missing_data  = "notBreaching"

  dimensions = {
    QueueName = aws_sqs_queue.order_processing_dlq.name
  }

  tags = local.common_tags
}

# Alarma para mensajes antiguos (latencia en procesamiento)
resource "aws_cloudwatch_metric_alarm" "queue_age" {
  alarm_name          = "${var.project_name}-${var.environment}-sqs-message-age"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "ApproximateAgeOfOldestMessage"
  namespace           = "AWS/SQS"
  period              = 300
  statistic           = "Maximum"
  threshold           = 600  # 10 minutos
  alarm_description   = "Alert when messages are not being processed"
  treat_missing_data  = "notBreaching"

  dimensions = {
    QueueName = aws_sqs_queue.order_processing.name
  }

  tags = local.common_tags
}
