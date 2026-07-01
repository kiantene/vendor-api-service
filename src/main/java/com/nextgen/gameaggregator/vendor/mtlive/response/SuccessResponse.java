package com.nextgen.gameaggregator.vendor.mtlive.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.mtlive.constant.ResponseCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SuccessResponse {

    @Builder.Default
    @NotNull
    private final String code = ResponseCode.SUCCESS.getCode();

    @Builder.Default
    @NotNull
    private final String message = ResponseCode.SUCCESS.getMessage();

    private long timestamp;

    @NotNull
    @Valid
    private final Data data;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Data {

        private final String bet_sn;

        @NotNull
        private final BigDecimal balance;
    }
}

