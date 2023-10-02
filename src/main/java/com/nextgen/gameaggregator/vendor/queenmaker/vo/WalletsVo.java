package com.nextgen.gameaggregator.vendor.queenmaker.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WalletsVo {
    private String code = "MainWallet";
    private BigDecimal bal;
    private String cur;
    private String name; // optional
    private String desc; // optional
}