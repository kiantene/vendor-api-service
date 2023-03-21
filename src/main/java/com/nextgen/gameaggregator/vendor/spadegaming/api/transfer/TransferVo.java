package com.nextgen.gameaggregator.vendor.spadegaming.api.transfer;

import lombok.Data;
import java.math.BigDecimal;
import com.nextgen.gameaggregator.vendor.spadegaming.vo.ResponseVo;

@Data
public class TransferVo extends ResponseVo {
    private String transferId;
    private String merchantTxId;
    private String acctId;
    private BigDecimal balance;
}
