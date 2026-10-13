package com.nextgen.gameaggregator.vendor.megafair.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "EXTSYSTEM")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ErrorResponse {

    @JacksonXmlProperty(localName = "REQUEST")
    private RequestData request;

    @JacksonXmlProperty(localName = "TIME")
    private String time;

    @JacksonXmlProperty(localName = "RESPONSE")
    private ResponseData response;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class RequestData {
        @JacksonXmlProperty(localName = "USERID")
        private String userId;

        @JacksonXmlProperty(localName = "BET")
        private String bet;

        @JacksonXmlProperty(localName = "WIN")
        private String win;

        @JacksonXmlProperty(localName = "ROUNDID")
        private String roundId;

        @JacksonXmlProperty(localName = "ISROUNDFINISHED")
        private String isRoundFinished;

        @JacksonXmlProperty(localName = "GAMEID")
        private String gameId;

        @JacksonXmlProperty(localName = "GAMESESSIONID")
        private String gameSessionId;

        @JacksonXmlProperty(localName = "KIND")
        private String kind;

        @JacksonXmlProperty(localName = "HASH")
        private String hash;

        @JacksonXmlProperty(localName = "TOKEN")
        private String token;

        @JacksonXmlProperty(localName = "CASINOTRANSACTIONID")
        private String casinoTransactionId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class ResponseData {

        @JacksonXmlProperty(localName = "RESULT")
        private String result;

        @JacksonXmlProperty(localName = "CODE")
        private String code;

        @JacksonXmlProperty(localName = "EXTSYSTEMTRANSACTIONID")
        private String extSystemTransactionId;

        @JacksonXmlProperty(localName = "BALANCE")
        private BigDecimal balance;
    }
}

