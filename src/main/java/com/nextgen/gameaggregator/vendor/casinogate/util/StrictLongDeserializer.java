package com.nextgen.gameaggregator.vendor.casinogate.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import java.io.IOException;

public class StrictLongDeserializer extends JsonDeserializer<Long> {
    @Override
    public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            throw MismatchedInputException.from(
                    parser,
                    Long.class,
                    "Amount must be an integer JSON number"
            );
        }

        return parser.getLongValue();
    }
}
