package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.BetHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaService {

    @Autowired
    private KafkaTemplate<String, String> stringKafkaTemplate;
    @Autowired
    private KafkaTemplate<String, Object> jsonSchemaKafkaTemplate;

    public void produceBetHistory(BetHistory betHistory){
        jsonSchemaKafkaTemplate.send("topic_16", betHistory);
    }
}
