package com.nextgen.gameaggregator.vendor.spribe.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
public class BalanceResponse {
    private final Integer code;
    private final String message;
    private final Data data;

    public BalanceResponse(Data data) {
        this.code = 200;
        this.message = "Success";
        this.data = data;
    }

    @Getter
    @Setter
    @Builder
    public static class Data {
        @JsonProperty("user_id")
        private String userId;
        private String username;
        private String currency;
        private BigDecimal balance;
    }
}
