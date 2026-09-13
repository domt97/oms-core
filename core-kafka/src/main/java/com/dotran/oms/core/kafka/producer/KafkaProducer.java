package com.dotran.oms.core.kafka.producer;

public interface KafkaProducer {

    void send(String topic, String key, Object payload);
}
