package com.nextgen.gameaggregator.vendor.wazdan.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.wazdan.constant.ResponseCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SuccessResponse {

    @Builder.Default
    @NotNull
    private final Integer status = ResponseCode.SUCCESS.code;

    @NotNull
    @Valid
    private Funds funds;
    
    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Funds {
        @NotNull
        private BigDecimal balance;
    }
}

