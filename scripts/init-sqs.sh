#!/bin/bash
# ╔══════════════════════════════════════════════════════════════════════╗
# ║              SQS (LocalStack) Initialization Script                  ║
# ║         Creates queues for asynchronous order processing             ║
# ╚══════════════════════════════════════════════════════════════════════╝

set -e

echo "🚀 Starting SQS initialization..."

SQS_ENDPOINT="http://localstack:4566"
AWS_REGION="us-east-1"

# Wait for LocalStack to be ready (with timeout)
echo "⏳ Waiting for LocalStack SQS to be ready..."
MAX_RETRIES=30
RETRY_COUNT=0

until aws sqs list-queues --endpoint-url $SQS_ENDPOINT --region $AWS_REGION > /dev/null 2>&1; do
  RETRY_COUNT=$((RETRY_COUNT+1))
  if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
    echo "❌ LocalStack failed to start after ${MAX_RETRIES} attempts"
    echo "Last error output:"
    aws sqs list-queues --endpoint-url $SQS_ENDPOINT --region $AWS_REGION 2>&1 || true
    exit 1
  fi
  echo "Waiting for LocalStack... (attempt $RETRY_COUNT/$MAX_RETRIES)"
  sleep 2
done

echo "✅ LocalStack SQS is ready!"

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# Create Dead Letter Queue (DLQ) - FIFO
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo "📬 Creating Dead Letter Queue FIFO (order-processing-dlq.fifo)..."

# Try to create the FIFO queue, and if it exists, get its URL
if ! DLQ_URL=$(aws sqs create-queue \
  --queue-name order-processing-dlq.fifo \
  --attributes FifoQueue=true,MessageRetentionPeriod=1209600,ContentBasedDeduplication=true \
  --endpoint-url $SQS_ENDPOINT \
  --region $AWS_REGION \
  --output text \
  --query 'QueueUrl' 2>&1); then
  echo "Queue might already exist, trying to get URL..."
  DLQ_URL=$(aws sqs get-queue-url \
    --queue-name order-processing-dlq.fifo \
    --endpoint-url $SQS_ENDPOINT \
    --region $AWS_REGION \
    --output text \
    --query 'QueueUrl')
fi

if [ -z "$DLQ_URL" ]; then
  echo "❌ Failed to create or get DLQ URL"
  exit 1
fi

echo "✅ DLQ FIFO created/exists: $DLQ_URL"

# Get DLQ ARN
DLQ_ARN=$(aws sqs get-queue-attributes \
  --queue-url $DLQ_URL \
  --attribute-names QueueArn \
  --endpoint-url $SQS_ENDPOINT \
  --region $AWS_REGION \
  --output text \
  --query 'Attributes.QueueArn')

echo "📋 DLQ ARN: $DLQ_ARN"

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# Create Main Queue with DLQ configuration - FIFO
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo "📬 Creating Main Queue FIFO (order-processing-queue.fifo)..."

# First, create the FIFO queue without RedrivePolicy
MAIN_QUEUE_URL=$(aws sqs create-queue \
  --queue-name order-processing-queue.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=true,MessageRetentionPeriod=345600,VisibilityTimeout=30,ReceiveMessageWaitTimeSeconds=20 \
  --endpoint-url $SQS_ENDPOINT \
  --region $AWS_REGION \
  --output text \
  --query 'QueueUrl' 2>&1 || \
  aws sqs get-queue-url \
    --queue-name order-processing-queue.fifo \
    --endpoint-url $SQS_ENDPOINT \
    --region $AWS_REGION \
    --output text \
    --query 'QueueUrl')

if [ -z "$MAIN_QUEUE_URL" ]; then
  echo "❌ Failed to create or get Main Queue URL"
  exit 1
fi

echo "✅ Main Queue FIFO created: $MAIN_QUEUE_URL"

# Now configure the RedrivePolicy
echo "📋 Configuring Dead Letter Queue policy..."

aws sqs set-queue-attributes \
  --queue-url "$MAIN_QUEUE_URL" \
  --attributes "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"${DLQ_ARN}\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"}" \
  --endpoint-url $SQS_ENDPOINT \
  --region $AWS_REGION

echo "✅ DLQ policy configured"

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# Display Queue Configuration
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo ""
echo "📋 Queue Configuration:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
aws sqs get-queue-attributes \
  --queue-url $MAIN_QUEUE_URL \
  --attribute-names All \
  --endpoint-url $SQS_ENDPOINT \
  --region $AWS_REGION \
  --output table

echo ""
echo "📋 Current SQS queues:"
aws sqs list-queues \
  --endpoint-url $SQS_ENDPOINT \
  --region $AWS_REGION \
  --output table

echo ""
echo "🎉 SQS initialization completed successfully!"
echo ""
echo "Queue URLs:"
echo "  Main Queue: $MAIN_QUEUE_URL"
echo "  DLQ:        $DLQ_URL"
