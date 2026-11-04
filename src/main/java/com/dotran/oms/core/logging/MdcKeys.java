package com.dotran.oms.core.logging;

import lombok.experimental.UtilityClass;

/**
 * Names of the MDC keys written by the OMS. Logs print them through {@code logging.pattern.correlation}.
 * Which request values are added automatically is configured in {@code app.logging.mdc.fields} ({@link MdcProperties}).
 */
@UtilityClass
public class MdcKeys {

    /**
     * Correlation id of one flow: the HTTP request's X-Request-Id (or a generated UUID), carried to Kafka consumers in
     * the {@code correlationContext} header ({@link MdcPropagation}); for a record without it, the record's
     * {@code topic-partition-offset}; for an SQS message, its message id.
     */
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
