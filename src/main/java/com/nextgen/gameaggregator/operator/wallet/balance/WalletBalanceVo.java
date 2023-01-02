package com.nextgen.gameaggregator.operator.wallet.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.vo.ResponseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper=true)
public class WalletBalanceVo extends ResponseVo {
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class ResponseData {
        private String username;
        private BigDecimal balance; // TODO: accepts only up to 4 decimals
        private String currency;
    }

    private ResponseData data;
}
