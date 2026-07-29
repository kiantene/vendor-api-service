package com.nextgen.gameaggregator.service.data.producer.endround;

import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.service.KafkaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoundEndedTriggerProducer {
    private final KafkaService kafkaService;

    public void publishEndRound(GameRound round) {
        kafkaService.produceRoundEndedTrigger(RoundEndedTriggerMessage.of(round), round.getUsername());
    }
}