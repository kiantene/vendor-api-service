package com.nextgen.gameaggregator.vendor.bglive.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ResultVo {
    private Long userId;
    private String sn;
    private BigDecimal availableAmount;
    private String tranId;
    private String orderResult;

}