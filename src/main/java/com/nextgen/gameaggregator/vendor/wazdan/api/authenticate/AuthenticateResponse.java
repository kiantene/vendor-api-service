package com.nextgen.gameaggregator.vendor.wazdan.api.authenticate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticateResponse {

    @NotNull
    private Integer status;

    @NotNull
    @Valid
    private User user;

    @NotNull
    @Valid
    private Funds funds;

    @Valid
    private Uk uk;

    @Valid
    private Se se;

    @Valid
    private Message message;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class User {
        @NotNull
        private String id;

        @NotNull
        private String currency;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Funds {
        @NotNull
        private BigDecimal balance;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Uk {
        @NotNull
        private Integer interval;

        @NotNull
        private String transactionUrl;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Se {
        @NotNull
        private Integer sessionTime;

        @NotNull
        private Double sessionWon;

        @NotNull
        private Double sessionLost;

        @NotNull
        private String riskUrl;

        @JsonProperty("gamestopUrl")
        private String gameStopUrl;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message {
        @NotNull
        private Integer type;

        @NotNull
        private String text;
    }
}