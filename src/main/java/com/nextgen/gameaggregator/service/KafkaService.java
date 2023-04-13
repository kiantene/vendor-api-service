package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.BetHistory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaService {

    private KafkaTemplate<String, String> stringKafkaTemplate;
    private KafkaTemplate<String, Object> jsonSchemaKafkaTemplate;

    public void produceBetHistory(BetHistory betHistory){
        jsonSchemaKafkaTemplate.send("topic_test_connector_4", betHistory);
    }
}
