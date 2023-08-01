package com.nextgen.gameaggregator.vendor.ezugi.api.rollback;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.ezugi.vo.CommonVo;
import lombok.Data;

import java.math.BigInteger;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RollbackVo extends CommonVo {
    private String uid;
    private BigInteger roundId;
    private String transactionId;
    private Double balance;
    private String currency;
}
