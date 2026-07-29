package com.nextgen.gameaggregator.scheduler.betaction;

import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
public class GeneralVo implements HttpResponse {
    public String message;

    @Override
    public boolean hasError() {
        return false;
    }
}
