package com.nextgen.gameaggregator.vendor.spadegaming.api.transfer;

import com.nextgen.gameaggregator.vendor.spadegaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class TransferAction {
    @PostMapping(path = EndPoints.TRANSFER)
    public TransferVo transfer(HttpServletRequest request) {
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
