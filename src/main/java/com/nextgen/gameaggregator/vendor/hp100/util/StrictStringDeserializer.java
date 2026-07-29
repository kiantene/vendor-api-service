package com.nextgen.gameaggregator.vendor.hp100.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class StrictStringDeserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() != JsonToken.VALUE_STRING) { // only strings are allowed
            return null;// to trigger validation error
        }
        String value = p.getText().trim();
        if (!value.matches("^\\d+(\\.\\d+)?$")) {// regex to match positive number with at most 2 decimal places
            return null;// to trigger validation error
        }
        return value;
    }
}
