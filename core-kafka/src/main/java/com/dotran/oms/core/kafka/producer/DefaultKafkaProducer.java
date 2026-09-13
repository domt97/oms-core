package com.dotran.oms.core.kafka.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultKafkaProducer implements KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void send(String topic, String key, Object payload) {
        kafkaTemplate.send(topic, key, payload);
    }
}
