package com.nextgen.gameaggregator.vendor.ezugi.api.rollback;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.ezugi.vo.CommonVo;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RollbackVo extends CommonVo {
    private String uid;
    private String roundId;
    private String transactionId;
    private BigDecimal balance;
    private String currency;
}
