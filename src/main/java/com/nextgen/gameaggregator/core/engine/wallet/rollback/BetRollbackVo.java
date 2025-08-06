package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
public class BetRollbackVo implements HttpResponse {

    private String status;

    @Override
    public boolean hasError() {
        return !status.equals("SUCCESS");
    }
}
