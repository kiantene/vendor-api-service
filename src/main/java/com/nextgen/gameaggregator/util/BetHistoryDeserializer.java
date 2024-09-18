package com.nextgen.gameaggregator.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

public class BetHistoryDeserializer extends StdDeserializer<com.nextgen.gameaggregator.entity.warehouse.BetHistory> {

    public BetHistoryDeserializer() {
        this(null);
    }

    public BetHistoryDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public com.nextgen.gameaggregator.entity.warehouse.BetHistory deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        ObjectNode root = mapper.readTree(jp);

        // Print or log any unknown fields that don't match BetHistory
        root.fieldNames().forEachRemaining(fieldName -> {
            if (!isBetHistoryField(fieldName)) {
                System.err.println("Unknown field in JSON: " + fieldName);
            }
        });

        // Deserialize the actual BetHistory object
        return mapper.treeToValue(root, com.nextgen.gameaggregator.entity.warehouse.BetHistory.class);
    }

    private boolean isBetHistoryField(String fieldName) {
        // List all the fields in your BetHistory class
        return fieldName.equals("id")
                || fieldName.equals("externalTransactionId")
                || fieldName.equals("vendorBetId")
                || fieldName.equals("roundId")
                || fieldName.equals("vendorGameId")
                || fieldName.equals("gameCode")
                || fieldName.equals("vendorPlayerId")
                || fieldName.equals("vendorPlayerUsername")
                || fieldName.equals("vendorId")
                || fieldName.equals("vendorCode")
                || fieldName.equals("vendorLineId")
                || fieldName.equals("agentPlayerId")
                || fieldName.equals("agentPlayerUsername")
                || fieldName.equals("agentId")
                || fieldName.equals("operatorStatus")
                || fieldName.equals("gameCategoryId")
                || fieldName.equals("gameCategoryCode")
                || fieldName.equals("currencyId")
                || fieldName.equals("currencyCode")
                || fieldName.equals("betAmount")
                || fieldName.equals("winAmount")
                || fieldName.equals("winLoss")
                || fieldName.equals("effectiveTurnover")
                || fieldName.equals("jackpotAmount")
                || fieldName.equals("resultType")
                || fieldName.equals("betType")
                || fieldName.equals("isFreespin")
                || fieldName.equals("resettleNum")
                || fieldName.equals("status")
                || fieldName.equals("gameSessionToken")
                || fieldName.equals("vendorBetTime")
                || fieldName.equals("vendorSettleTime")
                || fieldName.equals("resultTime");
    }
}
