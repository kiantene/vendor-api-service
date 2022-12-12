package com.nextgen.gameaggregator.vendor.api.pgsoft.v2_4_4.balance;

import com.nextgen.gameaggregator.vendor.api.pgsoft.component.constant.Constant;
import com.nextgen.sas.core.web.wrapper.WebRequestWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

import java.math.BigDecimal;

@RestController
@RequestScope
@RequestMapping(path = Constant.WEB_ACTION)
public class BalanceAction {


    @PostMapping(path = Constant.ACTION_BALANCE)
    public BalanceVo test(BalanceDto dto, WebRequestWrapper request) {
        System.out.println("========================================================================================");
        System.out.println(dto.getPlayerName());
        System.out.println(dto.getGameId());
        System.out.println(dto.getOperatorPlayerSession());

        BalanceVo haha = new BalanceVo();

        haha.setUpdatedTime(1020202020L);
        haha.setBalanceAmount(BigDecimal.valueOf(22.3));
        haha.setCurrencyCode("Dddd");
//        System.out.println(haha.getCurrencyCode());
//        System.out.println(haha.getBalanceAmount());
//        System.out.println(haha.getUpdatedTime());

        return haha;
    }

}
