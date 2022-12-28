package com.nextgen.gameaggregator.operator.wallet.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class WalletBalanceVo {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Response {
        @JsonIgnoreProperties(ignoreUnknown = true)
        @lombok.Data
        public static class Data {

            @NotBlank
            String playerUsername;
            @NotNull
            @Digits(integer = 12, fraction = 4, message = "balance")
            BigDecimal balance = BigDecimal.ZERO;
            @NotBlank
            @Size(min = 3, max = 3)
            String currency;
        }

        Data data;
    }

    @NotNull
    boolean status;

    @NotBlank
    @Size
    String traceId;

    @NotNull
    String errorMessage;

    @NotNull
    Response response;
}
