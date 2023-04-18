package com.nextgen.gameaggregator.vendor.queenmaker.api.credit;

import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

import java.util.List;
@Data
public class CreditVo implements HttpResponse {
    private List<? extends Transactions> transactions;

    @Override
    public boolean hasError() {
        return false;
    }
}
