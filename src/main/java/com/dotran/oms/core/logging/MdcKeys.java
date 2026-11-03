package com.dotran.oms.core.logging;

import lombok.experimental.UtilityClass;

/**
 * Names of the MDC keys written by the OMS. Logs print them through {@code logging.pattern.correlation}.
 * Which request values are added automatically is configured in {@code app.logging.mdc.fields} ({@link MdcProperties}).
 */
@UtilityClass
public class MdcKeys {

    /** One id per unit of work: HTTP request (X-Request-Id), Kafka record or SQS message. */
    public static final String REQUEST_ID = "requestId";

    public static final String MERCHANT_ID = "merchantId";
    public static final String STORE_ID = "storeId";
    public static final String PRODUCT_ID = "productId";
    public static final String INVENTORY_ID = "inventoryId";
    public static final String COLLECTION_ID = "collectionId";
    public static final String STORE_AVAILABILITY_ID = "storeAvailabilityId";

    public static final String EVENT_ID = "eventId";
    public static final String KAFKA_TOPIC = "kafkaTopic";
    public static final String KAFKA_PARTITION = "kafkaPartition";
    public static final String KAFKA_OFFSET = "kafkaOffset";
    public static final String SQS_MESSAGE_ID = "sqsMessageId";
}
