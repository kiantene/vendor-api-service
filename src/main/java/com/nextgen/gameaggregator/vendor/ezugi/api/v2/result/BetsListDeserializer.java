package com.nextgen.gameaggregator.vendor.ezugi.api.v2.result;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BetsListDeserializer extends JsonDeserializer<List<BetResultRequest.Bet>> {
    
    @Override
    public List<BetResultRequest.Bet> deserialize(JsonParser p, DeserializationContext ctxt) 
            throws IOException {
        
        List<BetResultRequest.Bet> betsList = new ArrayList<>();
        JsonToken currentToken = p.getCurrentToken();
        
        if (currentToken == JsonToken.START_ARRAY) {
            // Handle array format
            JsonNode arrayNode = p.getCodec().readTree(p);
            for (JsonNode betNode : arrayNode) {
                BetResultRequest.Bet bet = new BetResultRequest.Bet();
                bet.setBetName(betNode.get("BetName").asText());
                bet.setBetAmount(new BigDecimal(betNode.get("BetAmount").asText()));
                betsList.add(bet);
            }
        } else if (currentToken == JsonToken.START_OBJECT) {
            // Handle object format
            JsonNode objectNode = p.getCodec().readTree(p);
            objectNode.fields().forEachRemaining(entry -> {
                BetResultRequest.Bet bet = new BetResultRequest.Bet();
                bet.setBetName(entry.getKey());
                bet.setBetAmount(new BigDecimal(entry.getValue().asText()));
                betsList.add(bet);
            });
        }
        
        return betsList;
    }
}
