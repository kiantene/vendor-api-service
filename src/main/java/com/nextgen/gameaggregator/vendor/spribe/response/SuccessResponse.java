package com.nextgen.gameaggregator.vendor.spribe.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
public class SuccessResponse {
    private final Integer code;
    private final String message;
    private final Data data;

    public SuccessResponse(Data data) {
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

        private String currency;

        @JsonProperty("operator_tx_id")
        private String operatorTxId;

        @JsonProperty("new_balance")
        private BigDecimal newBalance;

        @JsonProperty("old_balance")
        private BigDecimal oldBalance;

        private String provider;

        @JsonProperty("provider_tx_id")
        private String providerTxId;
    }
}
