###############################################################################
# DynamoDB Module - NoSQL Database Tables
# 
# DECISIONES DE DISEÑO:
# =====================
# 1. ON-DEMAND vs PROVISIONED: Provisioned con auto-scaling para costos predecibles
# 2. ENCRYPTION: Encryption at rest habilitada (AWS managed keys por defecto)
# 3. POINT-IN-TIME RECOVERY: Habilitado para disaster recovery (hasta 35 días)
# 4. GSI (Global Secondary Indexes): Para queries eficientes por diferentes atributos
# 5. TTL: Para expiración automática de reservaciones temporales
# 6. STREAM: Habilitado para event sourcing y replicación
###############################################################################

locals {
  common_tags = {
    Module      = "dynamodb"
    Project     = var.project_name
    Environment = var.environment
  }
}

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# EVENTS TABLE - Catálogo de eventos disponibles
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
resource "aws_dynamodb_table" "events" {
  name           = "${var.project_name}-${var.environment}-events"
  billing_mode   = "PROVISIONED"
  read_capacity  = 10
  write_capacity = 5
  hash_key       = "eventId"

  attribute {
    name = "eventId"
    type = "S"
  }

  attribute {
    name = "date"
    type = "S"
  }

  # GSI para buscar eventos por fecha
  global_secondary_index {
    name            = "date-index"
    hash_key        = "date"
    projection_type = "ALL"
    read_capacity   = 5
    write_capacity  = 5
  }

  # Encryption at rest (AWS managed keys)
  server_side_encryption {
    enabled = true
  }

  # Point-in-time recovery para disaster recovery
  point_in_time_recovery {
    enabled = var.enable_pitr
  }

  # Stream para event sourcing
  stream_enabled   = true
  stream_view_type = "NEW_AND_OLD_IMAGES"

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-${var.environment}-events"
  })
}

# Auto-scaling para la tabla events
resource "aws_appautoscaling_target" "events_read" {
  max_capacity       = 50
  min_capacity       = 5
  resource_id        = "table/${aws_dynamodb_table.events.name}"
  scalable_dimension = "dynamodb:table:ReadCapacityUnits"
  service_namespace  = "dynamodb"
}

resource "aws_appautoscaling_policy" "events_read" {
  name               = "${var.project_name}-${var.environment}-events-read-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.events_read.resource_id
  scalable_dimension = aws_appautoscaling_target.events_read.scalable_dimension
  service_namespace  = aws_appautoscaling_target.events_read.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "DynamoDBReadCapacityUtilization"
    }
    target_value = 70.0
  }
}

resource "aws_appautoscaling_target" "events_write" {
  max_capacity       = 50
  min_capacity       = 5
  resource_id        = "table/${aws_dynamodb_table.events.name}"
  scalable_dimension = "dynamodb:table:WriteCapacityUnits"
  service_namespace  = "dynamodb"
}

resource "aws_appautoscaling_policy" "events_write" {
  name               = "${var.project_name}-${var.environment}-events-write-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.events_write.resource_id
  scalable_dimension = aws_appautoscaling_target.events_write.scalable_dimension
  service_namespace  = aws_appautoscaling_target.events_write.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "DynamoDBWriteCapacityUtilization"
    }
    target_value = 70.0
  }
}

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# TICKETS TABLE - Inventario de tickets (con reservaciones temporales)
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
resource "aws_dynamodb_table" "tickets" {
  name           = "${var.project_name}-${var.environment}-tickets"
  billing_mode   = "PROVISIONED"
  read_capacity  = 50  # Mayor capacidad para alta concurrencia
  write_capacity = 50
  hash_key       = "ticketId"

  attribute {
    name = "ticketId"
    type = "S"
  }

  attribute {
    name = "eventId"
    type = "S"
  }

  attribute {
    name = "status"
    type = "S"
  }

  attribute {
    name = "customerId"
    type = "S"
  }

  attribute {
    name = "orderId"
    type = "S"
  }

  # GSI para buscar tickets por evento y estado (query eficiente de disponibilidad)
  global_secondary_index {
    name            = "eventId-status-index"
    hash_key        = "eventId"
    range_key       = "status"
    projection_type = "ALL"
    read_capacity   = 50
    write_capacity  = 50
  }

  # GSI para buscar tickets por cliente
  global_secondary_index {
    name            = "customerId-index"
    hash_key        = "customerId"
    projection_type = "ALL"
    read_capacity   = 10
    write_capacity  = 10
  }

  # GSI para buscar tickets por orden
  global_secondary_index {
    name            = "orderId-index"
    hash_key        = "orderId"
    projection_type = "ALL"
    read_capacity   = 10
    write_capacity  = 10
  }

  # TTL para expiración automática de reservaciones
  ttl {
    attribute_name = "expirationTime"
    enabled        = true
  }

  server_side_encryption {
    enabled = true
  }

  point_in_time_recovery {
    enabled = var.enable_pitr
  }

  stream_enabled   = true
  stream_view_type = "NEW_AND_OLD_IMAGES"

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-${var.environment}-tickets"
  })
}

# Auto-scaling para tickets (alta concurrencia esperada)
resource "aws_appautoscaling_target" "tickets_read" {
  max_capacity       = 200
  min_capacity       = 50
  resource_id        = "table/${aws_dynamodb_table.tickets.name}"
  scalable_dimension = "dynamodb:table:ReadCapacityUnits"
  service_namespace  = "dynamodb"
}

resource "aws_appautoscaling_policy" "tickets_read" {
  name               = "${var.project_name}-${var.environment}-tickets-read-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.tickets_read.resource_id
  scalable_dimension = aws_appautoscaling_target.tickets_read.scalable_dimension
  service_namespace  = aws_appautoscaling_target.tickets_read.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "DynamoDBReadCapacityUtilization"
    }
    target_value = 70.0
  }
}

resource "aws_appautoscaling_target" "tickets_write" {
  max_capacity       = 200
  min_capacity       = 50
  resource_id        = "table/${aws_dynamodb_table.tickets.name}"
  scalable_dimension = "dynamodb:table:WriteCapacityUnits"
  service_namespace  = "dynamodb"
}

resource "aws_appautoscaling_policy" "tickets_write" {
  name               = "${var.project_name}-${var.environment}-tickets-write-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.tickets_write.resource_id
  scalable_dimension = aws_appautoscaling_target.tickets_write.scalable_dimension
  service_namespace  = aws_appautoscaling_target.tickets_write.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "DynamoDBWriteCapacityUtilization"
    }
    target_value = 70.0
  }
}

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# ORDERS TABLE - Órdenes de compra
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
resource "aws_dynamodb_table" "orders" {
  name           = "${var.project_name}-${var.environment}-orders"
  billing_mode   = "PROVISIONED"
  read_capacity  = 25
  write_capacity = 25
  hash_key       = "orderId"

  attribute {
    name = "orderId"
    type = "S"
  }

  attribute {
    name = "customerId"
    type = "S"
  }

  attribute {
    name = "eventId"
    type = "S"
  }

  attribute {
    name = "createdAt"
    type = "S"
  }

  # GSI para buscar órdenes por cliente
  global_secondary_index {
    name            = "customerId-createdAt-index"
    hash_key        = "customerId"
    range_key       = "createdAt"
    projection_type = "ALL"
    read_capacity   = 10
    write_capacity  = 10
  }

  # GSI para buscar órdenes por evento
  global_secondary_index {
    name            = "eventId-createdAt-index"
    hash_key        = "eventId"
    range_key       = "createdAt"
    projection_type = "ALL"
    read_capacity   = 10
    write_capacity  = 10
  }

  server_side_encryption {
    enabled = true
  }

  point_in_time_recovery {
    enabled = var.enable_pitr
  }

  stream_enabled   = true
  stream_view_type = "NEW_AND_OLD_IMAGES"

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-${var.environment}-orders"
  })
}

# Auto-scaling para orders
resource "aws_appautoscaling_target" "orders_read" {
  max_capacity       = 100
  min_capacity       = 10
  resource_id        = "table/${aws_dynamodb_table.orders.name}"
  scalable_dimension = "dynamodb:table:ReadCapacityUnits"
  service_namespace  = "dynamodb"
}

resource "aws_appautoscaling_policy" "orders_read" {
  name               = "${var.project_name}-${var.environment}-orders-read-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.orders_read.resource_id
  scalable_dimension = aws_appautoscaling_target.orders_read.scalable_dimension
  service_namespace  = aws_appautoscaling_target.orders_read.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "DynamoDBReadCapacityUtilization"
    }
    target_value = 70.0
  }
}

resource "aws_appautoscaling_target" "orders_write" {
  max_capacity       = 100
  min_capacity       = 10
  resource_id        = "table/${aws_dynamodb_table.orders.name}"
  scalable_dimension = "dynamodb:table:WriteCapacityUnits"
  service_namespace  = "dynamodb"
}

resource "aws_appautoscaling_policy" "orders_write" {
  name               = "${var.project_name}-${var.environment}-orders-write-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.orders_write.resource_id
  scalable_dimension = aws_appautoscaling_target.orders_write.scalable_dimension
  service_namespace  = aws_appautoscaling_target.orders_write.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "DynamoDBWriteCapacityUtilization"
    }
    target_value = 70.0
  }
}

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# INVENTORY TABLE - Control de inventario en tiempo real
# NOTA: Esta tabla podría usar optimistic locking con version fields
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
resource "aws_dynamodb_table" "inventory" {
  name           = "${var.project_name}-${var.environment}-inventory"
  billing_mode   = "PROVISIONED"
  read_capacity  = 100  # Alta capacidad para consultas frecuentes
  write_capacity = 100
  hash_key       = "eventId"

  attribute {
    name = "eventId"
    type = "S"
  }

  server_side_encryption {
    enabled = true
  }

  point_in_time_recovery {
    enabled = var.enable_pitr
  }

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-${var.environment}-inventory"
  })
}

# Auto-scaling agresivo para inventory (alta concurrencia)
resource "aws_appautoscaling_target" "inventory_read" {
  max_capacity       = 500
  min_capacity       = 100
  resource_id        = "table/${aws_dynamodb_table.inventory.name}"
  scalable_dimension = "dynamodb:table:ReadCapacityUnits"
  service_namespace  = "dynamodb"
}

resource "aws_appautoscaling_policy" "inventory_read" {
  name               = "${var.project_name}-${var.environment}-inventory-read-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.inventory_read.resource_id
  scalable_dimension = aws_appautoscaling_target.inventory_read.scalable_dimension
  service_namespace  = aws_appautoscaling_target.inventory_read.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "DynamoDBReadCapacityUtilization"
    }
    target_value = 70.0
  }
}

resource "aws_appautoscaling_target" "inventory_write" {
  max_capacity       = 500
  min_capacity       = 100
  resource_id        = "table/${aws_dynamodb_table.inventory.name}"
  scalable_dimension = "dynamodb:table:WriteCapacityUnits"
  service_namespace  = "dynamodb"
}

resource "aws_appautoscaling_policy" "inventory_write" {
  name               = "${var.project_name}-${var.environment}-inventory-write-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.inventory_write.resource_id
  scalable_dimension = aws_appautoscaling_target.inventory_write.scalable_dimension
  service_namespace  = aws_appautoscaling_target.inventory_write.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "DynamoDBWriteCapacityUtilization"
    }
    target_value = 70.0
  }
}
