package com.nextgen.gameaggregator.vendor.wazdan.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @NotNull
    private Integer status;

    private String errorMessage;

    @NotNull
    @Valid
    private Funds funds;

    @Valid
    private Message message;

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
    public static class Message {

        @NotNull
        private Integer type;

        @NotNull
        private String text;
    }
}
