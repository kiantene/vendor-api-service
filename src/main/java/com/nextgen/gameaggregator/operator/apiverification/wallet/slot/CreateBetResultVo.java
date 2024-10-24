package com.nextgen.gameaggregator.operator.apiverification.wallet.slot;

import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
public class CreateBetResultVo<T> implements HttpResponse {
    private String settleBetId;
    private String betIdempotentId;
    private Long requestStartTime;
    private Long requestEndTime;
    private Long requestTimeTaken;
    private String error;


    @Override
    public boolean hasError() {
        return false;
    }
}
