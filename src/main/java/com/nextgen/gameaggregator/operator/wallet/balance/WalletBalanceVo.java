package com.nextgen.gameaggregator.operator.wallet.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.grpc.constant.ConstantErrorMessage;
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

            @NotBlank(message = "playerUsername" + ConstantErrorMessage.NOT_BLANK)
            String playerUsername;
            @NotNull(message = "balance" + ConstantErrorMessage.NOT_NULL)
            @Digits(integer = 12, fraction = 4, message = "balance" + ConstantErrorMessage.DIGITS_12_4)
            BigDecimal balance = BigDecimal.ZERO;
            @NotBlank(message = "currency" + ConstantErrorMessage.NOT_BLANK)
            @Size(min = 3, max = 3, message = "currency" + ConstantErrorMessage.SIZE_MIN_MAX + " 3 and 3")
            String currency;
        }

        Data data;
    }


    @NotNull(message = "status" + ConstantErrorMessage.NOT_NULL)
    boolean status;
    @NotBlank(message = "traceId" + ConstantErrorMessage.NOT_BLANK)
    @Size(min = 36, max = 36, message = "traceId" + ConstantErrorMessage.SIZE_MIN_MAX + " 36 and 36")
    String traceId;

    @NotNull(message = "errorMessage" + ConstantErrorMessage.NOT_NULL)
    String errorMessage;

    @NotNull(message = "response" + ConstantErrorMessage.NOT_NULL)
    Response response;
}
