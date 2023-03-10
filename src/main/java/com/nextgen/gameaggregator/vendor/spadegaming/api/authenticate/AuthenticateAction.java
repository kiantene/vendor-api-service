package com.nextgen.gameaggregator.vendor.spadegaming.api.authenticate;

import com.nextgen.gameaggregator.vendor.spadegaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.spadegaming.vo.AuthBalanceVo;
import com.nextgen.gameaggregator.vendor.spadegaming.vo.AcctInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class AuthenticateAction {

    @PostMapping(path = EndPoints.AUTHENTICATE)
    public AuthBalanceVo authenticate(HttpServletRequest request) {

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