package com.nextgen.gameaggregator.vendor.spadegaming.service;

import com.nextgen.gameaggregator.vendor.spadegaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.spadegaming.vo.AcctInfoVo;
import com.nextgen.gameaggregator.vendor.spadegaming.vo.AuthBalanceVo;
import com.nextgen.gameaggregator.vendor.spadegaming.dto.AuthenticateDto;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AuthenticateService {
    public AuthBalanceVo authenticate(AuthenticateDto dto) {

        AuthBalanceVo authBalanceVo = new AuthBalanceVo();
        AcctInfoVo acctInfoVo = new AcctInfoVo();

        acctInfoVo.setAcctId(dto.getAcctId());
        acctInfoVo.setBalance(BigDecimal.ZERO);
        acctInfoVo.setUserName(dto.getAcctId());
        acctInfoVo.setCurrency("CNY");
        acctInfoVo.setSiteId("SITE_CNY");

        authBalanceVo.setAcctInfo(acctInfoVo);
        authBalanceVo.setMerchantCode(Credentials.MERCHANT_CODE);
        authBalanceVo.setMsg(ResponseCode.RESPONSE_DESCRIPTION.get(ResponseCode.SUCCESS));
        authBalanceVo.setCode(ResponseCode.SUCCESS);
        authBalanceVo.setSerialNo(dto.getSerialNo());

        return authBalanceVo;
    }
}
