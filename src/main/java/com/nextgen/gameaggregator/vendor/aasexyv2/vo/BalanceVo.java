package com.nextgen.gameaggregator.vendor.aasexyv2.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Objects;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BalanceVo {
    private BigDecimal balance;
    private Long timestamp;

    public BalanceVo(BigDecimal balance, Long timestamp) {
        this.balance = balance;
        this.timestamp = Objects.requireNonNullElseGet(timestamp, System::currentTimeMillis);
    }
}
