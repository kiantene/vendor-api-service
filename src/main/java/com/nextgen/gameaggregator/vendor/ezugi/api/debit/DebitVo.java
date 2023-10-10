package com.nextgen.gameaggregator.vendor.ezugi.api.debit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.ezugi.vo.CommonVo;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DebitVo extends CommonVo {
    private String uid;
    private BigInteger roundId;
    private String transactionId;
    private BigDecimal balance;
    private String currency;
}
