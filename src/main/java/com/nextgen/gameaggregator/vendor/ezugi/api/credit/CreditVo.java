package com.nextgen.gameaggregator.vendor.ezugi.api.credit;

import com.nextgen.gameaggregator.vendor.ezugi.vo.CommonVo;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreditVo extends CommonVo {
    private String uid;
    private String roundId;
    private String transactionId;
    private BigDecimal balance;
    private String currency;
}
