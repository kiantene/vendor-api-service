package com.nextgen.gameaggregator.eventing.listeners;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.eventing.core.EventListener;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BetEventListener implements EventListener<BetEvent> {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public void onEvent(BetEvent event) {
        BetHistory betHistory = event.getBetHistory();
        Gson gson = new GsonBuilder().create();

        kafkaTemplate.send("topic_seamless_bet_transformation", betHistory.getId(), gson.toJson(betHistory));
    }
}
