###############################################################################
# Module: DynamoDB
# Three tables: events, orders, inventory
# Uses optimistic locking (version attribute) and TTL for reservation expiry.
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
# Events Table
# PK: eventId (String)
###############################################################################

resource "aws_dynamodb_table" "events" {
  name         = "${local.name_prefix}-events"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "eventId"

  attribute {
    name = "eventId"
    type = "S"
  }

  # GSI to query by venue/date
  attribute {
    name = "eventDate"
    type = "S"
  }

  attribute {
    name = "venue"
    type = "S"
  }

  global_secondary_index {
    name            = "venue-date-index"
    hash_key        = "venue"
    range_key       = "eventDate"
    projection_type = "ALL"
  }

  point_in_time_recovery {
    enabled = var.enable_pitr
  }

  server_side_encryption {
    enabled = true
  }

  tags = { Name = "${local.name_prefix}-events" }
}

###############################################################################
# Orders Table
# PK: orderId (String)
# SK: userId  (String)  — allows querying all orders of a user
###############################################################################

resource "aws_dynamodb_table" "orders" {
  name         = "${local.name_prefix}-orders"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "orderId"
  range_key    = "userId"

  attribute {
    name = "orderId"
    type = "S"
  }

  attribute {
    name = "userId"
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

  # GSI: query all orders for a user
  global_secondary_index {
    name            = "userId-index"
    hash_key        = "userId"
    range_key       = "orderId"
    projection_type = "ALL"
  }

  # GSI: query orders by event + status (operational reporting)
  global_secondary_index {
    name            = "eventId-status-index"
    hash_key        = "eventId"
    range_key       = "status"
    projection_type = "ALL"
  }

  # TTL for automatic cleanup of old orders
  ttl {
    attribute_name = "expiresAt"
    enabled        = true
  }

  point_in_time_recovery {
    enabled = var.enable_pitr
  }

  server_side_encryption {
    enabled = true
  }

  tags = { Name = "${local.name_prefix}-orders" }
}

###############################################################################
# Inventory Table
# PK: eventId (String)
# Stores available/reserved/sold counts with optimistic locking via `version`.
###############################################################################

resource "aws_dynamodb_table" "inventory" {
  name         = "${local.name_prefix}-inventory"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "eventId"

  attribute {
    name = "eventId"
    type = "S"
  }

  # TTL used for auto-releasing expired RESERVED tickets
  ttl {
    attribute_name = "reservationExpiresAt"
    enabled        = true
  }

  point_in_time_recovery {
    enabled = var.enable_pitr
  }

  server_side_encryption {
    enabled = true
  }

  tags = { Name = "${local.name_prefix}-inventory" }
}

###############################################################################
# Audit / State-Transition Log Table
# Immutable record of every ticket state transition.
###############################################################################

resource "aws_dynamodb_table" "audit_log" {
  name         = "${local.name_prefix}-audit-log"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "entityId"   # orderId or ticketId
  range_key    = "timestamp"  # ISO-8601 string

  attribute {
    name = "entityId"
    type = "S"
  }

  attribute {
    name = "timestamp"
    type = "S"
  }

  point_in_time_recovery {
    enabled = var.enable_pitr
  }

  server_side_encryption {
    enabled = true
  }

  # Keep audit logs for 7 years (accounting requirement)
  # Managed by application-level TTL or archival job
  ttl {
    attribute_name = "archiveAt"
    enabled        = true
  }

  tags = { Name = "${local.name_prefix}-audit-log" }
}
