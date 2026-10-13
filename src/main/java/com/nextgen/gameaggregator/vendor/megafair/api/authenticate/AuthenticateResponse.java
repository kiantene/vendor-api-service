package com.nextgen.gameaggregator.vendor.megafair.api.authenticate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@JacksonXmlRootElement(localName = "EXTSYSTEM")
public class AuthenticateResponse {
    @JacksonXmlProperty(localName = "REQUEST")
    private RequestData request;

    @JacksonXmlProperty(localName = "TIME")
    private String time;

    @JacksonXmlProperty(localName = "RESPONSE")
    private ResponseData response;

    @Data
    @Builder
    public static class RequestData {
        @JacksonXmlProperty(localName = "TOKEN")
        private String token;

        @JacksonXmlProperty(localName = "GAMESESSIONID")
        private String gameSessionId;

        @JacksonXmlProperty(localName = "HASH")
        private String hash;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class ResponseData {
        @JacksonXmlProperty(localName = "RESULT")
        private String result;

        @JacksonXmlProperty(localName = "USERID")
        private String userId;

        @JacksonXmlProperty(localName = "CURRENCY")
        private String currency;

        @JacksonXmlProperty(localName = "BALANCE")
        private BigDecimal balance;
    }
}
