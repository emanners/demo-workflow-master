#!/usr/bin/env bash
set -e

export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_REGION=eu-west-1
ENDP="--endpoint-url=http://localhost:4566"

# 1) DynamoDB
aws dynamodb create-table \
  --table-name solance-workflow \
  --attribute-definitions AttributeName=eventId,AttributeType=S \
  --key-schema AttributeName=eventId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  $ENDP

# 2) SQS
aws sqs create-queue \
  --queue-name solance-workflow-queue \
  $ENDP

# grab the Queue ARN
QUEUE_URL=http://localhost:4566/000000000000/solance-workflow-queue
QUEUE_ARN=$(aws sqs get-queue-attributes \
  --queue-url $QUEUE_URL \
  --attribute-names QueueArn \
  --query "Attributes.QueueArn" --output text \
  $ENDP)

# 3) EventBridge bus & rule
aws events create-event-bus \
  --name workflow-bus \
  $ENDP

aws events put-rule \
  --name workflow-rule \
  --event-bus-name workflow-bus \
  --event-pattern '{"detail-type":[{"prefix":""}]}' \
  $ENDP

# 4) wire the rule to SQS
aws events put-targets \
  --rule workflow-rule \
  --event-bus-name workflow-bus \
  --targets "Id"="1","Arn"="$QUEUE_ARN" \
  $ENDP

# 5) Now YOU MUST allow EventBridge to send into SQS
aws sqs set-queue-attributes \
  --queue-url $QUEUE_URL \
  --attributes '{
    "Policy": "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"Service\":\"events.amazonaws.com\"},\"Action\":\"sqs:SendMessage\",\"Resource\":\"'"$QUEUE_ARN"'\",\"Condition\":{\"ArnEquals\":{\"aws:SourceArn\":\"arn:aws:events:eu-west-1:000000000000:rule/workflow-rule\"}}}]}"
  }' \
  $ENDP

echo "Local infra up and wired: DynamoDB, EventBridge → SQS queue ready"