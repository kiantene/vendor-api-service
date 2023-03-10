package com.nextgen.gameaggregator.vendor.spadegaming.vo;

import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper=false)
@Data
public class AuthBalanceVo extends ResponseVo implements HttpResponse{
    private AcctInfoVo acctInfo;

    @Override
    public boolean hasError() {
        return false;
    }
}
