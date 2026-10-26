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

# Merchants live in Postgres (stores-service, table merchants); no DynamoDB table is needed.
echo "Setup completed successfully"
