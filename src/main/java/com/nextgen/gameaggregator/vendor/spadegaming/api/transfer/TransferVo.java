package com.nextgen.gameaggregator.vendor.spadegaming.api.transfer;

import java.math.BigDecimal;

import com.nextgen.gameaggregator.vendor.spadegaming.vo.ResponseVo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper=true)
@Data
public class TransferVo extends ResponseVo {
    private String transferId;
    private String merchantTxId;
    private String acctId;
    private BigDecimal balance;
}
