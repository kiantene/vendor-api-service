package com.nextgen.gameaggregator.vendor.ezugi.api.rollback;

import com.nextgen.gameaggregator.vendor.ezugi.vo.CommonVo;
import lombok.Data;

@Data
public class RollbackVo extends CommonVo {
    private String uid;
    private String roundId;
    private String transactionId;
    private Double balance;
    private String currency;
}
