package com.nextgen.gameaggregator.vendor.digitain.api.rollback;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class RollbackResponse {
    private Integer err;
    private BigDecimal bln;
    private String pid;
    private String rid;
    private String otxid;

}
