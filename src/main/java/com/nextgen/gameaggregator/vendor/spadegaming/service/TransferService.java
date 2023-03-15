package com.nextgen.gameaggregator.vendor.spadegaming.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.nextgen.gameaggregator.vendor.spadegaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.spadegaming.vo.TransferVo;

@Service
public class TransferService {
    public TransferVo transfer() {
        TransferVo transferVo = new TransferVo();

        transferVo.setTransferId("0ab9bdca06c14811b24653468e609838");
        transferVo.setMerchantCode(Credentials.MERCHANT_CODE);
        transferVo.setMerchantTxId("20130813014319279367");
        transferVo.setAcctId("Test10001");
        transferVo.setBalance(BigDecimal.valueOf(1050));
        transferVo.setMsg(ResponseCode.RESPONSE_DESCRIPTION.get(ResponseCode.SUCCESS));
        transferVo.setCode(ResponseCode.SUCCESS);
        transferVo.setSerialNo("20120722224255982841");

        return transferVo;
    }
}
