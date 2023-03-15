package com.nextgen.gameaggregator.vendor.spadegaming.service;

import com.nextgen.gameaggregator.vendor.spadegaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.spadegaming.vo.AcctInfoVo;
import com.nextgen.gameaggregator.vendor.spadegaming.vo.AuthBalanceVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BalanceService {
    public AuthBalanceVo balance() {

        AuthBalanceVo authBalanceVo = new AuthBalanceVo();
        AcctInfoVo acctInfoVo = new AcctInfoVo();

        acctInfoVo.setAccId("TESTPLAYER1");
        acctInfoVo.setBalance(BigDecimal.ZERO);
        acctInfoVo.setUserName("TESTPlayer1");
        acctInfoVo.setCurrency("USD");
        acctInfoVo.setSiteId("SITE_USD");

        authBalanceVo.setAcctInfo(acctInfoVo);
        authBalanceVo.setMerchantCode(Credentials.MERCHANT_CODE);
        authBalanceVo.setMsg(ResponseCode.RESPONSE_DESCRIPTION.get(ResponseCode.SUCCESS));
        authBalanceVo.setCode(ResponseCode.SUCCESS);
        authBalanceVo.setSerialNo("20120722224255982841");

        return authBalanceVo;
    }
}
