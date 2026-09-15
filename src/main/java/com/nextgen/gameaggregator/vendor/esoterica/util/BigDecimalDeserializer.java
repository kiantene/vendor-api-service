package com.nextgen.gameaggregator.vendor.esoterica.util;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.math.BigDecimal;

public class BigDecimalDeserializer extends JsonDeserializer<BigDecimal> {
    @Override
    public BigDecimal deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        // Reject anything that's not a number
        if (!jsonParser.currentToken().isNumeric()) {
            return null;
        }

        // Reject floating point numbers (e.g., 123.45)
        if (jsonParser.currentToken() == JsonToken.VALUE_NUMBER_FLOAT) {
            return null;
        }

         return jsonParser.getDecimalValue();
    }
}
