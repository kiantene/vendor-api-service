package com.nextgen.gameaggregator.util.customvalidation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.math.BigDecimal;

public class BigDecimalDeserializer extends StdDeserializer<BigDecimal> {

    public BigDecimalDeserializer() {
        this(null);
    }

    public BigDecimalDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public BigDecimal deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        String value = node.asText();

        if (value == null || value.trim().isEmpty()) {
            return null; // Treat empty string as null
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            // Handle the invalid format gracefully
            throw ctxt.weirdStringException(value, BigDecimal.class, "Invalid BigDecimal value");
        }
    }
}
