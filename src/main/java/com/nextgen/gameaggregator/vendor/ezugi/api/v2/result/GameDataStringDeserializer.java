package com.nextgen.gameaggregator.vendor.ezugi.api.v2.result;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class GameDataStringDeserializer extends JsonDeserializer<BetResultRequest.GameDataString> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public BetResultRequest.GameDataString deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String raw = p.getValueAsString(); // vendor sends gameDataString as a JSON string
        if (raw == null || raw.isEmpty()) {
            return null;
        }

        try {
            return mapper.readValue(raw, BetResultRequest.GameDataString.class);
        } catch (Exception e) {
            return null;
        }
    }
}
