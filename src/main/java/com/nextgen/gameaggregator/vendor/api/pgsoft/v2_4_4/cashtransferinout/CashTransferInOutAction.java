package com.nextgen.gameaggregator.vendor.api.pgsoft.v2_4_4.cashtransferinout;

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
public class CashTransferInOutAction extends AbstractAction {
    @PostMapping(path = Constant.ACTION_CASH_TRANSFER_IN_OUT)
    public CashTransferInOutActionVo theAction(CashTransferInOutActionDto dto, WebRequestWrapper request) {
        CashTransferInOutActionVo cashTransferInOutActionVo = new CashTransferInOutActionVo();

        //* Temporary solution to map into DTO
        dto = this.queryStringToDto(request.getBody(), CashTransferInOutActionDto.class);

        //* hardcoded response
        cashTransferInOutActionVo.setUpdatedTime(1020202020L);
        cashTransferInOutActionVo.setBalanceAmount(BigDecimal.valueOf(22.3));
        cashTransferInOutActionVo.setCurrencyCode("CNY");
        return cashTransferInOutActionVo;
    }
}