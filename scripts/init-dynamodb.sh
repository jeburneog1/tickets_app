#!/bin/bash
# ╔══════════════════════════════════════════════════════════════════════╗
# ║           DynamoDB Local Initialization Script                       ║
# ║         Creates tables and indexes for Tickets System                ║
# ╚══════════════════════════════════════════════════════════════════════╝

set -e

echo "🚀 Starting DynamoDB initialization..."

DYNAMODB_ENDPOINT="http://dynamodb-local:8000"
AWS_REGION="us-east-1"

# Wait for DynamoDB to be ready (with timeout)
echo "⏳ Waiting for DynamoDB to be ready..."
MAX_RETRIES=30
RETRY_COUNT=0

until aws dynamodb list-tables --endpoint-url $DYNAMODB_ENDPOINT --region $AWS_REGION 2>/dev/null; do
  RETRY_COUNT=$((RETRY_COUNT+1))
  if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
    echo "❌ DynamoDB failed to start after ${MAX_RETRIES} attempts"
    exit 1
  fi
  echo "Waiting for DynamoDB... (attempt $RETRY_COUNT/$MAX_RETRIES)"
  sleep 2
done

echo "✅ DynamoDB is ready!"

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# Create Events Table
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo "📊 Creating 'events' table..."

aws dynamodb create-table \
  --table-name events \
  --attribute-definitions \
    AttributeName=eventId,AttributeType=S \
    AttributeName=date,AttributeType=S \
  --key-schema \
    AttributeName=eventId,KeyType=HASH \
  --provisioned-throughput \
    ReadCapacityUnits=5,WriteCapacityUnits=5 \
  --global-secondary-indexes \
    "[{
      \"IndexName\": \"date-index\",
      \"KeySchema\": [{\"AttributeName\":\"date\",\"KeyType\":\"HASH\"}],
      \"Projection\": {\"ProjectionType\":\"ALL\"},
      \"ProvisionedThroughput\": {\"ReadCapacityUnits\":5,\"WriteCapacityUnits\":5}
    }]" \
  --endpoint-url $DYNAMODB_ENDPOINT \
  --region $AWS_REGION \
  2>/dev/null || echo "⚠️  Table 'events' already exists"

echo "✅ Table 'events' ready!"

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# Create Tickets Table
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo "📊 Creating 'tickets' table..."

aws dynamodb create-table \
  --table-name tickets \
  --attribute-definitions \
    AttributeName=ticketId,AttributeType=S \
    AttributeName=eventId,AttributeType=S \
    AttributeName=status,AttributeType=S \
    AttributeName=customerId,AttributeType=S \
    AttributeName=orderId,AttributeType=S \
  --key-schema \
    AttributeName=ticketId,KeyType=HASH \
  --provisioned-throughput \
    ReadCapacityUnits=10,WriteCapacityUnits=10 \
  --global-secondary-indexes \
    "[{
      \"IndexName\": \"eventId-status-index\",
      \"KeySchema\": [
        {\"AttributeName\":\"eventId\",\"KeyType\":\"HASH\"},
        {\"AttributeName\":\"status\",\"KeyType\":\"RANGE\"}
      ],
      \"Projection\": {\"ProjectionType\":\"ALL\"},
      \"ProvisionedThroughput\": {\"ReadCapacityUnits\":10,\"WriteCapacityUnits\":10}
    },
    {
      \"IndexName\": \"customerId-index\",
      \"KeySchema\": [{\"AttributeName\":\"customerId\",\"KeyType\":\"HASH\"}],
      \"Projection\": {\"ProjectionType\":\"ALL\"},
      \"ProvisionedThroughput\": {\"ReadCapacityUnits\":5,\"WriteCapacityUnits\":5}
    },
    {
      \"IndexName\": \"orderId-index\",
      \"KeySchema\": [{\"AttributeName\":\"orderId\",\"KeyType\":\"HASH\"}],
      \"Projection\": {\"ProjectionType\":\"ALL\"},
      \"ProvisionedThroughput\": {\"ReadCapacityUnits\":5,\"WriteCapacityUnits\":5}
    }]" \
  --endpoint-url $DYNAMODB_ENDPOINT \
  --region $AWS_REGION \
  2>/dev/null || echo "⚠️  Table 'tickets' already exists"

echo "✅ Table 'tickets' ready!"

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# Create Orders Table
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo "📊 Creating 'orders' table..."

aws dynamodb create-table \
  --table-name orders \
  --attribute-definitions \
    AttributeName=orderId,AttributeType=S \
    AttributeName=customerId,AttributeType=S \
    AttributeName=eventId,AttributeType=S \
    AttributeName=status,AttributeType=S \
  --key-schema \
    AttributeName=orderId,KeyType=HASH \
  --provisioned-throughput \
    ReadCapacityUnits=10,WriteCapacityUnits=10 \
  --global-secondary-indexes \
    "[{
      \"IndexName\": \"customerId-index\",
      \"KeySchema\": [{\"AttributeName\":\"customerId\",\"KeyType\":\"HASH\"}],
      \"Projection\": {\"ProjectionType\":\"ALL\"},
      \"ProvisionedThroughput\": {\"ReadCapacityUnits\":10,\"WriteCapacityUnits\":10}
    },
    {
      \"IndexName\": \"eventId-index\",
      \"KeySchema\": [{\"AttributeName\":\"eventId\",\"KeyType\":\"HASH\"}],
      \"Projection\": {\"ProjectionType\":\"ALL\"},
      \"ProvisionedThroughput\": {\"ReadCapacityUnits\":5,\"WriteCapacityUnits\":5}
    },
    {
      \"IndexName\": \"status-index\",
      \"KeySchema\": [{\"AttributeName\":\"status\",\"KeyType\":\"HASH\"}],
      \"Projection\": {\"ProjectionType\":\"ALL\"},
      \"ProvisionedThroughput\": {\"ReadCapacityUnits\":5,\"WriteCapacityUnits\":5}
    }]" \
  --endpoint-url $DYNAMODB_ENDPOINT \
  --region $AWS_REGION \
  2>/dev/null || echo "⚠️  Table 'orders' already exists"

echo "✅ Table 'orders' ready!"

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# List all tables
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo ""
echo "📋 Current DynamoDB tables:"
aws dynamodb list-tables \
  --endpoint-url $DYNAMODB_ENDPOINT \
  --region $AWS_REGION \
  --output table

echo ""
echo "🎉 DynamoDB initialization completed successfully!"
