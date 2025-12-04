package com.nextgen.gameaggregator.vendor.ezugi.api.v2.result;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Custom deserializer for converting JSON to a Map of winning bets.
 * Handles both direct numeric values and objects containing WinAmount fields.
 */
public class WinningBetsDeserializer extends JsonDeserializer<Map<String, BigDecimal>> {
    
    @Override
    public Map<String, BigDecimal> deserialize(JsonParser p, DeserializationContext ctxt) 
            throws IOException {
        
        // Initialize map to store bet names and their corresponding amounts
        Map<String, BigDecimal> winningBets = new HashMap<>();
        
        // Parse the JSON structure into a tree for field-by-field processing
        JsonNode objectNode = p.getCodec().readTree(p);
        
        // Iterate through each field in the JSON object
        objectNode.fields().forEachRemaining(entry -> {
            String betName = entry.getKey();
            JsonNode valueNode = entry.getValue();
            
            // Handle direct numeric values
            if (valueNode.isNumber()) {
                winningBets.put(betName, new BigDecimal(valueNode.asText()));
            } 
            // Handle objects with WinAmount property
            else if (valueNode.isObject() && valueNode.has("WinAmount")) {
                winningBets.put(betName, new BigDecimal(valueNode.get("WinAmount").asText()));
            }
        });
        
        return winningBets;
    }
}
