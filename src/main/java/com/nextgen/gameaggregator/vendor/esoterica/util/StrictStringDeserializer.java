package com.nextgen.gameaggregator.vendor.esoterica.util;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.nextgen.gameaggregator.util.ValidationUtils;

import java.io.IOException;

public class StrictStringDeserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        // Reject non-string value (e.g., numbers like 123)
        if (jsonParser.currentToken() != JsonToken.VALUE_STRING) {
            return null;
        }

        String value = jsonParser.getValueAsString();

        if (!value.matches(ValidationUtils.ALPHANUMERIC_DASH_REGEX)) {
            return null;
        }

        return value;
    }
}
