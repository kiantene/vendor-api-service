package com.nextgen.gameaggregator.vendor.api.pgsoft.v2_4_4.cashget;

import com.nextgen.gameaggregator.vendor.api.pgsoft.component.constant.Constant;
import com.nextgen.gameaggregator.vendor.api.pgsoft.component.action.AbstractAction;
import com.nextgen.sas.core.web.wrapper.WebRequestWrapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

import java.math.BigDecimal;

@RestController
@RequestScope
@RequestMapping(path = Constant.WEB_ACTION)
public class BalanceAction extends AbstractAction {

    @PostMapping(path = Constant.ACTION_CASH_GET)
    public BalanceActionVo test(BalanceActionDto dto, WebRequestWrapper request) {
        BalanceActionVo balanceActionVo = new BalanceActionVo();

        //* Temporary solution to map into DTO
        dto = this.queryStringToDto(request.getBody(), BalanceActionDto.class);

        //*
        balanceActionVo.setUpdatedTime(1020202020L);
        balanceActionVo.setBalanceAmount(BigDecimal.valueOf(22.3));
        balanceActionVo.setCurrencyCode("CNY");
        return balanceActionVo;
    }

}
