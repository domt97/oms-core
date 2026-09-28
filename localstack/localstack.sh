#!/bin/sh

profile=localdev
region=eu-west-1
endpoint=http://localhost:4566

declare -a buckets=(
		${profile}-store-products
)

declare -a queues=(
  ${profile}-product-created-event
)

echo "Creating S3 Buckets ..."
for bucket in "${buckets[@]}"
do
	aws --endpoint-url=$endpoint s3 mb s3://$bucket
done

echo "Creating SQS Queues ..."
for queue in "${queues[@]}"
do
  aws \
    --endpoint-url=$endpoint \
    --region=$region \
    sqs create-queue \
    --queue-name "$queue"
done

echo "Creating EventBridge Event Bus ..."

aws \
  --endpoint-url=$endpoint \
  --region=$region \
  events create-event-bus \
  --name ${profile}-expire-reservation-event-bus

echo "Creating DynamoDB table: ${profile}_tenant_info..."

aws dynamodb create-table \
  --endpoint-url=$endpoint \
  --region ${region} \
  --table-name ${profile}_tenant_info \
  --attribute-definitions \
    AttributeName=pk,AttributeType=S \
  --key-schema \
    AttributeName=pk,KeyType=HASH \
  --provisioned-throughput \
    ReadCapacityUnits=3,WriteCapacityUnits=3 2>&1 | grep -v "ResourceInUseException"

echo "Table ready (created or already exists)"
sleep 2

echo "Inserting test data..."

aws dynamodb put-item \
  --endpoint-url=$endpoint \
  --region ${region} \
  --table-name ${profile}_tenant_info \
  --item '{
    "pk": {
      "S": "7b7fdc98-2c7c-45a9-bc1c-cfd4d52a55b8"
    },
    "code": {
      "S": "DTVN"
    },
    "name": {
      "S": "Do Tran Coffee Vietnam"
    },
    "status": {
      "S": "ACTIVE"
    },
    "settings": {
      "M": {
        "timezone": {
          "S": "Asia/Ho_Chi_Minh"
        },
        "currency": {
          "S": "VND"
        },
        "locale": {
          "S": "vi-VN"
        }
      }
    },
    "createdAt": {
      "S": "2026-08-02T15:00:00Z"
    },
    "updatedAt": {
      "S": "2026-08-02T15:00:00Z"
    }
  }'

if [ $? -ne 0 ]; then
  echo "ERROR: Failed to insert data"
  exit 1
fi

echo "Setup completed successfully"