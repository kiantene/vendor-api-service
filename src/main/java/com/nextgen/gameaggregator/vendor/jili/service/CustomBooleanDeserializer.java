package com.nextgen.gameaggregator.vendor.jili.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;

public class CustomBooleanDeserializer extends JsonDeserializer<Boolean> {
    @Override
    public Boolean deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String stringValue = jsonParser.getValueAsString();

        if ("true".equalsIgnoreCase(stringValue) || "false".equalsIgnoreCase(stringValue)) {
            return Boolean.parseBoolean(stringValue);
        } else if ("1".equals(stringValue)) {
            return Boolean.TRUE;
        } else if ("0".equals(stringValue)) {
            return Boolean.FALSE;
        } else {
            throw new InvalidFormatException(
                    jsonParser,
                    "Invalid boolean value: " + stringValue,
                    stringValue,
                    Boolean.class
            );
        }
    }
}