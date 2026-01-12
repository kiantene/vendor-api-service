package com.nextgen.gameaggregator.service.data.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.engine.game.round.GameRoundEndedEvent;
import com.nextgen.gameaggregator.data.kafka.constant.KafkaConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameRoundProducer {
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> stringKafkaTemplate;

    public void publishRoundEnded(GameRoundEndedEvent event, String settleType) {
        try {
            stringKafkaTemplate.send(
                    KafkaConstant.TOPIC_PROCESS_ROUND_ENDED,
                    event.agentPlayerUsername(),
                    objectMapper.writeValueAsString(GameRoundEndedMessage.ofEvent(event, settleType))
            );
        } catch (Exception e) {
            log.error("Error sending roundEnded to Kafka topic: " + e.getMessage() + " -> roundId = " + event.roundId() + "& vendorPlayerId = " + event.vendorPlayerId());
        }
    }
}
