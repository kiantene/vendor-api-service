package com.nextgen.gameaggregator.vendor.bglive.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Objects;

@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ResultVo {
    private Long userId;
    private String sn;
    private BigDecimal availableAmount;
    private String tranId;
    private String orderResult;

    @JsonIgnore
    private Long timestamp;

    public ResultVo(BigDecimal availableAmount, Long timestamp) {
        this.availableAmount = availableAmount;
        this.timestamp = Objects.requireNonNullElseGet(timestamp, System::currentTimeMillis);
    }

    public ResultVo() {
    }
}