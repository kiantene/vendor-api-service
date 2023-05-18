package com.nextgen.gameaggregator.vendor.ezugi.api.debit;

import com.nextgen.gameaggregator.vendor.ezugi.vo.CommonVo;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DebitVo extends CommonVo {
    private String uid;
    private String roundId;
    private String transactionId;
    private Double balance;
    private String currency;
}
