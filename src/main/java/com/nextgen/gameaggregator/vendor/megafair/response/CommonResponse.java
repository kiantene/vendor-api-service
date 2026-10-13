package com.nextgen.gameaggregator.vendor.megafair.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@JacksonXmlRootElement(localName = "EXTSYSTEM")
public class CommonResponse {

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
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class ResponseData {
        @JacksonXmlProperty(localName = "RESULT")
        private String result;

        @JacksonXmlProperty(localName = "EXTSYSTEMTRANSACTIONID")
        private String extSystemTransactionId;

        @JacksonXmlProperty(localName = "BALANCE")
        private BigDecimal balance;
    }
}
