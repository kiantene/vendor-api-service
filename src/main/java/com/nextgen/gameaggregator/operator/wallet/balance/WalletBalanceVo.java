package com.nextgen.gameaggregator.operator.wallet.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.vo.ResponseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper=true)
public class WalletBalanceVo extends ResponseVo {
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class ResponseData {
        @NotBlank(message = "username can not be blank")
        private String username;
        @NotNull(message = "balance can not be blank")
       // @Digits(integer = 8, fraction = 4)
        private BigDecimal balance;
        @NotBlank( message = "min 3 and max 10  characters")
        @Size(min = 3, max = 10,  message = "min 3 and max 10  characters")
        private String currency;

    }

    @Valid
    private ResponseData data;
}
