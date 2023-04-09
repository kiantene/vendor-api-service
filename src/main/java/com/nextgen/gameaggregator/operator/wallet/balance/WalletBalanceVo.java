package com.nextgen.gameaggregator.operator.wallet.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.vo.ResponseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper=true)
public class WalletBalanceVo extends ResponseVo {
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class ResponseData {
        @NotBlank
        private String username;
        @NotNull
       // @Digits(integer = 8, fraction = 4)
        private BigDecimal balance;
        @NotBlank
        @Size(min = 3, max = 6)
        private String currency;

    }

    @Valid
    private ResponseData data;
}
