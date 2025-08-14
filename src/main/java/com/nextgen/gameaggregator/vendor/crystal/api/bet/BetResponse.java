package com.nextgen.gameaggregator.vendor.crystal.api.bet;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class BetResponse {

    private DataVo dataVo;
    private ErrorVo errorVo;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class DataVo {
        private BigDecimal balance;
        private String actionId;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class ErrorVo {
        private String error;
    }
}